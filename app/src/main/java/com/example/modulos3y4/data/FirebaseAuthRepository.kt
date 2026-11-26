package com.example.modulos3y4.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.IOException

/** Resultado de un intento de autenticacion, con el mensaje ya listo para la UI. */
sealed interface AuthOutcome {
    data class Success(val email: String) : AuthOutcome
    data class Error(val message: String) : AuthOutcome
}

/**
 * Autenticacion con correo y contrasena contra Firebase.
 *
 * Reemplaza al antiguo AuthRepository que guardaba los usuarios en Room. Una
 * diferencia importante: FirebaseAuth persiste la sesion por su cuenta, asi que
 * el usuario sigue autenticado despues de cerrar la app, cosa que el login local
 * no hacia.
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    val currentUser: FirebaseUser? get() = auth.currentUser

    /**
     * Emite el usuario autenticado y null al cerrar sesion. Se usa el listener
     * de Firebase en lugar de un estado propio para que la UI reaccione tambien
     * cuando sea el SDK quien invalide la sesion.
     */
    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun login(email: String, password: String): AuthOutcome {
        val trimmed = email.trim()
        validate(trimmed, password)?.let { return it }
        return try {
            val result = auth.signInWithEmailAndPassword(trimmed, password).await()
            AuthOutcome.Success(result.user?.email ?: trimmed)
        } catch (e: Exception) {
            AuthOutcome.Error(messageFor(e, isRegistration = false))
        }
    }

    suspend fun register(email: String, password: String): AuthOutcome {
        val trimmed = email.trim()
        validate(trimmed, password)?.let { return it }
        return try {
            val result = auth.createUserWithEmailAndPassword(trimmed, password).await()
            AuthOutcome.Success(result.user?.email ?: trimmed)
        } catch (e: Exception) {
            AuthOutcome.Error(messageFor(e, isRegistration = true))
        }
    }

    fun signOut() = auth.signOut()

    private companion object {
        const val TAG = "FirebaseAuthRepository"
    }

    /** Validaciones locales, para no gastar una llamada de red en lo evidente. */
    private fun validate(email: String, password: String): AuthOutcome.Error? = when {
        email.isEmpty() || password.isEmpty() ->
            AuthOutcome.Error("Ingresa tu correo y contraseña")

        !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
            AuthOutcome.Error("El correo no tiene un formato válido")

        else -> null
    }

    /**
     * Firebase devuelve los mensajes en ingles y orientados a desarrollador.
     * Se traducen a algo que el usuario pueda accionar.
     */
    private fun messageFor(error: Exception, isRegistration: Boolean): String = when (error) {
        is FirebaseAuthWeakPasswordException ->
            "La contraseña es muy débil: usa al menos 6 caracteres"

        is FirebaseAuthUserCollisionException ->
            "Ya existe una cuenta con ese correo"

        is FirebaseAuthInvalidUserException ->
            "No existe una cuenta con ese correo"

        // Firebase usa la misma excepcion para la contrasena incorrecta y para el
        // correo mal formado, por eso el mensaje cubre ambos casos.
        is FirebaseAuthInvalidCredentialsException ->
            if (isRegistration) "El correo no es válido" else "Correo o contraseña incorrectos"

        is IOException ->
            "Sin conexión. Revisa tu red e inténtalo de nuevo."

        // No se expone error.message: Firebase devuelve textos de desarrollador
        // en ingles, como "An internal error has occurred.
        // [ CONFIGURATION_NOT_FOUND ]", que al usuario no le dicen nada. El
        // detalle va al log, donde si sirve para diagnosticar.
        else -> {
            Log.w(TAG, "Fallo de autenticación no contemplado", error)
            "No se pudo completar la operación. Inténtalo más tarde."
        }
    }
}

package com.example.modulos3y4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import com.example.modulos3y4.data.FirebaseAuthRepository
import com.example.modulos3y4.ui.login.LoginScreen
import com.example.modulos3y4.ui.theme.Modulos3y4Theme
import com.example.modulos3y4.ui.welcome.WelcomeRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val authRepository = FirebaseAuthRepository()
        setContent {
            Modulos3y4Theme {
                /**
                 * La sesion la manda FirebaseAuth, no un estado de la pantalla:
                 * el SDK la persiste, de modo que al reabrir la app el usuario
                 * sigue autenticado. El valor inicial se toma de currentUser para
                 * no mostrar el login un instante antes de que llegue el listener.
                 */
                val user by produceState(initialValue = authRepository.currentUser) {
                    authRepository.authState.collect { value = it }
                }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val email = user?.email
                    if (email == null) {
                        LoginScreen(
                            onLogin = authRepository::login,
                            onRegister = authRepository::register,
                            // El cambio de pantalla lo dispara el listener de
                            // FirebaseAuth; aqui no hay que guardar nada.
                            onSuccess = {},
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        WelcomeRoute(
                            username = email,
                            onLogout = authRepository::signOut,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

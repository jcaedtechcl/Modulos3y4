package com.example.modulos3y4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.modulos3y4.data.AppDatabase
import com.example.modulos3y4.data.AuthRepository
import com.example.modulos3y4.ui.login.LoginScreen
import com.example.modulos3y4.ui.theme.Modulos3y4Theme
import com.example.modulos3y4.ui.welcome.WelcomeRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val authRepository = AuthRepository(AppDatabase.getInstance(this).userDao())
        setContent {
            Modulos3y4Theme {
                // Usuario autenticado; null mientras se muestra el login.
                var loggedUser by rememberSaveable { mutableStateOf<String?>(null) }
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val user = loggedUser
                    if (user == null) {
                        LoginScreen(
                            onLogin = authRepository::login,
                            onRegister = authRepository::register,
                            onSuccess = { loggedUser = it },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        WelcomeRoute(
                            username = user,
                            onLogout = { loggedUser = null },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

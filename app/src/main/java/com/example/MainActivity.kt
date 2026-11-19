package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppNavigation
import com.example.ui.AuthViewModel
import com.example.ui.NoteViewModel
import com.example.ui.SubscriptionViewModel
import com.example.ui.VaultViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as RadiantGuardApplication

        setContent {
            // Theme mode (system / light / dark) chosen in MainScreen's overflow menu.
            val themeMode by app.settingsStore.themeModeFlow.collectAsState()
            val darkTheme = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return AuthViewModel(app.settingsStore, app.vaultRepository) as T
                            }
                        }
                    )

                    // Auto-lock: when the app goes to the background (ProcessLifecycleOwner
                    // ON_STOP), clear the session so the PIN is required again on return.
                    // Rotation / config change does NOT trigger this — only backgrounding.
                    DisposableEffect(authViewModel) {
                        val observer = object : DefaultLifecycleObserver {
                            override fun onStop(owner: LifecycleOwner) {
                                authViewModel.logout()
                            }
                        }
                        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
                        onDispose { ProcessLifecycleOwner.get().lifecycle.removeObserver(observer) }
                    }

                    val vaultViewModel: VaultViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return VaultViewModel(app.vaultRepository) as T
                            }
                        }
                    )

                    val subscriptionViewModel: SubscriptionViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return SubscriptionViewModel(app, app.subscriptionRepository) as T
                            }
                        }
                    )

                    val noteViewModel: NoteViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return NoteViewModel(app, app.noteRepository) as T
                            }
                        }
                    )

                    AppNavigation(
                        authViewModel = authViewModel,
                        vaultViewModel = vaultViewModel,
                        subscriptionViewModel = subscriptionViewModel,
                        noteViewModel = noteViewModel
                    )
                }
            }
        }
    }
}

package com.example.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AddEditNoteScreen
import com.example.ui.screens.AddEditScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainScreen

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel,
    vaultViewModel: VaultViewModel,
    subscriptionViewModel: SubscriptionViewModel,
    noteViewModel: NoteViewModel
) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsState()

    // Smooth single-source-of-truth lock listener without glitching
    LaunchedEffect(authState.screenState) {
        if (authState.screenState != AuthScreenState.AUTHENTICATED) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != null && currentRoute != "auth") {
                navController.navigate("auth") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = if (authState.screenState == AuthScreenState.AUTHENTICATED) "main" else "auth"
        ) {
            composable("auth") {
                LoginScreen(
                    viewModel = authViewModel,
                    onAuthenticated = {
                        navController.navigate("main") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                )
            }

            composable("main") {
                MainScreen(
                    vaultViewModel = vaultViewModel,
                    subscriptionViewModel = subscriptionViewModel,
                    noteViewModel = noteViewModel,
                    authViewModel = authViewModel,
                    onLock = {
                        // Just log out; the LaunchedEffect above owns navigation to "auth".
                        authViewModel.logout()
                    },
                    onNavigateToAddPassword = {
                        navController.navigate("addEditPassword/-1")
                    },
                    onNavigateToEditPassword = { itemId ->
                        navController.navigate("addEditPassword/$itemId")
                    },
                    onNavigateToAddNote = {
                        navController.navigate("addEditNote/-1")
                    },
                    onNavigateToEditNote = { noteId ->
                        navController.navigate("addEditNote/$noteId")
                    }
                )
            }

            composable(
                route = "addEditPassword/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.IntType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getInt("itemId") ?: -1
                AddEditScreen(
                    itemId = itemId,
                    viewModel = vaultViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "addEditNote/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.IntType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getInt("noteId") ?: -1
                AddEditNoteScreen(
                    noteId = noteId,
                    viewModel = noteViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

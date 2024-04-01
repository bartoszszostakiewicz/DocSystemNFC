package com.docsysnfc.flowtouch.ui

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.docsysnfc.flowtouch.MainViewModel
import com.docsysnfc.flowtouch.model.flowtouchStates.NFCSysScreen

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AppNavigation(viewModel: MainViewModel, context: Context) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NFCSysScreen.Login.name
    ) {


        composable(NFCSysScreen.Login.name) {
            LoginScreen(
                navController,
                viewModel,
                context
            )
        }

        composable(
            route = "${NFCSysScreen.Send.name}/{index}",
            arguments = listOf(
                navArgument("index") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val index = backStackEntry.arguments?.getInt("index")
            if (index != null) {
                SendScreen(navController, viewModel, context, index)
            } else {
                // Obsługa błędu, jeśli index jest null
            }
        }


        composable(NFCSysScreen.Receive.name) {
            ReceiveScreen(
                navController,
                viewModel,
                context
            )
        }

        composable(NFCSysScreen.Home.name) {
            HomeScreen(
                navController,
                viewModel,
                context
            )
        }

        composable(NFCSysScreen.Recovery.name) {
            RecoveryScreen(
                navController,
                viewModel,
                context
            )
        }

        composable(NFCSysScreen.Create.name) {
            CreateAccountScreen(
                navController,
                viewModel,
                context
            )
        }


        composable(NFCSysScreen.Settings.name) {
            SettingsScreen(
                navController,
                viewModel,
                context
            )
        }

        composable(NFCSysScreen.ShareKeyScreen.name) {
            ShareKeyScreen(
                navController,
                viewModel,
                context
            )
        }
    }
}





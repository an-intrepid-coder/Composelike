package com.example.composelike

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun ComposelikeApp() {

    // TODO: Loading Screen while initializing: <-- Next
    val simulationViewModel: SimulationViewModel = viewModel(
        factory = SimulationViewModelFactory()
    )

    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "composelikeInterface"
    ) {
        // TODO: A Main Menu
        // TODO: An Options Screen
        // TODO: A Character Screen
        // TODO: A Stats Screen
        composable("composelikeInterface") {
            ComposelikeInterface(
                simulationViewModel = simulationViewModel,
                navController = navController
            )
        }
        composable("inventoryScreen") {
            InventoryScreen(
                simulationViewModel = simulationViewModel,
                navController = navController
            )
        }
        composable("messageLog") {
            MessageLog(
                simulationViewModel = simulationViewModel,
            )
        }
        composable("mapScreen") {
            MapScreen(
                simulationViewModel = simulationViewModel,
            )
        }
    }
}
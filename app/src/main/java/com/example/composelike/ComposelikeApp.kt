package com.example.composelike

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun ComposelikeApp() {
    val gameViewModel: GameViewModel = viewModel(
        factory = SceneViewModelFactory(
            tilemapCols = 50,
            tilemapRows = 50,
            tilemapType = TilemapType.TESTING
        )
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
                gameViewModel = gameViewModel,
                navController = navController
            )
        }
        composable("inventoryScreen") {
            InventoryScreen(
                gameViewModel = gameViewModel,
                navController = navController
            )
        }
        composable("messageLog") {
            MessageLog(
                gameViewModel = gameViewModel
            )
        }
        composable("mapScreen") {
            MapScreen(
                gameViewModel = gameViewModel
            )
        }
    }
}
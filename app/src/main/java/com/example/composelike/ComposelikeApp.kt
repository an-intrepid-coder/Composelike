package com.example.composelike

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// TODO: A loading screen for long blocking processes such as map generation.
//  Generating new maps and populating them takes a bit, for now.

@Composable
fun ComposelikeApp() {
    val gameViewModel: GameViewModel = viewModel(
        factory = GameViewModelFactory(
            // Note: Larger maps (100x100, for example) have noticeable loading times.
            tilemapCols = 30,
            tilemapRows = 30,
            tilemapType = TilemapType.CAVE
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
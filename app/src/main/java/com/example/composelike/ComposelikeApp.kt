package com.example.composelike

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun ComposelikeApp() {
    val sceneViewModel: SceneViewModel = viewModel(
        factory = SceneViewModelFactory(
            tilemapCols = 50,
            tilemapRows = 50,
            tilemapType = TilemapType.TESTING
        )
    )

    val hudStrings by sceneViewModel.hudStrings.observeAsState()
    val tilemapStrings by sceneViewModel.tilemapStrings.observeAsState()
    val messageLog by sceneViewModel.messageLog.observeAsState()
    val inventoryEntries by sceneViewModel.inventoryEntries.observeAsState()
    val mapScreenStrings by sceneViewModel.mapScreenStrings.observeAsState()

    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "composelikeInterface"
    ) {
        // TODO: A Main Menu
        // TODO: An Options Screen
        // TODO: A Character Screen
        // TODO: An expanded Map Screen
        // TODO: A stats screen
        composable("composelikeInterface") {
            ComposelikeInterface(
                sceneViewModel = sceneViewModel,
                hudStrings = hudStrings!!,
                tilemapStrings = tilemapStrings!!,
                messageLog = messageLog!!,
                navController = navController
            )
        }
        composable("inventoryScreen") {
            InventoryScreen(
                sceneViewModel = sceneViewModel,
                inventoryEntries = inventoryEntries!!,
                navController = navController
            )
        }
        composable("messageLog") {
            MessageLog(messageLog = messageLog!!)
        }
        composable("mapScreen") {
            MapScreen(mapScreenStrings = mapScreenStrings!!)
        }
    }
}
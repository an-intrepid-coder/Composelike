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
            tilemapCols = 36,
            tilemapRows = 12,
            tilemapType = TilemapType.TESTING
        )
    )

    val hudStrings by sceneViewModel.hudStrings.observeAsState()
    val tilemapStrings by sceneViewModel.tilemapStrings.observeAsState()
    val messageLog by sceneViewModel.messageLog.observeAsState()

    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "composelikeInterface"
    ) {
        composable("composelikeInterface") {
            ComposelikeInterface(
                sceneViewModel = sceneViewModel,
                hudStrings = hudStrings!!,
                tilemapStrings = tilemapStrings!!,
                messageLog = messageLog!!
            )
        }
        // TODO: Navigation to menus!
    }
}
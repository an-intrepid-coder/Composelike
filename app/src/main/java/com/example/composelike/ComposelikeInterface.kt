package com.example.composelike

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun ComposelikeHud(hudStrings: Map<String, String>) {
    Row {
        Text(hudStrings["hp"]!!)
        Spacer(Modifier.width(28.dp))
        Text(hudStrings["mp"]!!)
        Spacer(Modifier.width(28.dp))
        Text(hudStrings["turnsPassed"]!!)
    }
    Row {
        Text(hudStrings["bonusAttack"]!!)
        Spacer(Modifier.width(28.dp))
        Text(hudStrings["bonusDefense"]!!)
        Spacer(Modifier.width(28.dp))
        Text(hudStrings["playerLevel"]!!)
    }
    Text(hudStrings["experienceToLevel"]!!) // TODO: XP Bar!
    Row {
        Text(hudStrings["dungeonLevel"]!!)
        Spacer(Modifier.width(28.dp))
        Text(hudStrings["gold"]!!)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
fun ComposelikeTilemap(tilemapStrings: List<String>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (line in tilemapStrings) {
            item { Text(line) }
        }
    }
}

@Composable
fun ComposelikeTouchControls(
    simulationViewModel: SimulationViewModel,
    navController: NavController
) {
    Row {
        Text("[MAP]", Modifier.clickable {
            //simulation.updateMapScreenStrings() <-- maybe
            navController.navigate("mapScreen")
        })
        Spacer(Modifier.width(8.dp))
        Text("[LOG]", Modifier.clickable {
            navController.navigate("messageLog")
        })
        Spacer(Modifier.width(8.dp))
        Text("[Y]", Modifier.clickable {
            simulationViewModel.advanceSimByMove(MovementDirection.UpLeft())
        })
        Spacer(Modifier.width(8.dp))
        Text("[K]", Modifier.clickable {
            simulationViewModel.advanceSimByMove(MovementDirection.Up())
        })
        Spacer(Modifier.width(8.dp))
        Text("[U]", Modifier.clickable {
            simulationViewModel.advanceSimByMove(MovementDirection.UpRight())
        })
        Spacer(Modifier.width(8.dp))
        Text("[INV]", Modifier.clickable {
            //simulation.updateInventoryEntries() <-- Maybe
            navController.navigate("inventoryScreen")
        })
        Spacer(Modifier.width(8.dp))
        Text("[CHR]", Modifier.clickable {
            // TODO: Character Sheet & Misc. Player Stats
        })
    }
    Spacer(Modifier.height(8.dp))
    Row {
        Text("[<]", Modifier.clickable {
            // TODO: Stairs up.
        })
        Spacer(Modifier.width(8.dp))
        Text("[H]", Modifier.clickable {
            simulationViewModel.advanceSimByMove(MovementDirection.Left())
        })
        Spacer(Modifier.width(8.dp))
        Text("[.]", Modifier.clickable {
            simulationViewModel.advanceSimByMove(MovementDirection.Stationary())
        })
        Spacer(Modifier.width(8.dp))
        Text("[L]", Modifier.clickable {
            simulationViewModel.advanceSimByMove(MovementDirection.Right())
        })
        Spacer(Modifier.width(8.dp))
        Text("[>]", Modifier.clickable {
            // TODO: Stairs down.
        })
    }
    Spacer(Modifier.height(8.dp))
    Row {
        Text("[B]", Modifier.clickable {
            simulationViewModel.advanceSimByMove(MovementDirection.DownLeft())
        })
        Spacer(Modifier.width(8.dp))
        Text("[J]", Modifier.clickable {
            simulationViewModel.advanceSimByMove(MovementDirection.Down())
        })
        Spacer(Modifier.width(8.dp))
        Text("[N]", Modifier.clickable {
            simulationViewModel.advanceSimByMove(MovementDirection.DownRight())
        })
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
fun ComposelikeMessageLog(messageLog: List<String>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (msg in messageLog.reversed()) {
            item { Text(msg) }
        }
    }
}

@Composable
fun ComposelikeInterface(simulationViewModel: SimulationViewModel, navController: NavController) {

    val hudStrings by simulationViewModel.hudStrings.observeAsState()
    val tilemapStrings by simulationViewModel.tilemapStrings.observeAsState()
    val messageLog by simulationViewModel.messageLogStrings.observeAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Some HUD information:
        ComposelikeHud(hudStrings = hudStrings!!)
        // The Tilemap display:
        ComposelikeTilemap(tilemapStrings = tilemapStrings!!)
        // Touch Controls:
        ComposelikeTouchControls(simulationViewModel = simulationViewModel, navController = navController)
        // A LazyColumn of the entire Message Log, starting with the tail end.
        ComposelikeMessageLog(messageLog = messageLog!!)
    }
}

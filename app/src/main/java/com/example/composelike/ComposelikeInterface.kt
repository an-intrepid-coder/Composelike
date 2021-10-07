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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun ComposelikeHud(hudStrings: Map<String, String>) {
    Row {
        Text(hudStrings.getOrElse("hp") { "0" })
        Spacer(Modifier.width(28.dp))
        Text(hudStrings.getOrElse("mp") { "0" })
        Spacer(Modifier.width(28.dp))
        Text(hudStrings.getOrElse("turnsPassed") { "0" })
    }
    Row {
        Text(hudStrings.getOrElse("bonusAttack") { "0" })
        Spacer(Modifier.width(28.dp))
        Text(hudStrings.getOrElse("bonusDefense") { "0" })
        Spacer(Modifier.width(28.dp))
        Text(hudStrings.getOrElse("playerLevel") { "0" })
    }
    Text(hudStrings.getOrElse("experienceToLevel") { "0" }) // TODO: XP Bar!
    Row {
        Text(hudStrings.getOrElse("dungeonLevel") { "0" })
        Spacer(Modifier.width(28.dp))
        Text(hudStrings.getOrElse("gold") { "0" })
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

// TODO: Perhaps a Fog of War toggle for development purposes. Such a thing would be appropriate
//  for a "Wizard Mode" down the road, as well.

@Composable
fun ComposelikeTouchControls(
    simulationViewModel: SimulationViewModel,
    navController: NavController
) {
    Row {
        Text(
            text = "[MAP]",
            modifier = Modifier.clickable { navController.navigate("mapScreen") },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "[LOG]",
            modifier = Modifier.clickable { navController.navigate("messageLog") },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "[Y]",
            modifier = Modifier.clickable {
                simulationViewModel.advanceSimByMove(MovementDirection.UpLeft())
            },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "[K]",
            modifier = Modifier.clickable {
                simulationViewModel.advanceSimByMove(MovementDirection.Up())
            },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "[U]",
            modifier = Modifier.clickable {
                simulationViewModel.advanceSimByMove(MovementDirection.UpRight())
            },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "[INV]",
            modifier = Modifier.clickable { navController.navigate("inventoryScreen") },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "[CHR]",
            modifier = Modifier.clickable { }, // TODO
            fontSize = 17.sp
        )
    }
    // TODO: Polish the rest of these Text()s. Factor out into a re-usable button or something.
    // TODO: Custom Font styles in more depth. Perhaps some real buttons.
    Spacer(Modifier.height(8.dp))
    Row {
        Text(
            text = "[<]",
            modifier = Modifier.clickable {
                // TODO: Stairs up.
            },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "[H]",
            modifier = Modifier.clickable {
                simulationViewModel.advanceSimByMove(MovementDirection.Left())
            },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "[.]",
            modifier = Modifier.clickable {
                simulationViewModel.advanceSimByMove(MovementDirection.Stationary())
            },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "[L]",
            modifier = Modifier.clickable {
                simulationViewModel.advanceSimByMove(MovementDirection.Right())
            },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "[>]",
            modifier = Modifier.clickable {
                // TODO: Stairs down.
            },
            fontSize = 17.sp
        )
    }
    Spacer(Modifier.height(8.dp))
    Row {
        Text(
            text = "[B]",
            modifier = Modifier.clickable {
                simulationViewModel.advanceSimByMove(MovementDirection.DownLeft())
            },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "[J]",
            modifier = Modifier.clickable {
                simulationViewModel.advanceSimByMove(MovementDirection.Down())
            },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "[N]",
            modifier = Modifier.clickable {
                simulationViewModel.advanceSimByMove(MovementDirection.DownRight())
            },
            fontSize = 17.sp
        )
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
        if (hudStrings != null) ComposelikeHud(hudStrings!!)
        // The Tilemap display:
        if (tilemapStrings != null) ComposelikeTilemap(tilemapStrings!!)
        // Touch Controls:
        ComposelikeTouchControls(simulationViewModel, navController)
        // A LazyColumn of the entire Message Log, starting with the tail end.
        if (messageLog != null) ComposelikeMessageLog(messageLog!!)
    }
}

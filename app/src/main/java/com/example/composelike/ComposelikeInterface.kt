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
import com.example.composelike.ui.theme.CautionYellow
import com.example.composelike.ui.theme.DoublePlusGreen
import com.example.composelike.ui.theme.ElectricTeal
import com.example.composelike.ui.theme.VibrantMagenta

@Composable
fun ComposelikeHud(hudStrings: Map<String, String>) {
    // TODO: A more refined HUD with numbers that change color based on player status,
    //  and also some images.
    Row {
        Text(hudStrings.getOrElse("hp") { "display error!" })
        Spacer(Modifier.width(28.dp))
        Text(hudStrings.getOrElse("mp") { "display error!" })
        Spacer(Modifier.width(28.dp))
        Text(hudStrings.getOrElse("turnsPassed") { "display error!" })
    }
    Row {
        Text(hudStrings.getOrElse("bonusAttack") { "display error!" })
        Spacer(Modifier.width(28.dp))
        Text(hudStrings.getOrElse("bonusDefense") { "display error!" })
        Spacer(Modifier.width(28.dp))
        Text(hudStrings.getOrElse("playerLevel") { "display error!" })
    }
    Text(hudStrings.getOrElse("experienceToLevel") { "display error!" })
    Row {
        Text(hudStrings.getOrElse("dungeonLevel") { "display error!" })
        Spacer(Modifier.width(28.dp))
        Text(hudStrings.getOrElse("gold") { "display error!" })
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
fun ComposelikeTilemap(tilemapStrings: List<String>) {
    // TODO: Long-Term: Convert this portion of the UI to something based on the Canvas using
    //  a Cell-based system.
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        tilemapStrings.forEach {
            item { Text(it) }
        }
    }
}

@Composable
fun ComposelikeTouchControls(
    simulationViewModel: SimulationViewModel,
    navController: NavController
) {
    Row {
        Text(
            color = CautionYellow,
            text = "[MAP]",
            modifier = Modifier.clickable { navController.navigate("mapScreen") },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            color = CautionYellow,
            text = "[LOG]",
            modifier = Modifier.clickable { navController.navigate("messageLog") },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            color = ElectricTeal,
            text = "[Y]",
            modifier = Modifier.clickable {
                simulationViewModel.advanceSimByMove(MovementDirection.UpLeft())
            },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            color = ElectricTeal,
            text = "[K]",
            modifier = Modifier.clickable {
                simulationViewModel.advanceSimByMove(MovementDirection.Up())
            },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            color = ElectricTeal,
            text = "[U]",
            modifier = Modifier.clickable {
                simulationViewModel.advanceSimByMove(MovementDirection.UpRight())
            },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            color = CautionYellow,
            text = "[INV]",
            modifier = Modifier.clickable { navController.navigate("inventoryScreen") },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            color = CautionYellow,
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
            color = DoublePlusGreen,
            text = "[<]",
            modifier = Modifier.clickable {
                // TODO: Stairs up.
            },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            color = ElectricTeal,
            text = "[H]",
            modifier = Modifier.clickable {
                simulationViewModel.advanceSimByMove(MovementDirection.Left())
            },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            color = VibrantMagenta,
            text = "[.]",
            modifier = Modifier.clickable {
                simulationViewModel.advanceSimByMove(MovementDirection.Stationary())
            },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            color = ElectricTeal,
            text = "[L]",
            modifier = Modifier.clickable {
                simulationViewModel.advanceSimByMove(MovementDirection.Right())
            },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            color = DoublePlusGreen,
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
            color = ElectricTeal,
            text = "[B]",
            modifier = Modifier.clickable {
                simulationViewModel.advanceSimByMove(MovementDirection.DownLeft())
            },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            color = ElectricTeal,
            text = "[J]",
            modifier = Modifier.clickable {
                simulationViewModel.advanceSimByMove(MovementDirection.Down())
            },
            fontSize = 17.sp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            color = ElectricTeal,
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
        horizontalAlignment = Alignment.CenterHorizontally,
        reverseLayout = true,
    ) {
        for (msg in messageLog.reversed()) {
            item { Text(msg) }
        }
    }
}

@Composable
fun ComposelikeInterface(simulationViewModel: SimulationViewModel, navController: NavController) {

    // TODO: Long-Term: Put together a real theme and apply it to all the Composables.

    val hudStrings by simulationViewModel.hudStrings.observeAsState()
    val tilemapStrings by simulationViewModel.tilemapStrings.observeAsState()
    val messageLog by simulationViewModel.messageLogStrings.observeAsState()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Some HUD information:
        hudStrings?.let { ComposelikeHud(it) }
        // The Tilemap display:
        tilemapStrings?.let { ComposelikeTilemap(it) }
        // Touch Controls:
        ComposelikeTouchControls(simulationViewModel, navController)
        // A LazyColumn of the entire Message Log, starting with the tail end.
        messageLog?.let { ComposelikeMessageLog(it) }
    }
}

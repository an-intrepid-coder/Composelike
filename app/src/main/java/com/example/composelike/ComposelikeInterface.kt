package com.example.composelike

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ComposelikeHud(
    hudStrings: Map<String, String>
) {
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
fun ComposelikeTilemap(
    tilemapStrings: List<String>
) {
    LazyColumn {
        for (line in tilemapStrings) {
            item { Text(line) }
        }
    }
}

@Composable
fun ComposelikeTouchControls(
    sceneViewModel: SceneViewModel
) {
    Row {
        Text("[LOG]", Modifier.clickable {
            // TODO
        })
        Spacer(Modifier.width(8.dp))
        Text("[Y]", Modifier.clickable {
            sceneViewModel.movePlayerAndProcessTurn(MovementDirection.UPLEFT)
        })
        Spacer(Modifier.width(8.dp))
        Text("[K]", Modifier.clickable {
            sceneViewModel.movePlayerAndProcessTurn(MovementDirection.UP)
        })
        Spacer(Modifier.width(8.dp))
        Text("[U]", Modifier.clickable {
            sceneViewModel.movePlayerAndProcessTurn(MovementDirection.UPRIGHT)
        })
        Spacer(Modifier.width(8.dp))
        Text("[INV]", Modifier.clickable {
            // TODO
        })
    }
    Spacer(Modifier.height(8.dp))
    Row {
        Text("[H]", Modifier.clickable {
            sceneViewModel.movePlayerAndProcessTurn(MovementDirection.LEFT)
        })
        Spacer(Modifier.width(8.dp))
        Text("[.]", Modifier.clickable {
            sceneViewModel.movePlayerAndProcessTurn(MovementDirection.STATIONARY)
        })
        Spacer(Modifier.width(8.dp))
        Text("[L]", Modifier.clickable {
            sceneViewModel.movePlayerAndProcessTurn(MovementDirection.RIGHT)
        })
    }
    Spacer(Modifier.height(8.dp))
    Row {
        Text("[B]", Modifier.clickable {
            sceneViewModel.movePlayerAndProcessTurn(MovementDirection.DOWNLEFT)
        })
        Spacer(Modifier.width(8.dp))
        Text("[J]", Modifier.clickable {
            sceneViewModel.movePlayerAndProcessTurn(MovementDirection.DOWN)
        })
        Spacer(Modifier.width(8.dp))
        Text("[N]", Modifier.clickable {
            sceneViewModel.movePlayerAndProcessTurn(MovementDirection.DOWNRIGHT)
        })
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
fun ComposelikeMessageLog(
    messageLog: List<String>
) {
    LazyColumn {
        for (msg in messageLog.reversed()) {
            item { Text(msg) }
        }
    }
}

@Composable
fun ComposelikeInterface(
    sceneViewModel: SceneViewModel,
    hudStrings: Map<String, String>,
    tilemapStrings: List<String>,
    messageLog: List<String>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Some HUD information:
        ComposelikeHud(hudStrings = hudStrings)
        // The Tilemap display:
        ComposelikeTilemap(tilemapStrings = tilemapStrings)
        // Touch Controls:
        ComposelikeTouchControls(sceneViewModel = sceneViewModel)
        // A LazyColumn of the entire Message Log, starting with the tail end.
        ComposelikeMessageLog(messageLog = messageLog)
    }
}

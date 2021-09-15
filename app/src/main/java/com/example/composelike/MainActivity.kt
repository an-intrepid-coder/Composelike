package com.example.composelike

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.composelike.ui.theme.ComposelikeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposelikeTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    ComposelikeApp()
                }
            }
        }
    }
}

@Composable
fun ComposelikeApp() {
    val tilemapViewModel = SceneViewModel(36, 12, TilemapType.TESTING)
    val tilemapStrings by tilemapViewModel.tilemapStrings.observeAsState()

    // TODO: Navigation graph, along with factoring the below stuff into an GameScreen or
    //  something like that. Must test with a placeholder inventory as soon as movement
    //  and recomposition are working. Once all of those are working together, the fun can
    //  truly begin and the sky is the limit.

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Some HUD information:
        // TODO
        // The Tilemap display:
        LazyColumn {
            for (line in tilemapStrings!!) {
                item { Text(line) }
            }
        }
        // Movement Controls:
        Row {
            Text("[Y]", Modifier.clickable {
                tilemapViewModel.movePlayerAndProcessTurn(MovementDirection.UPLEFT)
            })
            Spacer(Modifier.width(8.dp))
            Text("[K]", Modifier.clickable {
                tilemapViewModel.movePlayerAndProcessTurn(MovementDirection.UP)
            })
            Spacer(Modifier.width(8.dp))
            Text("[U]", Modifier.clickable {
                tilemapViewModel.movePlayerAndProcessTurn(MovementDirection.UPRIGHT)
            })
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Text("[H]", Modifier.clickable {
                tilemapViewModel.movePlayerAndProcessTurn(MovementDirection.LEFT)
            })
            Spacer(Modifier.width(8.dp))
            Text("[.]", Modifier.clickable {
                tilemapViewModel.movePlayerAndProcessTurn(MovementDirection.STATIONARY)
            })
            Spacer(Modifier.width(8.dp))
            Text("[L]", Modifier.clickable {
                tilemapViewModel.movePlayerAndProcessTurn(MovementDirection.RIGHT)
            })
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Text("[B]", Modifier.clickable {
                tilemapViewModel.movePlayerAndProcessTurn(MovementDirection.DOWNLEFT)
            })
            Spacer(Modifier.width(8.dp))
            Text("[J]", Modifier.clickable {
                tilemapViewModel.movePlayerAndProcessTurn(MovementDirection.DOWN)
            })
            Spacer(Modifier.width(8.dp))
            Text("[N]", Modifier.clickable {
                tilemapViewModel.movePlayerAndProcessTurn(MovementDirection.DOWNRIGHT)
            })
        }
        Spacer(Modifier.height(8.dp))
        // Some additional controls:
        // TODO
        // Tail of the Message Log:
        // TODO
    }
}
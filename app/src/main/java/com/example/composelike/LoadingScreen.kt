package com.example.composelike

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO: Loading animations! Procedural! Cellular Automata and stuff. Could make a whole new
//  side project out of that actually.

@RequiresApi(Build.VERSION_CODES.N)
@Composable
fun LoadingScreen(
    simulationViewModel: SimulationViewModel,
    navController: NavController,
) {

    var launched by rememberSaveable { mutableStateOf(false) }
    var loaded by rememberSaveable { mutableStateOf(false) }
    val composableScope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition()
    val animationFloat by infiniteTransition.animateFloat(
        initialValue = 0F,
        targetValue = 100F,
        animationSpec = infiniteRepeatable(
            animation = tween(80000),
            repeatMode = RepeatMode.Reverse
        )
    )

    fun updatedLoadingEllipses(animationFloat: Float): String {
        var newEllipses = ""
        repeat (animationFloat.toInt() % 4) {
            newEllipses += "."
        }
        return newEllipses
    }

    Row(
        modifier = Modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (!launched) {
                    "New Game"
                } else if (launched && loaded) {
                    "Click to Continue"
                } else {
                    "Loading${updatedLoadingEllipses(animationFloat)}"
                },
                modifier = Modifier.clickable {
                    if (!launched) {
                        composableScope.launch(Dispatchers.Default) {
                            launched = true
                            simulationViewModel.loadSimulation()
                            loaded = true
                        }
                    } else if (launched && loaded) {
                        simulationViewModel.update()
                        navController.navigate("composelikeInterface")
                    }
                },
            )
        }
    }
}
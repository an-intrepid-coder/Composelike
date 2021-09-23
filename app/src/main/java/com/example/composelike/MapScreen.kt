package com.example.composelike

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.unit.sp

// TODO: Snap-to-player button! Actually quite a good little idea. Will require using
//  a finite animation.

@Composable
fun MapScreen(simulationViewModel: SimulationViewModel) {
    val mapScreenStrings by simulationViewModel.mapScreenStrings.observeAsState()
    LazyRow {
        item {
            LazyColumn {
                for (line in mapScreenStrings!!) {
                    item { Text(text = line, fontSize = 12.sp) }
                }
            }
        }
    }
}
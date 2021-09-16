package com.example.composelike

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.unit.sp

@Composable
fun MapScreen(gameViewModel: GameViewModel) {
    val mapScreenStrings by gameViewModel.mapScreenStrings.observeAsState()
    LazyRow {
        item {
            LazyColumn {
                for (line in mapScreenStrings!!) {
                    item { Text(text = line, fontSize = 13.sp) }
                }
            }
        }
    }
}
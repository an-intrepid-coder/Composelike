package com.example.composelike

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable

@Composable
fun MapScreen(
    mapScreenStrings: List<String>,
) {
    LazyRow {
        item {
            LazyColumn {
                for (line in mapScreenStrings) {
                    item { Text(line) }
                }
            }
        }
    }
}
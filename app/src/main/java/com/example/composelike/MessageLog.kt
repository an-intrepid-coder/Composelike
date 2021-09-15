package com.example.composelike

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MessageLog(
    gameViewModel: GameViewModel
) {
    val messageLog by gameViewModel.messageLog.observeAsState()
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("MESSAGE LOG")
            Text("-~-~-~-~-~-")
            Spacer(Modifier.height(8.dp))
        }
        for (msg in messageLog!!.reversed()) {
            item {
                Text(msg)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
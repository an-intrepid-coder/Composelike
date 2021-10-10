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
import com.example.composelike.ui.theme.DoublePlusGreen
import com.example.composelike.ui.theme.ElectricTeal

@Composable
fun MessageLogScreen(simulationViewModel: SimulationViewModel) {

    val messageLogStrings by simulationViewModel.messageLogStrings.observeAsState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        reverseLayout = true,
    ) {
        for (msg in messageLogStrings!!.reversed()) {
            item {
                Text(msg)
                Spacer(Modifier.height(8.dp))
            }
        }
        item {
            Text(color = ElectricTeal, text = "-~-~-~-~-~-")
            Text(color = DoublePlusGreen, text = "MESSAGE LOG")
            Spacer(Modifier.height(8.dp))
        }
    }
}
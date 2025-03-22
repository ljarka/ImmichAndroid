package com.github.ljarka.immich.android.server

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ServerConfigurationScreen(
    modifier: Modifier = Modifier,
    onAccept: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var viewModel: ServerConfigurationViewModel = hiltViewModel()
        var serverUrl by remember { mutableStateOf("") }

        TextField(value = serverUrl, onValueChange = {
            serverUrl = it
        })
        Button(onClick = {
            viewModel.setServerUrl(serverUrl)
            onAccept()
        }) {
            Text(text = "Accept")
        }
    }
}
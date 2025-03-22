package com.github.ljarka.immich.android.server

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ColumnScope.ServerConfigurationContent(
    onServerConfigured: () -> Unit = {},
) {
    var viewModel: ServerConfigurationViewModel = hiltViewModel()
    var serverUrl by remember { mutableStateOf("") }

    TextField(
        label = {
            Text(text = "Server URL")
        },
        value = serverUrl,
        onValueChange = {
            serverUrl = it
        })
    Button(
        modifier = Modifier.imePadding(),
        onClick = {
            viewModel.setServerUrl(serverUrl)
            onServerConfigured()
        }
    ) {
        Text(text = "Accept")
    }
}
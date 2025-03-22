package com.github.ljarka.immich.android.server

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.ljarka.immich.android.R

@Composable
fun ServerConfigurationScreen(
    modifier: Modifier = Modifier,
    onServerConfigured: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var viewModel: ServerConfigurationViewModel = hiltViewModel()
        var state = viewModel.state.collectAsStateWithLifecycle()
        var serverUrl by remember { mutableStateOf("") }

        LaunchedEffect(state.value) {
            if (state.value.isNotEmpty()) {
                onServerConfigured()
            }
        }

        Image(
            painter = painterResource(R.drawable.immich_logo),
            contentDescription = null
        )
        if (state.value.isEmpty()) {
            TextField(value = serverUrl, onValueChange = {
                serverUrl = it
            })
            Button(onClick = {
                viewModel.setServerUrl(serverUrl)
            }) {
                Text(text = "Accept")
            }
        }
    }
}
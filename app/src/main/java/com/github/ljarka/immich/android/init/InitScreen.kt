package com.github.ljarka.immich.android.init

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.ljarka.immich.android.R
import com.github.ljarka.immich.android.login.LoginContent
import com.github.ljarka.immich.android.server.ServerConfigurationContent

@Composable
fun InitScreen(
    onInitialized: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val viewModel: InitViewModel = hiltViewModel()
        Image(
            modifier = Modifier.padding(16.dp),
            painter = painterResource(R.drawable.immich_logo),
            contentDescription = null
        )

        val state = viewModel.initState.collectAsStateWithLifecycle()

        when (state.value) {
            InitState.INITIALIZED -> {
                LaunchedEffect(Unit) {
                    onInitialized()
                }
            }

            InitState.REQUIRES_LOGIN -> {
                LoginContent(onLoggedIn = {
                    viewModel.updateState()
                })
            }

            InitState.REQUIRES_SERVER_CONFIGURATION -> {
                ServerConfigurationContent(
                    onServerConfigured = {
                        viewModel.updateState()
                    }
                )
            }

            InitState.UNKNOWN -> {
                // Loading
            }
        }
    }
}
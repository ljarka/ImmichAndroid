package com.github.ljarka.immich.android.ui.login

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ColumnScope.LoginContent(
    onLoggedIn: () -> Unit,
) {
    val viewModel: LoginViewModel = hiltViewModel()
    var state = viewModel.state.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state.value) {
        if (state.value is LoginState.LoggedIn) {
            onLoggedIn()
        }
    }

    TextField(
        value = email,
        label = {
            Text("Email")
        },
        onValueChange = {
            email = it
        })
    TextField(
        value = password,
        label = {
            Text("Password")
        },
        onValueChange = {
            password = it
        })
    Button(
        onClick = {
            viewModel.performLogin(email, password)
        },
        modifier = Modifier.imePadding(),
    ) {
        Text(text = "Login")
    }
}
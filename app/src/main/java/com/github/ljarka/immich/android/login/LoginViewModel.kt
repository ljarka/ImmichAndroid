package com.github.ljarka.immich.android.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginService: LoginService,
    private val accessTokenStore: AccessTokenStore,
) : ViewModel() {

    private val _state = MutableStateFlow<LoginState>(LoginState.LoggedOut)
    val state = _state.asStateFlow()

    fun performLogin(
        email: String,
        password: String,
    ) {
        viewModelScope.launch {
            val result = loginService.login(LoginRequestBody(email, password))
            accessTokenStore.accessToken = result.accessToken
            _state.value = LoginState.LoggedIn
        }
    }
}
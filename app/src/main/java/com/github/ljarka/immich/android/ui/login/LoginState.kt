package com.github.ljarka.immich.android.ui.login

sealed class LoginState {

    object LoggedIn : LoginState()

    object LoggedOut : LoginState()
}
package com.github.ljarka.immich.android.login

sealed class LoginState {

    object LoggedIn : LoginState()

    object LoggedOut : LoginState()
}
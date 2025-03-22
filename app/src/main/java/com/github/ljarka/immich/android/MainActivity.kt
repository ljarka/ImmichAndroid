package com.github.ljarka.immich.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.ljarka.immich.android.server.ServerConfigurationScreen
import com.github.ljarka.immich.android.ui.theme.ImmichAndroidTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@Serializable
object ServerConfiguration

@Serializable
object Login

@Serializable
object Timeline

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            ImmichAndroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    NavHost(navController = navController, startDestination = ServerConfiguration) {
                        composable<ServerConfiguration> {
                            ServerConfigurationScreen(
                                modifier = Modifier.padding(innerPadding),
                                onServerConfigured = {
                                    navController.navigate(Login)
                                }
                            )
                        }
                        composable<Login> {
                            LoginScreen(
                                onAccept = {
                                    navController.navigate(Timeline)
                                },
                                modifier = Modifier.padding(innerPadding),
                            )
                        }

                        composable<Timeline> {
                            TimeLineScreen()
                        }
                    }
                }
            }
        }
    }
}
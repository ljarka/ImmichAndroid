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
import com.github.ljarka.immich.android.init.InitScreen
import com.github.ljarka.immich.android.ui.theme.ImmichAndroidTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@Serializable
object Init

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

                    NavHost(navController = navController, startDestination = Init) {
                        composable<Init> {
                            InitScreen(
                                modifier = Modifier.padding(innerPadding),
                                onInitialized = {
                                    navController.navigate(Timeline)
                                }
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
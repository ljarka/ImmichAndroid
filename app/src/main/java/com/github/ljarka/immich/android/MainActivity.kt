package com.github.ljarka.immich.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.ljarka.immich.android.ui.init.InitScreen
import com.github.ljarka.immich.android.ui.theme.ImmichAndroidTheme
import com.github.ljarka.immich.android.ui.timeline.TimelineScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable

@Serializable
object Init

@Serializable
object Timeline

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            ImmichAndroidTheme {

                val scrollBehavior = enterAlwaysScrollBehavior()
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(text = "Immich")
                            },
                            navigationIcon = {
                                Image(
                                    painter = painterResource(R.drawable.immich_logo),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .size(32.dp),
                                )
                            },
                            scrollBehavior = scrollBehavior,
                        )
                    }
                ) { innerPadding ->
                    NavHost(navController = navController, startDestination = Init) {
                        composable<Init> {
                            InitScreen(
                                modifier = Modifier.padding(innerPadding),
                                onInitialized = {
                                    navController.navigate(Timeline) {
                                        popUpTo(Init) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable<Timeline> {
                            TimelineScreen(modifier = Modifier.padding(innerPadding))
                        }
                    }
                }
            }
        }
    }
}
package com.github.ljarka.immich.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.enterAlwaysScrollBehavior
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.github.ljarka.immich.android.ui.image.ImageDetailsScreen
import com.github.ljarka.immich.android.ui.init.InitScreen
import com.github.ljarka.immich.android.ui.theme.ImmichAndroidTheme
import com.github.ljarka.immich.android.ui.timeline.TimelineScreen
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen(val route: String) {
    object Init : Screen("init")
    object Timeline : Screen("timeline")
    object ImageDetails : Screen("imageDetails/{assetId}") {
        fun createRoute(assetId: String) = "imageDetails/$assetId"
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
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
                    SharedTransitionLayout {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Init.route
                        ) {
                            composable(Screen.Init.route) {
                                InitScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    onInitialized = {
                                        navController.navigate(Screen.Timeline.route) {
                                            popUpTo(Screen.Init.route) { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable(Screen.Timeline.route) {
                                TimelineScreen(
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable,
                                    modifier = Modifier.padding(innerPadding),
                                    onImageClick = { assetId ->
                                        navController.navigate(
                                            Screen.ImageDetails.createRoute(assetId)
                                        )
                                    }
                                )
                            }
                            composable(
                                route = Screen.ImageDetails.route,
                                arguments = listOf(navArgument("assetId") {
                                    type = NavType.StringType
                                })
                            ) { backStackEntry ->
                                val assetId = backStackEntry.arguments?.getString("assetId")
                                var isPopBackStackCalled by remember { mutableStateOf(false) }
                                ImageDetailsScreen(
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@composable,
                                    onDismissRequest = {
                                        if (!isPopBackStackCalled) {
                                            isPopBackStackCalled = true
                                            navController.popBackStack()
                                        }
                                    },
                                    assetId = requireNotNull(assetId),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
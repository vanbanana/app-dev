package com.example.sketchto3view.ui.navigation

import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.sketchto3view.ui.crop.ManualCropScreen
import com.example.sketchto3view.ui.detail.ImagePreviewScreen
import com.example.sketchto3view.ui.detail.TaskDetailScreen
import com.example.sketchto3view.ui.history.HistoryScreen
import com.example.sketchto3view.ui.home.HomeScreen
import com.example.sketchto3view.ui.picker.ImagePickerScreen
import com.example.sketchto3view.ui.settings.SettingsScreen

private const val TRANSITION_DURATION = 300

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() },
                animationSpec = tween(TRANSITION_DURATION, easing = EaseOutCubic)
            ) + fadeIn(
                animationSpec = tween(TRANSITION_DURATION, easing = EaseOutCubic)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() },
                animationSpec = tween(TRANSITION_DURATION, easing = EaseInCubic)
            ) + fadeOut(
                animationSpec = tween(TRANSITION_DURATION, easing = EaseInCubic)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() },
                animationSpec = tween(TRANSITION_DURATION, easing = EaseOutCubic)
            ) + fadeIn(
                animationSpec = tween(TRANSITION_DURATION, easing = EaseOutCubic)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() },
                animationSpec = tween(TRANSITION_DURATION, easing = EaseInCubic)
            ) + fadeOut(
                animationSpec = tween(TRANSITION_DURATION, easing = EaseInCubic)
            )
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToImagePicker = {
                    navController.navigate(Screen.ImagePicker.route)
                },
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToTaskDetail = { taskId ->
                    navController.navigate(Screen.TaskDetail.createRoute(taskId))
                }
            )
        }

        composable(Screen.ImagePicker.route) {
            ImagePickerScreen(
                onNavigateBack = { navController.popBackStack() },
                onTasksCreated = { taskIds ->
                    navController.popBackStack()
                    if (taskIds.isNotEmpty()) {
                        navController.navigate(Screen.TaskDetail.createRoute(taskIds.first()))
                    }
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
            TaskDetailScreen(
                taskId = taskId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToManualCrop = {
                    navController.navigate(Screen.ManualCrop.createRoute(taskId))
                },
                onNavigateToImagePreview = { viewIndex ->
                    navController.navigate(Screen.ImagePreview.createRoute(taskId, viewIndex))
                }
            )
        }

        composable(
            route = Screen.ManualCrop.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
            ManualCropScreen(
                taskId = taskId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ImagePreview.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.StringType },
                navArgument("viewIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: return@composable
            val viewIndex = backStackEntry.arguments?.getInt("viewIndex") ?: 0
            ImagePreviewScreen(
                taskId = taskId,
                viewIndex = viewIndex,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

package com.example.sketchto3view.ui.navigation

/**
 * Sealed class defining all navigation routes in the app.
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object History : Screen("history")
    object ImagePicker : Screen("image_picker")
    object Settings : Screen("settings")
    object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: String) = "task_detail/$taskId"
    }
    object ManualCrop : Screen("manual_crop/{taskId}") {
        fun createRoute(taskId: String) = "manual_crop/$taskId"
    }
    object ImagePreview : Screen("image_preview/{taskId}/{viewIndex}") {
        fun createRoute(taskId: String, viewIndex: Int) = "image_preview/$taskId/$viewIndex"
    }
}

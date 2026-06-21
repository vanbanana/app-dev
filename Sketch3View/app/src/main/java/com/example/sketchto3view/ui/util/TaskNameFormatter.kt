package com.example.sketchto3view.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formats a task name using its index and creation timestamp.
 * Example output: "任务1 · 05/22 20:38"
 */
fun formatTaskName(index: Int, createdAt: Long): String {
    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    return "任务${index + 1} · ${sdf.format(Date(createdAt))}"
}

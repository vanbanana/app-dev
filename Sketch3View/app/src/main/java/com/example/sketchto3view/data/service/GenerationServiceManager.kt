package com.example.sketchto3view.data.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.sketchto3view.data.queue.TaskQueue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the lifecycle of GenerationForegroundService.
 * Starts the service when tasks are submitted, stops when all complete.
 * Provides a StateFlow of current service status for UI observation.
 */
@Singleton
class GenerationServiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskQueue: TaskQueue,
    private val applicationScope: CoroutineScope
) {

    data class ServiceStatus(
        val activeCount: Int = 0,
        val queuedCount: Int = 0,
        val completedCount: Int = 0,
        val totalCount: Int = 0,
        val isRunning: Boolean = false
    )

    private val _serviceStatus = MutableStateFlow(ServiceStatus())
    val serviceStatus: StateFlow<ServiceStatus> = _serviceStatus.asStateFlow()

    private var totalSubmitted = 0
    private var completedSoFar = 0
    private var isServiceRunning = false

    init {
        // Observe task queue state to update service status
        applicationScope.launch {
            combine(
                taskQueue.activeTaskCount,
                taskQueue.queueState
            ) { activeCount, queuedTasks ->
                Pair(activeCount, queuedTasks.size)
            }.collect { (activeCount, queuedCount) ->
                val newCompleted = totalSubmitted - activeCount - queuedCount
                if (newCompleted > completedSoFar) {
                    completedSoFar = newCompleted
                }

                _serviceStatus.value = ServiceStatus(
                    activeCount = activeCount,
                    queuedCount = queuedCount,
                    completedCount = completedSoFar.coerceAtLeast(0),
                    totalCount = totalSubmitted,
                    isRunning = isServiceRunning
                )

                // Stop service when all tasks are done
                if (isServiceRunning && activeCount == 0 && queuedCount == 0) {
                    stopService()
                }
            }
        }
    }

    /**
     * Called when new tasks are submitted. Starts the foreground service if not already running.
     */
    fun onTasksSubmitted(count: Int) {
        totalSubmitted += count
        if (!isServiceRunning && count > 0) {
            startService()
        }
    }

    /**
     * Resets counters when a batch is fully complete (for next batch tracking).
     */
    fun resetBatch() {
        totalSubmitted = 0
        completedSoFar = 0
    }

    private fun startService() {
        isServiceRunning = true
        val intent = Intent(context, GenerationForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun stopService() {
        isServiceRunning = false
        totalSubmitted = 0
        completedSoFar = 0
        val intent = Intent(context, GenerationForegroundService::class.java)
        context.stopService(intent)
        _serviceStatus.value = ServiceStatus()
    }
}

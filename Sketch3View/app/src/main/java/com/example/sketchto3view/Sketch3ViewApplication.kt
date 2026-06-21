package com.example.sketchto3view

import android.app.Application
import com.example.sketchto3view.data.local.TaskDao
import com.example.sketchto3view.domain.usecase.SubmitImagesUseCase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class Sketch3ViewApplication : Application() {

    @Inject
    lateinit var taskDao: TaskDao

    @Inject
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var submitImagesUseCase: SubmitImagesUseCase

    override fun onCreate() {
        super.onCreate()

        // On app startup, first try to resume pending downloads (tasks with saved URLs).
        // These tasks had their API call succeed but the image download was interrupted.
        // Then reset any remaining stuck tasks that don't have a pending URL.
        applicationScope.launch {
            // Resume pending downloads first (these have a chance of succeeding)
            submitImagesUseCase.resumePendingDownloads()

            // Reset any remaining stuck tasks without pending URLs
            // (these were interrupted before getting a URL, so they need full retry)
            taskDao.resetStuckTasks("PROCESSING", "FAILED", "任务被中断，请重试")
            taskDao.resetStuckTasks("QUEUED", "FAILED", "任务被中断，请重试")
        }
    }
}

package com.example.sketchto3view.di

import com.example.sketchto3view.data.local.TaskDao
import com.example.sketchto3view.data.queue.TaskQueue
import com.example.sketchto3view.data.service.GenerationServiceManager
import com.example.sketchto3view.data.service.NotificationHelper
import com.example.sketchto3view.domain.repository.TaskRepository
import com.example.sketchto3view.domain.service.CropperService
import com.example.sketchto3view.domain.service.DownloadManagerService
import com.example.sketchto3view.domain.service.ImageGenerationApi
import com.example.sketchto3view.domain.usecase.CropImageUseCase
import com.example.sketchto3view.domain.usecase.DownloadImagesUseCase
import com.example.sketchto3view.domain.usecase.SubmitImagesUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideSubmitImagesUseCase(
        taskRepository: TaskRepository,
        taskQueue: TaskQueue,
        imageGenerationApi: ImageGenerationApi,
        cropperService: CropperService,
        generationServiceManager: GenerationServiceManager,
        notificationHelper: NotificationHelper,
        taskDao: TaskDao
    ): SubmitImagesUseCase {
        return SubmitImagesUseCase(taskRepository, taskQueue, imageGenerationApi, cropperService, generationServiceManager, notificationHelper, taskDao)
    }

    @Provides
    fun provideCropImageUseCase(
        cropperService: CropperService,
        taskRepository: TaskRepository
    ): CropImageUseCase {
        return CropImageUseCase(cropperService, taskRepository)
    }

    @Provides
    fun provideDownloadImagesUseCase(
        downloadManagerService: DownloadManagerService,
        taskRepository: TaskRepository
    ): DownloadImagesUseCase {
        return DownloadImagesUseCase(downloadManagerService, taskRepository)
    }
}

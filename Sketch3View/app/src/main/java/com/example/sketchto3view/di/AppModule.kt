package com.example.sketchto3view.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.example.sketchto3view.data.api.ApiLayerConfig
import com.example.sketchto3view.data.api.DefaultImageGenerationApi
import com.example.sketchto3view.data.cropper.DefaultCropperService
import com.example.sketchto3view.data.download.DefaultDownloadManagerService
import com.example.sketchto3view.data.local.AppDatabase
import com.example.sketchto3view.data.local.TaskDao
import com.example.sketchto3view.data.queue.TaskQueue
import com.example.sketchto3view.data.repository.FileBasedTaskRepository
import com.example.sketchto3view.domain.repository.TaskRepository
import com.example.sketchto3view.domain.service.CropperService
import com.example.sketchto3view.domain.service.DownloadManagerService
import com.example.sketchto3view.domain.service.ImageGenerationApi
import com.example.sketchto3view.ui.settings.SettingsViewModel
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = Gson()

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(
            SettingsViewModel.PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }

    @Provides
    @Singleton
    fun provideApiLayerConfig(prefs: SharedPreferences): ApiLayerConfig {
        return ApiLayerConfig(
            prefs = prefs,
            timeoutMs = 60_000
        )
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(config: ApiLayerConfig): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .connectTimeout(config.timeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(config.timeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(config.timeoutMs, TimeUnit.MILLISECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "sketch3view_database"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao {
        return database.taskDao()
    }

    @Provides
    @Singleton
    fun provideTaskRepository(
        @ApplicationContext context: Context,
        taskDao: TaskDao,
        gson: Gson
    ): TaskRepository {
        return FileBasedTaskRepository(context, taskDao, gson)
    }

    @Provides
    @Singleton
    fun provideImageGenerationApi(
        config: ApiLayerConfig,
        httpClient: OkHttpClient
    ): ImageGenerationApi {
        return DefaultImageGenerationApi(config, httpClient)
    }

    @Provides
    @Singleton
    fun provideCropperService(): CropperService {
        return DefaultCropperService()
    }

    @Provides
    @Singleton
    fun provideDownloadManagerService(
        @ApplicationContext context: Context
    ): DownloadManagerService {
        return DefaultDownloadManagerService(context)
    }

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Provides
    @Singleton
    fun provideTaskQueue(applicationScope: CoroutineScope): TaskQueue {
        return TaskQueue(
            maxConcurrent = 3,
            dispatchDelayMs = 1000L,
            coroutineScope = applicationScope
        )
    }
}

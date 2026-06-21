package com.example.sketchto3view.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room database for the application.
 * Stores task metadata locally.
 */
@Database(entities = [TaskEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        /**
         * Migration from version 1 to 2: adds pendingImageUrl column to tasks table.
         * This column stores the image URL for background-resilient downloads.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN pendingImageUrl TEXT DEFAULT NULL")
            }
        }
    }
}

package com.vega.di

import android.content.Context
import androidx.room.Room
import com.vega.data.database.TaskDao
import com.vega.data.database.VegaDatabase
import com.vega.data.repository.TaskRepository
import com.vega.data.repository.TagRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Singleton
    @Provides
    fun provideVegaDatabase(
        @ApplicationContext context: Context
    ): VegaDatabase {
        return Room.databaseBuilder(
            context,
            VegaDatabase::class.java,
            "vega_database"
        )
        .fallbackToDestructiveMigration(true)
        .build()
    }
    
    @Singleton
    @Provides
    fun provideTaskDao(database: VegaDatabase): TaskDao {
        return database.taskDao()
    }
    
    @Singleton
    @Provides
    fun provideTaskRepository(
        taskDao: TaskDao,
        alarmScheduler: com.vega.alarms.TaskAlarmScheduler
    ): TaskRepository {
        return TaskRepository(taskDao, alarmScheduler)
    }

    @Singleton
    @Provides
    fun provideTagDao(database: VegaDatabase): com.vega.data.database.TagDao {
        return database.tagDao()
    }

    @Singleton
    @Provides
    fun provideTagRepository(tagDao: com.vega.data.database.TagDao): TagRepository {
        return TagRepository(tagDao)
    }
}

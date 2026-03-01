package com.echoran.flowfocus.di

import android.content.Context
import com.echoran.flowfocus.data.AppDatabase
import com.echoran.flowfocus.data.dao.FocusSessionDao
import com.echoran.flowfocus.data.dao.TaskDao
import com.echoran.flowfocus.data.dao.WhitelistedAppDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideTaskDao(appDatabase: AppDatabase): TaskDao {
        return appDatabase.taskDao()
    }

    @Provides
    fun provideFocusSessionDao(appDatabase: AppDatabase): FocusSessionDao {
        return appDatabase.focusSessionDao()
    }

    @Provides
    fun provideWhitelistedAppDao(appDatabase: AppDatabase): WhitelistedAppDao {
        return appDatabase.whitelistedAppDao()
    }
}

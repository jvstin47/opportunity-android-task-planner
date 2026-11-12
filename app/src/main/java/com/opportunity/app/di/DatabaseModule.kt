package com.opportunity.app.di

import android.content.Context
import androidx.room.Room
import com.opportunity.app.data.local.OpportunityDatabase
import com.opportunity.app.data.local.dao.OpportunityDao
import com.opportunity.app.data.local.dao.SessionDao
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
    fun provideDatabase(@ApplicationContext context: Context): OpportunityDatabase =
        Room.databaseBuilder(context, OpportunityDatabase::class.java, OpportunityDatabase.DATABASE_NAME)
            .addMigrations(OpportunityDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration() // acceptable pre-1.0; replace with real migrations before shipping
            .build()

    @Provides
    fun provideOpportunityDao(db: OpportunityDatabase): OpportunityDao = db.opportunityDao()

    @Provides
    fun provideSessionDao(db: OpportunityDatabase): SessionDao = db.sessionDao()
}

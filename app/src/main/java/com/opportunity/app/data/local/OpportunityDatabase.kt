package com.opportunity.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.opportunity.app.data.local.dao.OpportunityDao
import com.opportunity.app.data.local.dao.SessionDao
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.data.local.entity.SessionEntity

@Database(
    entities = [OpportunityEntity::class, SessionEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class OpportunityDatabase : RoomDatabase() {
    abstract fun opportunityDao(): OpportunityDao
    abstract fun sessionDao(): SessionDao

    companion object {
        const val DATABASE_NAME = "opportunity.db"
    }
}

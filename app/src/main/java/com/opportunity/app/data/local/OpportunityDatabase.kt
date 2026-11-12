package com.opportunity.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.opportunity.app.data.local.dao.OpportunityDao
import com.opportunity.app.data.local.dao.SessionDao
import com.opportunity.app.data.local.entity.OpportunityEntity
import com.opportunity.app.data.local.entity.SessionEntity

@Database(
    entities = [OpportunityEntity::class, SessionEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class OpportunityDatabase : RoomDatabase() {
    abstract fun opportunityDao(): OpportunityDao
    abstract fun sessionDao(): SessionDao

    companion object {
        const val DATABASE_NAME = "opportunity.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreate sessions table with foreign key constraint
                db.execSQL("CREATE TABLE IF NOT EXISTS sessions_new (id TEXT NOT NULL PRIMARY KEY, opportunityId TEXT NOT NULL, startedAtEpochMillis INTEGER NOT NULL, durationMinutes INTEGER NOT NULL, mode TEXT NOT NULL, note TEXT, FOREIGN KEY (opportunityId) REFERENCES opportunities(id) ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sessions_opportunityId ON sessions_new(opportunityId)")
                db.execSQL("INSERT INTO sessions_new SELECT * FROM sessions")
                db.execSQL("DROP TABLE sessions")
                db.execSQL("ALTER TABLE sessions_new RENAME TO sessions")
            }
        }
    }
}

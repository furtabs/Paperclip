package com.furtabs.paperclip.tracking.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracking_history ADD COLUMN firstDate TEXT DEFAULT NULL")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Schema in version 3 contains no structural changes for tracking_history currently;
        // this no-op migration allows Room to validate the existing schema.
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // No structural changes between v3 and v4; this no-op migration keeps existing data intact
    }
}

@Entity(tableName = "tracking_history")
data class TrackingEntity(
    @PrimaryKey val code: String,
    val description: String,
    val lastStatus: String,
    val lastDate: String,
    val firstDate: String? = null,
    val savedAt: Long = System.currentTimeMillis()
)
@Dao
interface TrackingDao {
    @Query("SELECT * FROM tracking_history ORDER BY savedAt DESC")
    fun getAllTracking(): Flow<List<TrackingEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracking(tracking: TrackingEntity)
    @Query("DELETE FROM tracking_history WHERE code = :code")
    suspend fun deleteTracking(code: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trackings: List<TrackingEntity>)
    @Query("UPDATE tracking_history SET description = :newDescription WHERE code = :code")
    suspend fun updateDescription(code: String, newDescription: String)
}
@Database(entities = [TrackingEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trackingDao(): TrackingDao
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kaeru_track_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
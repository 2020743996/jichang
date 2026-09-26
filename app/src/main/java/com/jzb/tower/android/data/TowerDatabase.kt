package com.jzb.tower.android.data

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

@Entity(tableName = "app_snapshot")
data class SnapshotEntity(@PrimaryKey val id: Int = 0, val payload: String)

@Dao
interface SnapshotDao {
    @Query("SELECT payload FROM app_snapshot WHERE id = 0")
    fun observe(): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(snapshot: SnapshotEntity)
}

@Database(entities = [SnapshotEntity::class], version = 1, exportSchema = false)
abstract class TowerDatabase : RoomDatabase() {
    abstract fun snapshots(): SnapshotDao

    companion object {
        @Volatile private var instance: TowerDatabase? = null
        fun get(context: Context): TowerDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, TowerDatabase::class.java, "tower-android.db")
                .build().also { instance = it }
        }
    }
}

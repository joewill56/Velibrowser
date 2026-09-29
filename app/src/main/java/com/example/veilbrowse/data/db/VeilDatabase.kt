package com.example.veilbrowse.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.veilbrowse.data.model.BlockedTrackerEntity
import com.example.veilbrowse.data.model.BookmarkEntity
import com.example.veilbrowse.data.model.TabEntity

@Database(
    entities = [
        TabEntity::class,
        BlockedTrackerEntity::class,
        BookmarkEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VeilDatabase : RoomDatabase() {
    abstract fun tabDao(): TabDao
    abstract fun trackerDao(): TrackerDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: VeilDatabase? = null

        fun getInstance(context: Context): VeilDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VeilDatabase::class.java,
                    "veil_browse_db"
                ).fallbackToDestructiveMigration(false).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.example.veilbrowse.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.veilbrowse.data.model.BlockedTrackerEntity
import com.example.veilbrowse.data.model.BookmarkEntity
import com.example.veilbrowse.data.model.TabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TabDao {
    @Query("SELECT * FROM tabs ORDER BY createdAt ASC")
    fun getAllTabs(): Flow<List<TabEntity>>

    @Query("SELECT * FROM tabs WHERE id = :id LIMIT 1")
    suspend fun getTabById(id: String): TabEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabEntity)

    @Update
    suspend fun updateTab(tab: TabEntity)

    @Delete
    suspend fun deleteTab(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun deleteTabById(id: String)

    @Query("DELETE FROM tabs")
    suspend fun deleteAllTabs()
}

@Dao
interface TrackerDao {
    @Query("SELECT * FROM blocked_trackers ORDER BY timestamp DESC LIMIT 100")
    fun getRecentBlockedTrackers(): Flow<List<BlockedTrackerEntity>>

    @Query("SELECT COUNT(*) FROM blocked_trackers")
    fun getTotalBlockedCount(): Flow<Int>

    @Insert
    suspend fun insertBlockedTracker(tracker: BlockedTrackerEntity)

    @Query("DELETE FROM blocked_trackers")
    suspend fun clearAllBlockedTrackers()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmark(id: String)
}

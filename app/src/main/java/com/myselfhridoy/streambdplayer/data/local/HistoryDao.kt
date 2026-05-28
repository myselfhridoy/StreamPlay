package com.myselfhridoy.streambdplayer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myselfhridoy.streambdplayer.data.models.HistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_items ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: HistoryItem)

    @Query("DELETE FROM history_items WHERE url = :url")
    suspend fun deleteHistoryItem(url: String)

    @Query("DELETE FROM history_items")
    suspend fun clearAll()
}

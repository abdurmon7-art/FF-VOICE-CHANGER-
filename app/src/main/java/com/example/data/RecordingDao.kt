package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAllRecordings(): Flow<List<RecordingItem>>

    @Query("SELECT * FROM recordings WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteRecordings(): Flow<List<RecordingItem>>

    @Query("SELECT * FROM recordings WHERE id = :id LIMIT 1")
    suspend fun getRecordingById(id: Long): RecordingItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(item: RecordingItem): Long

    @Update
    suspend fun updateRecording(item: RecordingItem)

    @Delete
    suspend fun deleteRecording(item: RecordingItem)

    @Query("UPDATE recordings SET title = :newTitle WHERE id = :id")
    suspend fun renameRecording(id: Long, newTitle: String)

    @Query("UPDATE recordings SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long, isFavorite: Boolean)
}

package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE mediaType = :type ORDER BY createdAt DESC")
    fun getDownloadsByType(type: String): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
    suspend fun getDownloadById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE downloadManagerId = :dmId LIMIT 1")
    suspend fun getDownloadByManagerId(dmId: Long): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity): Long

    @Update
    suspend fun update(download: DownloadEntity)

    @Delete
    suspend fun delete(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE downloads SET status = :status, localFilePath = :filePath, fileSizeBytes = :fileSize WHERE downloadManagerId = :dmId")
    suspend fun updateDownloadStatus(dmId: Long, status: String, filePath: String?, fileSize: Long)

    @Query("UPDATE downloads SET status = :status, localFilePath = :filePath, fileSizeBytes = :fileSize WHERE id = :id")
    suspend fun updateStatusById(id: Long, status: String, filePath: String?, fileSize: Long)
}

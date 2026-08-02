package com.snapload.app.data.db

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): LiveData<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status = :status ORDER BY createdAt DESC")
    fun getDownloadsByStatus(status: String): LiveData<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity): Long

    @Update
    suspend fun update(download: DownloadEntity)

    @Delete
    suspend fun delete(download: DownloadEntity)

    @Query("UPDATE downloads SET status = :status, progress = :progress WHERE id = :id")
    suspend fun updateProgress(id: Long, status: String, progress: Int)

    @Query("UPDATE downloads SET status = :status, filePath = :filePath WHERE downloadManagerId = :dmId")
    suspend fun updateByDownloadManagerId(dmId: Long, status: String, filePath: String)

    @Query("SELECT * FROM downloads WHERE downloadManagerId = :dmId LIMIT 1")
    suspend fun getByDownloadManagerId(dmId: Long): DownloadEntity?

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM downloads WHERE status = 'downloading'")
    suspend fun getActiveDownloadCount(): Int
}

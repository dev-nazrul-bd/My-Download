package com.example.data

import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val dao: DownloadDao) {
    val allDownloads: Flow<List<DownloadEntity>> = dao.getAllDownloads()

    fun getDownloadsByType(type: String): Flow<List<DownloadEntity>> = dao.getDownloadsByType(type)

    suspend fun insert(download: DownloadEntity): Long = dao.insert(download)

    suspend fun update(download: DownloadEntity) = dao.update(download)

    suspend fun delete(download: DownloadEntity) = dao.delete(download)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun getById(id: Long) = dao.getDownloadById(id)

    suspend fun getByManagerId(dmId: Long) = dao.getDownloadByManagerId(dmId)

    suspend fun updateStatus(dmId: Long, status: String, filePath: String?, fileSize: Long) =
        dao.updateDownloadStatus(dmId, status, filePath, fileSize)
}

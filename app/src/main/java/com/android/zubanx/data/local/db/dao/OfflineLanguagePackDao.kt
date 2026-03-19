package com.android.zubanx.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.zubanx.data.local.db.entity.OfflineLanguagePackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineLanguagePackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OfflineLanguagePackEntity): Long

    @Query("SELECT * FROM offline_language_packs WHERE languageCode = :code LIMIT 1")
    suspend fun getByLanguageCode(code: String): OfflineLanguagePackEntity?

    @Query("UPDATE offline_language_packs SET isDownloaded = :isDownloaded, downloadedAt = :downloadedAt WHERE languageCode = :code")
    suspend fun updateDownloadStatus(code: String, isDownloaded: Boolean, downloadedAt: Long?)

    @Query("SELECT * FROM offline_language_packs")
    fun getAll(): Flow<List<OfflineLanguagePackEntity>>
}

package com.android.zubanx.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.zubanx.data.local.db.entity.DictionaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DictionaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DictionaryEntity): Long

    @Query("SELECT * FROM dictionary WHERE word = :word AND language = :language LIMIT 1")
    suspend fun getByWordAndLanguage(word: String, language: String): DictionaryEntity?

    @Query("SELECT * FROM dictionary ORDER BY timestamp DESC")
    fun getAll(): Flow<List<DictionaryEntity>>
}

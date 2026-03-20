package com.android.zubanx.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.zubanx.data.local.db.entity.FavouriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavouriteEntity): Long

    @Query("SELECT * FROM favourites ORDER BY timestamp DESC")
    fun getAll(): Flow<List<FavouriteEntity>>

    @Query("DELETE FROM favourites WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE sourceText = :sourceText)")
    suspend fun existsBySourceText(sourceText: String): Boolean

    @Query("SELECT * FROM favourites WHERE category = :category ORDER BY timestamp DESC")
    fun getAllByCategory(category: String): Flow<List<FavouriteEntity>>
}

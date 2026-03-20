package com.android.zubanx.domain.repository

import com.android.zubanx.domain.model.Favourite
import kotlinx.coroutines.flow.Flow

interface FavouriteRepository {
    fun getAll(): Flow<List<Favourite>>
    fun getByCategory(category: String): Flow<List<Favourite>>
    suspend fun add(favourite: Favourite)
    suspend fun remove(id: Long)
    suspend fun isFavourite(sourceText: String): Boolean
}

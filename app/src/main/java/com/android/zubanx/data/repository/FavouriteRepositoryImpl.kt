package com.android.zubanx.data.repository

import com.android.zubanx.data.local.db.dao.FavouriteDao
import com.android.zubanx.data.local.db.mapper.toDomain
import com.android.zubanx.data.local.db.mapper.toEntity
import com.android.zubanx.domain.model.Favourite
import com.android.zubanx.domain.repository.FavouriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavouriteRepositoryImpl(private val dao: FavouriteDao) : FavouriteRepository {

    override fun getAll(): Flow<List<Favourite>> =
        dao.getAll().map { it.map { entity -> entity.toDomain() } }

    override suspend fun add(favourite: Favourite) {
        dao.insert(favourite.toEntity())
    }

    override suspend fun remove(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun isFavourite(sourceText: String): Boolean =
        dao.existsBySourceText(sourceText)
}

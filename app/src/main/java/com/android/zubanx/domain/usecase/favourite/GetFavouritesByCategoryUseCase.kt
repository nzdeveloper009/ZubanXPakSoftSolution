package com.android.zubanx.domain.usecase.favourite

import com.android.zubanx.domain.model.Favourite
import com.android.zubanx.domain.repository.FavouriteRepository
import kotlinx.coroutines.flow.Flow

class GetFavouritesByCategoryUseCase(private val repository: FavouriteRepository) {
    operator fun invoke(category: String): Flow<List<Favourite>> =
        repository.getByCategory(category)
}

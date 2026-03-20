package com.android.zubanx.domain.usecase.favourite

import com.android.zubanx.domain.repository.FavouriteRepository

class DeleteFavouriteUseCase(private val repository: FavouriteRepository) {
    suspend operator fun invoke(id: Long) = repository.remove(id)
}

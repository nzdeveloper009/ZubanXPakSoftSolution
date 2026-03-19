package com.android.zubanx.data.repository

import app.cash.turbine.test
import com.android.zubanx.data.local.db.dao.FavouriteDao
import com.android.zubanx.data.local.db.entity.FavouriteEntity
import com.android.zubanx.domain.model.Favourite
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FavouriteRepositoryImplTest {

    private val dao: FavouriteDao = mockk(relaxed = true)
    private val repo = FavouriteRepositoryImpl(dao)

    @Test
    fun `getAll maps entities to domain models`() = runTest {
        every { dao.getAll() } returns flowOf(listOf(
            FavouriteEntity(
                id = 1L, sourceText = "Cat", translatedText = "Gato",
                sourceLang = "en", targetLang = "es", timestamp = 2000L
            )
        ))
        repo.getAll().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Cat", list[0].sourceText)
            awaitComplete()
        }
    }

    @Test
    fun `isFavourite delegates to dao`() = runTest {
        coEvery { dao.existsBySourceText("Cat") } returns true
        assertTrue(repo.isFavourite("Cat"))
    }

    @Test
    fun `add calls dao insert`() = runTest {
        repo.add(Favourite(
            sourceText = "Dog", translatedText = "Perro",
            sourceLang = "en", targetLang = "es", timestamp = 3000L
        ))
        coVerify { dao.insert(any()) }
    }
}

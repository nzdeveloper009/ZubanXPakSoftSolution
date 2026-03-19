package com.android.zubanx.data.repository

import app.cash.turbine.test
import com.android.zubanx.data.local.db.dao.TranslationDao
import com.android.zubanx.data.local.db.entity.TranslationEntity
import com.android.zubanx.domain.model.Translation
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationRepositoryImplTest {

    private val dao: TranslationDao = mockk(relaxed = true)
    private val repo = TranslationRepositoryImpl(dao)

    @Test
    fun `getHistory maps entities to domain models`() = runTest {
        every { dao.getHistory() } returns flowOf(listOf(
            TranslationEntity(
                id = 1L, sourceText = "Hi", translatedText = "Hola",
                sourceLang = "en", targetLang = "es", expert = "DEFAULT", timestamp = 1000L
            )
        ))
        repo.getHistory().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("Hi", list[0].sourceText)
            awaitComplete()
        }
    }

    @Test
    fun `saveTranslation calls dao insert`() = runTest {
        repo.saveTranslation(Translation(
            sourceText = "Hello", translatedText = "Hola",
            sourceLang = "en", targetLang = "es", expert = "DEFAULT", timestamp = 1000L
        ))
        coVerify { dao.insert(any()) }
    }

    @Test
    fun `clearHistory calls dao clearAll`() = runTest {
        repo.clearHistory()
        coVerify { dao.clearAll() }
    }
}

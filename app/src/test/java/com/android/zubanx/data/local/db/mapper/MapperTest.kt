package com.android.zubanx.data.local.db.mapper

import com.android.zubanx.data.local.db.entity.*
import com.android.zubanx.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class MapperTest {

    @Test
    fun `TranslationEntity toDomain preserves all fields`() {
        val entity = TranslationEntity(
            id = 1L, sourceText = "Hi", translatedText = "Hola",
            sourceLang = "en", targetLang = "es", expert = "GPT", timestamp = 1000L
        )
        val model = entity.toDomain()
        assertEquals(1L, model.id)
        assertEquals("Hi", model.sourceText)
        assertEquals("GPT", model.expert)
    }

    @Test
    fun `Translation toEntity preserves all fields`() {
        val model = Translation(
            id = 2L, sourceText = "Hi", translatedText = "Hola",
            sourceLang = "en", targetLang = "es", expert = "GPT", timestamp = 1000L
        )
        val entity = model.toEntity()
        assertEquals(2L, entity.id)
        assertEquals("GPT", entity.expert)
    }

    @Test
    fun `FavouriteEntity toDomain preserves all fields`() {
        val entity = FavouriteEntity(
            id = 2L, sourceText = "Cat", translatedText = "Gato",
            sourceLang = "en", targetLang = "es", timestamp = 2000L
        )
        val model = entity.toDomain()
        assertEquals(2L, model.id)
        assertEquals("Cat", model.sourceText)
    }

    @Test
    fun `DictionaryEntity toDomain maps examples list`() {
        val entity = DictionaryEntity(
            id = 3L, word = "run", language = "en", definition = "to move fast",
            phonetic = "/rʌn/", partOfSpeech = "verb",
            examples = listOf("I run daily", "She runs fast"), timestamp = 3000L
        )
        val model = entity.toDomain()
        assertEquals(2, model.examples.size)
        assertEquals("I run daily", model.examples[0])
    }

    @Test
    fun `DictionaryEntry toEntity maps examples list`() {
        val model = DictionaryEntry(
            word = "run", language = "en", definition = "to move",
            examples = listOf("I run daily"), timestamp = 3000L
        )
        val entity = model.toEntity()
        assertEquals(listOf("I run daily"), entity.examples)
    }

    @Test
    fun `OfflineLanguagePackEntity toDomain preserves isDownloaded`() {
        val entity = OfflineLanguagePackEntity(
            id = 4L, languageCode = "es", languageName = "Spanish",
            isDownloaded = true, downloadedAt = 4000L
        )
        val model = entity.toDomain()
        assertEquals("es", model.languageCode)
        assertEquals(true, model.isDownloaded)
    }
}

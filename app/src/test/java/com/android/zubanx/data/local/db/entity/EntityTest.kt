package com.android.zubanx.data.local.db.entity

import org.junit.Assert.assertEquals
import org.junit.Test

class EntityTest {

    @Test
    fun `TranslationEntity holds fields correctly`() {
        val entity = TranslationEntity(
            id = 1L, sourceText = "Hello", translatedText = "Hola",
            sourceLang = "en", targetLang = "es", expert = "DEFAULT", timestamp = 1000L
        )
        assertEquals("Hello", entity.sourceText)
        assertEquals("DEFAULT", entity.expert)
    }

    @Test
    fun `DictionaryEntity stores examples as list`() {
        val entity = DictionaryEntity(
            word = "run", language = "en", definition = "to move",
            examples = listOf("I run daily"), timestamp = 2000L
        )
        assertEquals("run", entity.word)
        assertEquals(listOf("I run daily"), entity.examples)
    }

    @Test
    fun `OfflineLanguagePackEntity isDownloaded defaults to false`() {
        val entity = OfflineLanguagePackEntity(languageCode = "es", languageName = "Spanish")
        assertEquals(false, entity.isDownloaded)
    }
}

package com.android.zubanx.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelTest {

    @Test
    fun `Translation model holds correct fields`() {
        val t = Translation(
            id = 1L, sourceText = "Hello", translatedText = "Hola",
            sourceLang = "en", targetLang = "es", expert = "DEFAULT", timestamp = 1000L
        )
        assertEquals("Hello", t.sourceText)
        assertEquals("DEFAULT", t.expert)
    }

    @Test
    fun `Favourite model holds correct fields`() {
        val f = Favourite(
            id = 1L, sourceText = "Cat", translatedText = "Gato",
            sourceLang = "en", targetLang = "es", timestamp = 2000L
        )
        assertEquals("Cat", f.sourceText)
        assertEquals("es", f.targetLang)
    }

    @Test
    fun `DictionaryEntry examples default to empty list`() {
        val d = DictionaryEntry(
            word = "run", language = "en", definition = "to move fast", timestamp = 3000L
        )
        assertTrue(d.examples.isEmpty())
    }

    @Test
    fun `OfflineLanguagePack isDownloaded defaults to false`() {
        val pack = OfflineLanguagePack(languageCode = "es", languageName = "Spanish")
        assertEquals(false, pack.isDownloaded)
    }
}

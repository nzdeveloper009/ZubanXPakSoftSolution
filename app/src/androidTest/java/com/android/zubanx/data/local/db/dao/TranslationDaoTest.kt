package com.android.zubanx.data.local.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.zubanx.data.local.db.ZubanDatabase
import com.android.zubanx.data.local.db.entity.TranslationEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TranslationDaoTest {

    private lateinit var db: ZubanDatabase
    private lateinit var dao: TranslationDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), ZubanDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.translationDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun insertAndGetHistory() = runTest {
        val entity = TranslationEntity(
            sourceText = "Hello", translatedText = "Hola",
            sourceLang = "en", targetLang = "es", expert = "DEFAULT", timestamp = 1000L
        )
        dao.insert(entity)
        val history = dao.getHistory().first()
        assertEquals(1, history.size)
        assertEquals("Hello", history[0].sourceText)
    }

    @Test
    fun deleteById() = runTest {
        val id = dao.insert(TranslationEntity(
            sourceText = "Bye", translatedText = "Adios",
            sourceLang = "en", targetLang = "es", expert = "DEFAULT", timestamp = 2000L
        ))
        dao.deleteById(id)
        assertTrue(dao.getHistory().first().isEmpty())
    }

    @Test
    fun clearAll() = runTest {
        repeat(3) { i ->
            dao.insert(TranslationEntity(
                sourceText = "Text$i", translatedText = "Trans$i",
                sourceLang = "en", targetLang = "es", expert = "DEFAULT", timestamp = i.toLong()
            ))
        }
        dao.clearAll()
        assertTrue(dao.getHistory().first().isEmpty())
    }
}

package com.android.zubanx.data.local.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.zubanx.data.local.db.ZubanDatabase
import com.android.zubanx.data.local.db.entity.FavouriteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavouriteDaoTest {

    private lateinit var db: ZubanDatabase
    private lateinit var dao: FavouriteDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), ZubanDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.favouriteDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun insertAndGetAll() = runTest {
        dao.insert(FavouriteEntity(
            sourceText = "Cat", translatedText = "Gato",
            sourceLang = "en", targetLang = "es", timestamp = 1000L
        ))
        val list = dao.getAll().first()
        assertEquals(1, list.size)
        assertEquals("Cat", list[0].sourceText)
    }

    @Test
    fun deleteById() = runTest {
        val id = dao.insert(FavouriteEntity(
            sourceText = "Dog", translatedText = "Perro",
            sourceLang = "en", targetLang = "es", timestamp = 2000L
        ))
        dao.deleteById(id)
        assertTrue(dao.getAll().first().isEmpty())
    }

    @Test
    fun existsBySourceText() = runTest {
        dao.insert(FavouriteEntity(
            sourceText = "Sun", translatedText = "Sol",
            sourceLang = "en", targetLang = "es", timestamp = 3000L
        ))
        assertTrue(dao.existsBySourceText("Sun"))
        assertFalse(dao.existsBySourceText("Moon"))
    }
}

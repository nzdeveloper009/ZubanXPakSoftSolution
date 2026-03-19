package com.android.zubanx.data.local.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.zubanx.data.local.db.ZubanDatabase
import com.android.zubanx.data.local.db.entity.DictionaryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DictionaryDaoTest {

    private lateinit var db: ZubanDatabase
    private lateinit var dao: DictionaryDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), ZubanDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.dictionaryDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun insertAndGetByWordAndLanguage() = runTest {
        dao.insert(DictionaryEntity(
            word = "run", language = "en", definition = "to move fast",
            examplesJson = """["I run daily"]""", timestamp = 1000L
        ))
        val result = dao.getByWordAndLanguage("run", "en")
        assertNotNull(result)
        assertEquals("run", result!!.word)
    }

    @Test
    fun getByWordReturnsNullWhenNotCached() = runTest {
        assertNull(dao.getByWordAndLanguage("unknown", "en"))
    }

    @Test
    fun getAll() = runTest {
        repeat(2) { i ->
            dao.insert(DictionaryEntity(
                word = "word$i", language = "en", definition = "def$i",
                examplesJson = "[]", timestamp = i.toLong()
            ))
        }
        assertEquals(2, dao.getAll().first().size)
    }
}

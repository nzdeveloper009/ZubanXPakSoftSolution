package com.android.zubanx.data.local.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.zubanx.data.local.db.ZubanDatabase
import com.android.zubanx.data.local.db.entity.OfflineLanguagePackEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfflineLanguagePackDaoTest {

    private lateinit var db: ZubanDatabase
    private lateinit var dao: OfflineLanguagePackDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), ZubanDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.offlineLanguagePackDao()
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun insertAndGetByCode() = runTest {
        dao.insert(OfflineLanguagePackEntity(
            languageCode = "es", languageName = "Spanish", isDownloaded = false
        ))
        val result = dao.getByLanguageCode("es")
        assertNotNull(result)
        assertFalse(result!!.isDownloaded)
    }

    @Test
    fun updateDownloadStatus() = runTest {
        dao.insert(OfflineLanguagePackEntity(languageCode = "fr", languageName = "French"))
        dao.updateDownloadStatus("fr", isDownloaded = true, downloadedAt = 5000L)
        val result = dao.getByLanguageCode("fr")
        assertTrue(result!!.isDownloaded)
        assertEquals(5000L, result.downloadedAt)
    }

    @Test
    fun getAll() = runTest {
        dao.insert(OfflineLanguagePackEntity(languageCode = "de", languageName = "German"))
        dao.insert(OfflineLanguagePackEntity(languageCode = "it", languageName = "Italian"))
        assertEquals(2, dao.getAll().first().size)
    }
}

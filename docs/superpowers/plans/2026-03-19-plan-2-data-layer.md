# Data Layer Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete local data layer — Room database with 4 entities/DAOs, DataStore preferences, domain models, mappers, repository interfaces and implementations, and Koin wiring.

**Architecture:** Domain models in `domain/model/` (pure Kotlin), Room entities in `data/local/db/entity/`, mappers in `data/local/db/mapper/`. Repository interfaces in `domain/repository/`, implementations in `data/repository/`. All wired via Koin in `core/di/`. DAO tests are instrumented (androidTest) using in-memory Room.

**Tech Stack:** Room 2.7.0-alpha13 (KSP, group `androidx.room`), DataStore Preferences 1.1.4, Koin 4.1.1 BOM, Kotlin Coroutines/Flow, MockK + Turbine (JVM unit tests), AndroidJUnit4 (DAO instrumented tests).

---

## Chunk 1: Build Config, Domain Models, Entities, Mappers

### Task 1: Add room-testing dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add `room-testing` to `libs.versions.toml`**

In `gradle/libs.versions.toml`, under the `# Room` libraries section, add:
```toml
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
```

- [ ] **Step 2: Add test dependencies to `app/build.gradle.kts`**

In `app/build.gradle.kts`, under test dependencies, add:
```kotlin
androidTestImplementation(libs.room.testing)
androidTestImplementation(libs.kotlinx.coroutines.test)
```

Note: `kotlinx.coroutines.test` is already in `testImplementation` but must also be in `androidTestImplementation` so that `runTest` is available in instrumented DAO tests.

- [ ] **Step 3: Sync and verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**
```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add room-testing dependency for DAO instrumented tests"
```

---

### Task 2: Domain Models

**Files:**
- Create: `app/src/main/java/com/android/zubanx/domain/model/Translation.kt`
- Create: `app/src/main/java/com/android/zubanx/domain/model/Favourite.kt`
- Create: `app/src/main/java/com/android/zubanx/domain/model/DictionaryEntry.kt`
- Create: `app/src/main/java/com/android/zubanx/domain/model/OfflineLanguagePack.kt`
- Test: `app/src/test/java/com/android/zubanx/domain/model/DomainModelTest.kt`

- [ ] **Step 1: Write failing domain model test**

Create `app/src/test/java/com/android/zubanx/domain/model/DomainModelTest.kt`:
```kotlin
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
```

- [ ] **Step 2: Run test to confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.domain.model.DomainModelTest"`
Expected: FAIL — classes not found

- [ ] **Step 3: Create `Translation.kt`**
```kotlin
package com.android.zubanx.domain.model

data class Translation(
    val id: Long = 0L,
    val sourceText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val expert: String,
    val timestamp: Long
)
```

- [ ] **Step 4: Create `Favourite.kt`**
```kotlin
package com.android.zubanx.domain.model

data class Favourite(
    val id: Long = 0L,
    val sourceText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val timestamp: Long
)
```

- [ ] **Step 5: Create `DictionaryEntry.kt`**
```kotlin
package com.android.zubanx.domain.model

data class DictionaryEntry(
    val id: Long = 0L,
    val word: String,
    val language: String,
    val definition: String,
    val phonetic: String? = null,
    val partOfSpeech: String? = null,
    val examples: List<String> = emptyList(),
    val timestamp: Long
)
```

- [ ] **Step 6: Create `OfflineLanguagePack.kt`**
```kotlin
package com.android.zubanx.domain.model

data class OfflineLanguagePack(
    val id: Long = 0L,
    val languageCode: String,
    val languageName: String,
    val isDownloaded: Boolean = false,
    val downloadedAt: Long? = null
)
```

- [ ] **Step 7: Run tests to confirm they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.domain.model.DomainModelTest"`
Expected: PASS — 4 tests

- [ ] **Step 8: Commit**
```bash
git add app/src/main/java/com/android/zubanx/domain/model/ app/src/test/java/com/android/zubanx/domain/model/
git commit -m "feat(domain): add Translation, Favourite, DictionaryEntry, OfflineLanguagePack models"
```

---

### Task 3: Room Entities + TypeConverter

**Files:**
- Create: `app/src/main/java/com/android/zubanx/data/local/db/converter/ListStringConverter.kt`
- Create: `app/src/main/java/com/android/zubanx/data/local/db/entity/TranslationEntity.kt`
- Create: `app/src/main/java/com/android/zubanx/data/local/db/entity/FavouriteEntity.kt`
- Create: `app/src/main/java/com/android/zubanx/data/local/db/entity/DictionaryEntity.kt`
- Create: `app/src/main/java/com/android/zubanx/data/local/db/entity/OfflineLanguagePackEntity.kt`
- Test: `app/src/test/java/com/android/zubanx/data/local/db/entity/EntityTest.kt`

- [ ] **Step 1: Write failing entity test**

Create `app/src/test/java/com/android/zubanx/data/local/db/entity/EntityTest.kt`:
```kotlin
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
    fun `DictionaryEntity stores examplesJson as string`() {
        val entity = DictionaryEntity(
            word = "run", language = "en", definition = "to move",
            examplesJson = """["I run daily"]""", timestamp = 2000L
        )
        assertEquals("run", entity.word)
        assertEquals("""["I run daily"]""", entity.examplesJson)
    }

    @Test
    fun `OfflineLanguagePackEntity isDownloaded defaults to false`() {
        val entity = OfflineLanguagePackEntity(languageCode = "es", languageName = "Spanish")
        assertEquals(false, entity.isDownloaded)
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.local.db.entity.EntityTest"`
Expected: FAIL — classes not found

- [ ] **Step 3: Create `ListStringConverter.kt`**
```kotlin
package com.android.zubanx.data.local.db.converter

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class ListStringConverter {
    @TypeConverter
    fun fromList(list: List<String>): String =
        Json.encodeToString(ListSerializer(String.serializer()), list)

    @TypeConverter
    fun toList(json: String): List<String> =
        runCatching { Json.decodeFromString(ListSerializer(String.serializer()), json) }
            .getOrDefault(emptyList())
}
```

- [ ] **Step 4: Create `TranslationEntity.kt`**
```kotlin
package com.android.zubanx.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translations")
data class TranslationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sourceText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val expert: String,
    val timestamp: Long
)
```

- [ ] **Step 5: Create `FavouriteEntity.kt`**
```kotlin
package com.android.zubanx.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sourceText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val timestamp: Long
)
```

- [ ] **Step 6: Create `DictionaryEntity.kt`**
```kotlin
package com.android.zubanx.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dictionary")
data class DictionaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val word: String,
    val language: String,
    val definition: String,
    val phonetic: String? = null,
    val partOfSpeech: String? = null,
    val examplesJson: String = "[]",
    val timestamp: Long
)
```

- [ ] **Step 7: Create `OfflineLanguagePackEntity.kt`**
```kotlin
package com.android.zubanx.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_language_packs")
data class OfflineLanguagePackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val languageCode: String,
    val languageName: String,
    val isDownloaded: Boolean = false,
    val downloadedAt: Long? = null
)
```

- [ ] **Step 8: Run entity tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.local.db.entity.EntityTest"`
Expected: PASS — 3 tests

- [ ] **Step 9: Commit**
```bash
git add app/src/main/java/com/android/zubanx/data/local/db/
git add app/src/test/java/com/android/zubanx/data/local/db/entity/
git commit -m "feat(data): add Room entities (Translation, Favourite, Dictionary, OfflinePack) and ListStringConverter"
```

---

### Task 4: Mappers

**Files:**
- Create: `app/src/main/java/com/android/zubanx/data/local/db/mapper/TranslationMapper.kt`
- Create: `app/src/main/java/com/android/zubanx/data/local/db/mapper/FavouriteMapper.kt`
- Create: `app/src/main/java/com/android/zubanx/data/local/db/mapper/DictionaryMapper.kt`
- Create: `app/src/main/java/com/android/zubanx/data/local/db/mapper/OfflineLanguagePackMapper.kt`
- Test: `app/src/test/java/com/android/zubanx/data/local/db/mapper/MapperTest.kt`

- [ ] **Step 1: Write failing mapper test**

Create `app/src/test/java/com/android/zubanx/data/local/db/mapper/MapperTest.kt`:
```kotlin
package com.android.zubanx.data.local.db.mapper

import com.android.zubanx.data.local.db.entity.*
import com.android.zubanx.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `DictionaryEntity toDomain deserializes examples`() {
        val entity = DictionaryEntity(
            id = 3L, word = "run", language = "en", definition = "to move fast",
            phonetic = "/rʌn/", partOfSpeech = "verb",
            examplesJson = """["I run daily","She runs fast"]""", timestamp = 3000L
        )
        val model = entity.toDomain()
        assertEquals(2, model.examples.size)
        assertEquals("I run daily", model.examples[0])
    }

    @Test
    fun `DictionaryEntry toEntity serializes examples`() {
        val model = DictionaryEntry(
            word = "run", language = "en", definition = "to move",
            examples = listOf("I run daily"), timestamp = 3000L
        )
        val entity = model.toEntity()
        assertTrue(entity.examplesJson.contains("I run daily"))
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
```

- [ ] **Step 2: Run test to confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.local.db.mapper.MapperTest"`
Expected: FAIL — extension functions not found

- [ ] **Step 3: Create `TranslationMapper.kt`**
```kotlin
package com.android.zubanx.data.local.db.mapper

import com.android.zubanx.data.local.db.entity.TranslationEntity
import com.android.zubanx.domain.model.Translation

fun TranslationEntity.toDomain(): Translation = Translation(
    id = id, sourceText = sourceText, translatedText = translatedText,
    sourceLang = sourceLang, targetLang = targetLang, expert = expert, timestamp = timestamp
)

fun Translation.toEntity(): TranslationEntity = TranslationEntity(
    id = id, sourceText = sourceText, translatedText = translatedText,
    sourceLang = sourceLang, targetLang = targetLang, expert = expert, timestamp = timestamp
)
```

- [ ] **Step 4: Create `FavouriteMapper.kt`**
```kotlin
package com.android.zubanx.data.local.db.mapper

import com.android.zubanx.data.local.db.entity.FavouriteEntity
import com.android.zubanx.domain.model.Favourite

fun FavouriteEntity.toDomain(): Favourite = Favourite(
    id = id, sourceText = sourceText, translatedText = translatedText,
    sourceLang = sourceLang, targetLang = targetLang, timestamp = timestamp
)

fun Favourite.toEntity(): FavouriteEntity = FavouriteEntity(
    id = id, sourceText = sourceText, translatedText = translatedText,
    sourceLang = sourceLang, targetLang = targetLang, timestamp = timestamp
)
```

- [ ] **Step 5: Create `DictionaryMapper.kt`**
```kotlin
package com.android.zubanx.data.local.db.mapper

import com.android.zubanx.data.local.db.entity.DictionaryEntity
import com.android.zubanx.domain.model.DictionaryEntry
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

fun DictionaryEntity.toDomain(): DictionaryEntry = DictionaryEntry(
    id = id, word = word, language = language, definition = definition,
    phonetic = phonetic, partOfSpeech = partOfSpeech,
    examples = runCatching {
        Json.decodeFromString(ListSerializer(String.serializer()), examplesJson)
    }.getOrDefault(emptyList()),
    timestamp = timestamp
)

fun DictionaryEntry.toEntity(): DictionaryEntity = DictionaryEntity(
    id = id, word = word, language = language, definition = definition,
    phonetic = phonetic, partOfSpeech = partOfSpeech,
    examplesJson = runCatching {
        Json.encodeToString(ListSerializer(String.serializer()), examples)
    }.getOrDefault("[]"),
    timestamp = timestamp
)
```

- [ ] **Step 6: Create `OfflineLanguagePackMapper.kt`**
```kotlin
package com.android.zubanx.data.local.db.mapper

import com.android.zubanx.data.local.db.entity.OfflineLanguagePackEntity
import com.android.zubanx.domain.model.OfflineLanguagePack

fun OfflineLanguagePackEntity.toDomain(): OfflineLanguagePack = OfflineLanguagePack(
    id = id, languageCode = languageCode, languageName = languageName,
    isDownloaded = isDownloaded, downloadedAt = downloadedAt
)

fun OfflineLanguagePack.toEntity(): OfflineLanguagePackEntity = OfflineLanguagePackEntity(
    id = id, languageCode = languageCode, languageName = languageName,
    isDownloaded = isDownloaded, downloadedAt = downloadedAt
)
```

- [ ] **Step 7: Run mapper tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.local.db.mapper.MapperTest"`
Expected: PASS — 6 tests

- [ ] **Step 8: Commit**
```bash
git add app/src/main/java/com/android/zubanx/data/local/db/mapper/
git add app/src/test/java/com/android/zubanx/data/local/db/mapper/
git commit -m "feat(data): add entity↔domain mappers for all four models"
```

---

## Chunk 2: DAOs, ZubanDatabase

**Note:** All DAO tests are instrumented (`androidTest`) because they require Android context to build an in-memory Room database. They will only compile and run after `ZubanDatabase` is created in Task 9. Create all four DAOs and their test files (Tasks 5–8), then create `ZubanDatabase` (Task 9), then run all DAO tests together.

### Task 5: TranslationDao

**Files:**
- Create: `app/src/main/java/com/android/zubanx/data/local/db/dao/TranslationDao.kt`
- Test: `app/src/androidTest/java/com/android/zubanx/data/local/db/dao/TranslationDaoTest.kt`

- [ ] **Step 1: Create `TranslationDao.kt`**
```kotlin
package com.android.zubanx.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.zubanx.data.local.db.entity.TranslationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TranslationEntity): Long

    @Query("SELECT * FROM translations ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<TranslationEntity>>

    @Query("DELETE FROM translations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM translations")
    suspend fun clearAll()
}
```

- [ ] **Step 2: Create `TranslationDaoTest.kt`**

Create `app/src/androidTest/java/com/android/zubanx/data/local/db/dao/TranslationDaoTest.kt`:
```kotlin
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
```

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/android/zubanx/data/local/db/dao/TranslationDao.kt
git add app/src/androidTest/java/com/android/zubanx/data/local/db/dao/TranslationDaoTest.kt
git commit -m "feat(data): add TranslationDao and instrumented test"
```

---

### Task 6: FavouriteDao

**Files:**
- Create: `app/src/main/java/com/android/zubanx/data/local/db/dao/FavouriteDao.kt`
- Test: `app/src/androidTest/java/com/android/zubanx/data/local/db/dao/FavouriteDaoTest.kt`

- [ ] **Step 1: Create `FavouriteDao.kt`**
```kotlin
package com.android.zubanx.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.zubanx.data.local.db.entity.FavouriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavouriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavouriteEntity): Long

    @Query("SELECT * FROM favourites ORDER BY timestamp DESC")
    fun getAll(): Flow<List<FavouriteEntity>>

    @Query("DELETE FROM favourites WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE sourceText = :sourceText)")
    suspend fun existsBySourceText(sourceText: String): Boolean
}
```

- [ ] **Step 2: Create `FavouriteDaoTest.kt`**

Create `app/src/androidTest/java/com/android/zubanx/data/local/db/dao/FavouriteDaoTest.kt`:
```kotlin
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
```

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/android/zubanx/data/local/db/dao/FavouriteDao.kt
git add app/src/androidTest/java/com/android/zubanx/data/local/db/dao/FavouriteDaoTest.kt
git commit -m "feat(data): add FavouriteDao and instrumented test"
```

---

### Task 7: DictionaryDao

**Files:**
- Create: `app/src/main/java/com/android/zubanx/data/local/db/dao/DictionaryDao.kt`
- Test: `app/src/androidTest/java/com/android/zubanx/data/local/db/dao/DictionaryDaoTest.kt`

- [ ] **Step 1: Create `DictionaryDao.kt`**
```kotlin
package com.android.zubanx.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.zubanx.data.local.db.entity.DictionaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DictionaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DictionaryEntity): Long

    @Query("SELECT * FROM dictionary WHERE word = :word AND language = :language LIMIT 1")
    suspend fun getByWordAndLanguage(word: String, language: String): DictionaryEntity?

    @Query("SELECT * FROM dictionary ORDER BY timestamp DESC")
    fun getAll(): Flow<List<DictionaryEntity>>
}
```

- [ ] **Step 2: Create `DictionaryDaoTest.kt`**

Create `app/src/androidTest/java/com/android/zubanx/data/local/db/dao/DictionaryDaoTest.kt`:
```kotlin
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
```

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/android/zubanx/data/local/db/dao/DictionaryDao.kt
git add app/src/androidTest/java/com/android/zubanx/data/local/db/dao/DictionaryDaoTest.kt
git commit -m "feat(data): add DictionaryDao and instrumented test"
```

---

### Task 8: OfflineLanguagePackDao

**Files:**
- Create: `app/src/main/java/com/android/zubanx/data/local/db/dao/OfflineLanguagePackDao.kt`
- Test: `app/src/androidTest/java/com/android/zubanx/data/local/db/dao/OfflineLanguagePackDaoTest.kt`

- [ ] **Step 1: Create `OfflineLanguagePackDao.kt`**
```kotlin
package com.android.zubanx.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.zubanx.data.local.db.entity.OfflineLanguagePackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineLanguagePackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OfflineLanguagePackEntity): Long

    @Query("SELECT * FROM offline_language_packs WHERE languageCode = :code LIMIT 1")
    suspend fun getByLanguageCode(code: String): OfflineLanguagePackEntity?

    @Query("UPDATE offline_language_packs SET isDownloaded = :isDownloaded, downloadedAt = :downloadedAt WHERE languageCode = :code")
    suspend fun updateDownloadStatus(code: String, isDownloaded: Boolean, downloadedAt: Long?)

    @Query("SELECT * FROM offline_language_packs")
    fun getAll(): Flow<List<OfflineLanguagePackEntity>>
}
```

- [ ] **Step 2: Create `OfflineLanguagePackDaoTest.kt`**

Create `app/src/androidTest/java/com/android/zubanx/data/local/db/dao/OfflineLanguagePackDaoTest.kt`:
```kotlin
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
```

- [ ] **Step 3: Commit**
```bash
git add app/src/main/java/com/android/zubanx/data/local/db/dao/OfflineLanguagePackDao.kt
git add app/src/androidTest/java/com/android/zubanx/data/local/db/dao/OfflineLanguagePackDaoTest.kt
git commit -m "feat(data): add OfflineLanguagePackDao and instrumented test"
```

---

### Task 9: ZubanDatabase

**Files:**
- Create: `app/src/main/java/com/android/zubanx/data/local/db/ZubanDatabase.kt`

- [ ] **Step 1: Create `ZubanDatabase.kt`**
```kotlin
package com.android.zubanx.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.android.zubanx.data.local.db.converter.ListStringConverter
import com.android.zubanx.data.local.db.dao.*
import com.android.zubanx.data.local.db.entity.*

@Database(
    entities = [
        TranslationEntity::class,
        FavouriteEntity::class,
        DictionaryEntity::class,
        OfflineLanguagePackEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(ListStringConverter::class)
abstract class ZubanDatabase : RoomDatabase() {
    abstract fun translationDao(): TranslationDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun offlineLanguagePackDao(): OfflineLanguagePackDao
}
```

- [ ] **Step 2: Build to trigger KSP code generation**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL — KSP generates `ZubanDatabase_Impl`

- [ ] **Step 3: Run all DAO instrumented tests**

Run: `./gradlew :app:connectedDebugAndroidTest`
Expected: PASS — 12 DAO tests (3 per DAO × 4 DAOs). Requires connected emulator or device.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/android/zubanx/data/local/db/ZubanDatabase.kt
git commit -m "feat(data): add ZubanDatabase — all 12 DAO instrumented tests pass"
```

---

## Chunk 3: DataStore, Repository Interfaces, Implementations, Koin

### Task 10: AppPreferences (DataStore)

**Files:**
- Create: `app/src/main/java/com/android/zubanx/data/local/datastore/AppPreferences.kt`
- Create: `app/src/main/java/com/android/zubanx/data/local/datastore/AppPreferencesImpl.kt`
- Test: `app/src/test/java/com/android/zubanx/data/local/datastore/AppPreferencesImplTest.kt`

- [ ] **Step 1: Write failing AppPreferences test**

Create `app/src/test/java/com/android/zubanx/data/local/datastore/AppPreferencesImplTest.kt`:
```kotlin
package com.android.zubanx.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class AppPreferencesImplTest {

    private val dataStore: DataStore<Preferences> = mockk()

    @Test
    fun `theme returns stored value`() = runTest {
        val prefs = mutablePreferencesOf(stringPreferencesKey("theme") to "DARK")
        every { dataStore.data } returns flowOf(prefs)
        assertEquals("DARK", AppPreferencesImpl(dataStore).theme.first())
    }

    @Test
    fun `isPremium defaults to false when not set`() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())
        assertFalse(AppPreferencesImpl(dataStore).isPremium.first())
    }

    @Test
    fun `selectedExpert defaults to DEFAULT when not set`() = runTest {
        every { dataStore.data } returns flowOf(emptyPreferences())
        assertEquals("DEFAULT", AppPreferencesImpl(dataStore).selectedExpert.first())
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.local.datastore.AppPreferencesImplTest"`
Expected: FAIL — AppPreferencesImpl not found

- [ ] **Step 3: Create `AppPreferences.kt`**
```kotlin
package com.android.zubanx.data.local.datastore

import kotlinx.coroutines.flow.Flow

interface AppPreferences {
    val theme: Flow<String>
    val selectedExpert: Flow<String>
    val sourceLang: Flow<String>
    val targetLang: Flow<String>
    val isPremium: Flow<Boolean>
    val offlineMode: Flow<Boolean>
    val onboardingComplete: Flow<Boolean>

    suspend fun setTheme(value: String)
    suspend fun setSelectedExpert(value: String)
    suspend fun setSourceLang(value: String)
    suspend fun setTargetLang(value: String)
    suspend fun setIsPremium(value: Boolean)
    suspend fun setOfflineMode(value: Boolean)
    suspend fun setOnboardingComplete(value: Boolean)
}
```

- [ ] **Step 4: Create `AppPreferencesImpl.kt`**
```kotlin
package com.android.zubanx.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppPreferencesImpl(private val dataStore: DataStore<Preferences>) : AppPreferences {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val SELECTED_EXPERT = stringPreferencesKey("selected_expert")
        val SOURCE_LANG = stringPreferencesKey("source_lang")
        val TARGET_LANG = stringPreferencesKey("target_lang")
        val IS_PREMIUM = booleanPreferencesKey("is_premium")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    override val theme: Flow<String> = dataStore.data.map { it[Keys.THEME] ?: "SYSTEM" }
    override val selectedExpert: Flow<String> = dataStore.data.map { it[Keys.SELECTED_EXPERT] ?: "DEFAULT" }
    override val sourceLang: Flow<String> = dataStore.data.map { it[Keys.SOURCE_LANG] ?: "en" }
    override val targetLang: Flow<String> = dataStore.data.map { it[Keys.TARGET_LANG] ?: "es" }
    override val isPremium: Flow<Boolean> = dataStore.data.map { it[Keys.IS_PREMIUM] ?: false }
    override val offlineMode: Flow<Boolean> = dataStore.data.map { it[Keys.OFFLINE_MODE] ?: false }
    override val onboardingComplete: Flow<Boolean> = dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    override suspend fun setTheme(value: String) { dataStore.edit { it[Keys.THEME] = value } }
    override suspend fun setSelectedExpert(value: String) { dataStore.edit { it[Keys.SELECTED_EXPERT] = value } }
    override suspend fun setSourceLang(value: String) { dataStore.edit { it[Keys.SOURCE_LANG] = value } }
    override suspend fun setTargetLang(value: String) { dataStore.edit { it[Keys.TARGET_LANG] = value } }
    override suspend fun setIsPremium(value: Boolean) { dataStore.edit { it[Keys.IS_PREMIUM] = value } }
    override suspend fun setOfflineMode(value: Boolean) { dataStore.edit { it[Keys.OFFLINE_MODE] = value } }
    override suspend fun setOnboardingComplete(value: Boolean) { dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = value } }
}
```

- [ ] **Step 5: Run tests to confirm they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.local.datastore.AppPreferencesImplTest"`
Expected: PASS — 3 tests

- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/android/zubanx/data/local/datastore/
git add app/src/test/java/com/android/zubanx/data/local/datastore/
git commit -m "feat(data): add AppPreferences interface and DataStore implementation"
```

---

### Task 11: Repository Interfaces

**Files:**
- Create: `app/src/main/java/com/android/zubanx/domain/repository/TranslationRepository.kt`
- Create: `app/src/main/java/com/android/zubanx/domain/repository/FavouriteRepository.kt`
- Create: `app/src/main/java/com/android/zubanx/domain/repository/DictionaryRepository.kt`
- Create: `app/src/main/java/com/android/zubanx/domain/repository/OfflineLanguagePackRepository.kt`

No tests — pure contracts, tested via implementations.

- [ ] **Step 1: Create `TranslationRepository.kt`**
```kotlin
package com.android.zubanx.domain.repository

import com.android.zubanx.domain.model.Translation
import kotlinx.coroutines.flow.Flow

interface TranslationRepository {
    fun getHistory(): Flow<List<Translation>>
    suspend fun saveTranslation(translation: Translation)
    suspend fun deleteTranslation(id: Long)
    suspend fun clearHistory()
}
```

- [ ] **Step 2: Create `FavouriteRepository.kt`**
```kotlin
package com.android.zubanx.domain.repository

import com.android.zubanx.domain.model.Favourite
import kotlinx.coroutines.flow.Flow

interface FavouriteRepository {
    fun getAll(): Flow<List<Favourite>>
    suspend fun add(favourite: Favourite)
    suspend fun remove(id: Long)
    suspend fun isFavourite(sourceText: String): Boolean
}
```

- [ ] **Step 3: Create `DictionaryRepository.kt`**
```kotlin
package com.android.zubanx.domain.repository

import com.android.zubanx.domain.model.DictionaryEntry
import kotlinx.coroutines.flow.Flow

interface DictionaryRepository {
    fun getAll(): Flow<List<DictionaryEntry>>
    suspend fun getCached(word: String, language: String): DictionaryEntry?
    suspend fun saveToCache(entry: DictionaryEntry)
}
```

- [ ] **Step 4: Create `OfflineLanguagePackRepository.kt`**
```kotlin
package com.android.zubanx.domain.repository

import com.android.zubanx.domain.model.OfflineLanguagePack
import kotlinx.coroutines.flow.Flow

interface OfflineLanguagePackRepository {
    fun getAll(): Flow<List<OfflineLanguagePack>>
    suspend fun getByLanguageCode(code: String): OfflineLanguagePack?
    suspend fun markDownloaded(code: String, downloadedAt: Long)
    suspend fun markRemoved(code: String)
}
```

- [ ] **Step 5: Verify build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/android/zubanx/domain/repository/
git commit -m "feat(domain): add repository interfaces for Translation, Favourite, Dictionary, OfflinePack"
```

---

### Task 12: Repository Implementations

**Files:**
- Create: `app/src/main/java/com/android/zubanx/data/repository/TranslationRepositoryImpl.kt`
- Create: `app/src/main/java/com/android/zubanx/data/repository/FavouriteRepositoryImpl.kt`
- Create: `app/src/main/java/com/android/zubanx/data/repository/DictionaryRepositoryImpl.kt`
- Create: `app/src/main/java/com/android/zubanx/data/repository/OfflineLanguagePackRepositoryImpl.kt`
- Test: `app/src/test/java/com/android/zubanx/data/repository/TranslationRepositoryImplTest.kt`
- Test: `app/src/test/java/com/android/zubanx/data/repository/FavouriteRepositoryImplTest.kt`

- [ ] **Step 1: Write failing TranslationRepositoryImpl test**

Create `app/src/test/java/com/android/zubanx/data/repository/TranslationRepositoryImplTest.kt`:
```kotlin
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
```

- [ ] **Step 2: Write failing FavouriteRepositoryImpl test**

Create `app/src/test/java/com/android/zubanx/data/repository/FavouriteRepositoryImplTest.kt`:
```kotlin
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
```

- [ ] **Step 3: Run tests to confirm they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.repository.*"`
Expected: FAIL — implementations not found

- [ ] **Step 4: Create `TranslationRepositoryImpl.kt`**
```kotlin
package com.android.zubanx.data.repository

import com.android.zubanx.data.local.db.dao.TranslationDao
import com.android.zubanx.data.local.db.mapper.toDomain
import com.android.zubanx.data.local.db.mapper.toEntity
import com.android.zubanx.domain.model.Translation
import com.android.zubanx.domain.repository.TranslationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TranslationRepositoryImpl(private val dao: TranslationDao) : TranslationRepository {

    override fun getHistory(): Flow<List<Translation>> =
        dao.getHistory().map { it.map { entity -> entity.toDomain() } }

    override suspend fun saveTranslation(translation: Translation) {
        dao.insert(translation.toEntity())
    }

    override suspend fun deleteTranslation(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun clearHistory() {
        dao.clearAll()
    }
}
```

- [ ] **Step 5: Create `FavouriteRepositoryImpl.kt`**
```kotlin
package com.android.zubanx.data.repository

import com.android.zubanx.data.local.db.dao.FavouriteDao
import com.android.zubanx.data.local.db.mapper.toDomain
import com.android.zubanx.data.local.db.mapper.toEntity
import com.android.zubanx.domain.model.Favourite
import com.android.zubanx.domain.repository.FavouriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavouriteRepositoryImpl(private val dao: FavouriteDao) : FavouriteRepository {

    override fun getAll(): Flow<List<Favourite>> =
        dao.getAll().map { it.map { entity -> entity.toDomain() } }

    override suspend fun add(favourite: Favourite) {
        dao.insert(favourite.toEntity())
    }

    override suspend fun remove(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun isFavourite(sourceText: String): Boolean =
        dao.existsBySourceText(sourceText)
}
```

- [ ] **Step 6: Create `DictionaryRepositoryImpl.kt`**
```kotlin
package com.android.zubanx.data.repository

import com.android.zubanx.data.local.db.dao.DictionaryDao
import com.android.zubanx.data.local.db.mapper.toDomain
import com.android.zubanx.data.local.db.mapper.toEntity
import com.android.zubanx.domain.model.DictionaryEntry
import com.android.zubanx.domain.repository.DictionaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DictionaryRepositoryImpl(private val dao: DictionaryDao) : DictionaryRepository {

    override fun getAll(): Flow<List<DictionaryEntry>> =
        dao.getAll().map { it.map { entity -> entity.toDomain() } }

    override suspend fun getCached(word: String, language: String): DictionaryEntry? =
        dao.getByWordAndLanguage(word, language)?.toDomain()

    override suspend fun saveToCache(entry: DictionaryEntry) {
        dao.insert(entry.toEntity())
    }
}
```

- [ ] **Step 7: Create `OfflineLanguagePackRepositoryImpl.kt`**
```kotlin
package com.android.zubanx.data.repository

import com.android.zubanx.data.local.db.dao.OfflineLanguagePackDao
import com.android.zubanx.data.local.db.mapper.toDomain
import com.android.zubanx.data.local.db.mapper.toEntity
import com.android.zubanx.domain.model.OfflineLanguagePack
import com.android.zubanx.domain.repository.OfflineLanguagePackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineLanguagePackRepositoryImpl(
    private val dao: OfflineLanguagePackDao
) : OfflineLanguagePackRepository {

    override fun getAll(): Flow<List<OfflineLanguagePack>> =
        dao.getAll().map { it.map { entity -> entity.toDomain() } }

    override suspend fun getByLanguageCode(code: String): OfflineLanguagePack? =
        dao.getByLanguageCode(code)?.toDomain()

    override suspend fun markDownloaded(code: String, downloadedAt: Long) {
        dao.updateDownloadStatus(code, isDownloaded = true, downloadedAt = downloadedAt)
    }

    override suspend fun markRemoved(code: String) {
        dao.updateDownloadStatus(code, isDownloaded = false, downloadedAt = null)
    }
}
```

- [ ] **Step 8: Run all repository unit tests**

Run: `./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.repository.*"`
Expected: PASS — 6 tests (3 TranslationRepo + 3 FavouriteRepo)

- [ ] **Step 9: Commit**
```bash
git add app/src/main/java/com/android/zubanx/data/repository/
git add app/src/test/java/com/android/zubanx/data/repository/
git commit -m "feat(data): add repository implementations for all four domain entities"
```

---

### Task 13: Wire Koin DI Modules

**Files:**
- Modify: `app/src/main/java/com/android/zubanx/core/di/DatabaseModule.kt`
- Modify: `app/src/main/java/com/android/zubanx/core/di/DataStoreModule.kt`
- Modify: `app/src/main/java/com/android/zubanx/core/di/RepositoryModule.kt`

- [ ] **Step 1: Replace stub `DatabaseModule.kt`**
```kotlin
package com.android.zubanx.core.di

import androidx.room.Room
import com.android.zubanx.data.local.db.ZubanDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single<ZubanDatabase> {
        Room.databaseBuilder(
            androidContext(),
            ZubanDatabase::class.java,
            "zuban_database"
        ).build()
    }
    single { get<ZubanDatabase>().translationDao() }
    single { get<ZubanDatabase>().favouriteDao() }
    single { get<ZubanDatabase>().dictionaryDao() }
    single { get<ZubanDatabase>().offlineLanguagePackDao() }
}
```

- [ ] **Step 2: Replace stub `DataStoreModule.kt`**
```kotlin
package com.android.zubanx.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.android.zubanx.data.local.datastore.AppPreferences
import com.android.zubanx.data.local.datastore.AppPreferencesImpl
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zuban_prefs")

val dataStoreModule = module {
    single<DataStore<Preferences>> { androidContext().dataStore }
    single<AppPreferences> { AppPreferencesImpl(get()) }
}
```

- [ ] **Step 3: Replace stub `RepositoryModule.kt`**
```kotlin
package com.android.zubanx.core.di

import com.android.zubanx.data.repository.*
import com.android.zubanx.domain.repository.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::TranslationRepositoryImpl) bind TranslationRepository::class
    singleOf(::FavouriteRepositoryImpl) bind FavouriteRepository::class
    singleOf(::DictionaryRepositoryImpl) bind DictionaryRepository::class
    singleOf(::OfflineLanguagePackRepositoryImpl) bind OfflineLanguagePackRepository::class
}
```

- [ ] **Step 4: Build to verify everything compiles**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Run all JVM unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: All tests PASS (domain models + entities + mappers + AppPreferences + repositories)

- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/android/zubanx/core/di/DatabaseModule.kt
git add app/src/main/java/com/android/zubanx/core/di/DataStoreModule.kt
git add app/src/main/java/com/android/zubanx/core/di/RepositoryModule.kt
git commit -m "feat(di): wire databaseModule, dataStoreModule, repositoryModule with Koin"
```

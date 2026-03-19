# Network Layer Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete network infrastructure layer — `NetworkResult`/`safeApiCall`, `KtorClientFactory`, all remote DTOs, service interfaces and stub implementations, `KeyDecryptionModule`, `MlKitOcrService`, `MlKitTranslateService`, and wire all three Koin modules (`networkModule`, `securityModule`, `mlKitModule`).

**Architecture:** `NetworkResult<T>` is a sealed interface (`Success`/`Error`) — `Loading` is a UI concern handled in MVI State, not here. `safeApiCall` is a top-level suspend function that wraps any Ktor call and catches exceptions. `KtorClientFactory` produces a configured `HttpClient` (OkHttp engine, JSON content negotiation, Timber logging). Three service interfaces (`TranslateApiService`, `DictionaryApiService`, `AiExpertService`) live in `data/remote/api/` with stub implementations that throw `NotImplementedError` — real logic is added in Plans 5–8. `KeyDecryptionModule` fetches from Firebase Remote Config and decrypts in memory. `MlKitOcrService` and `MlKitTranslateService` are on-device wrappers registered in `mlKitModule`.

**Tech Stack:** Ktor 3.1.2 (OkHttp engine, content negotiation, kotlinx-json serialization, logging), kotlinx-serialization-json 1.8.0, Firebase Remote Config (firebase-config-ktx via BOM 33.12.0), ML Kit Text Recognition 16.0.1, ML Kit Translate 17.0.3, Koin BOM 4.1.1, MockK 1.14.0, kotlinx-coroutines-test 1.10.1.

---

## File Structure

**`core/network/`** — shared network primitives, no Android/Ktor imports in `NetworkResult`:
- `NetworkResult.kt` — sealed interface `NetworkResult<out T>` with `Success` and `Error`
- `SafeApiCall.kt` — top-level `suspend fun <T> safeApiCall(block: suspend () -> T): NetworkResult<T>`
- `KtorClientFactory.kt` — `object` that returns a configured `HttpClient`

**`data/remote/dto/`** — `@Serializable` data classes, no logic:
- `TranslateResponseDto.kt` — wraps Google Translate scrape result
- `DictionaryResponseDto.kt` — Free Dictionary API response (word, phonetics, meanings, definitions)
- `AiExpertResponseDto.kt` — unified AI expert response wrapper

**`data/remote/api/`** — interface + stub implementation per service:
- `TranslateApiService.kt` — interface
- `TranslateApiServiceImpl.kt` — stub implementation
- `DictionaryApiService.kt` — interface
- `DictionaryApiServiceImpl.kt` — stub implementation
- `AiExpertService.kt` — interface
- `AiExpertServiceImpl.kt` — stub, delegates based on selected expert

**`security/`**:
- `KeyDecryptionModule.kt` — fetches + decrypts keys from Firebase Remote Config

**`data/mlkit/`**:
- `MlKitOcrService.kt` — ML Kit Text Recognition wrapper
- `MlKitTranslateService.kt` — ML Kit Translate wrapper (pack download + translation)

**`core/di/`** — fill existing stubs:
- `NetworkModule.kt` — registers `KtorClientFactory` result + all three API services
- `SecurityModule.kt` — registers `KeyDecryptionModule`
- `MlKitModule.kt` — registers `MlKitOcrService`, `MlKitTranslateService`

**Tests (JVM unit tests in `app/src/test/`):**
- `core/network/NetworkResultTest.kt`
- `core/network/SafeApiCallTest.kt`
- `core/network/KtorClientFactoryTest.kt`
- `data/remote/dto/DtoSerializationTest.kt`
- `data/remote/api/TranslateApiServiceImplTest.kt`
- `data/remote/api/DictionaryApiServiceImplTest.kt`
- `data/remote/api/AiExpertServiceImplTest.kt`
- `security/KeyDecryptionModuleTest.kt`
- `data/mlkit/MlKitOcrServiceTest.kt`
- `data/mlkit/MlKitTranslateServiceTest.kt`

---

## Chunk 1: NetworkResult, safeApiCall, KtorClientFactory

### Task 1: `NetworkResult` sealed interface

**Files:**
- Create: `app/src/main/java/com/android/zubanx/core/network/NetworkResult.kt`
- Test: `app/src/test/java/com/android/zubanx/core/network/NetworkResultTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/android/zubanx/core/network/NetworkResultTest.kt`:
```kotlin
package com.android.zubanx.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkResultTest {

    @Test
    fun `Success holds data`() {
        val result: NetworkResult<String> = NetworkResult.Success("hello")
        assertTrue(result is NetworkResult.Success)
        assertEquals("hello", (result as NetworkResult.Success).data)
    }

    @Test
    fun `Error holds message and null code by default`() {
        val result: NetworkResult<Nothing> = NetworkResult.Error("oops")
        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertEquals("oops", error.message)
        assertEquals(null, error.code)
    }

    @Test
    fun `Error holds message and explicit code`() {
        val result: NetworkResult<Nothing> = NetworkResult.Error("not found", 404)
        assertEquals(404, (result as NetworkResult.Error).code)
    }

    @Test
    fun `NetworkResult is covariant — Success String assignable to NetworkResult Any`() {
        val result: NetworkResult<String> = NetworkResult.Success("test")
        val covariant: NetworkResult<Any> = result // compiles only if out T
        assertTrue(covariant is NetworkResult.Success)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.core.network.NetworkResultTest" --info
```
Expected: FAILED — `NetworkResult` does not exist yet.

- [ ] **Step 3: Create `NetworkResult.kt`**

Create `app/src/main/java/com/android/zubanx/core/network/NetworkResult.kt`:
```kotlin
package com.android.zubanx.core.network

sealed interface NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>
    data class Error(val message: String, val code: Int? = null) : NetworkResult<Nothing>
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.core.network.NetworkResultTest"
```
Expected: BUILD SUCCESSFUL, 4 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/android/zubanx/core/network/NetworkResult.kt \
        app/src/test/java/com/android/zubanx/core/network/NetworkResultTest.kt
git commit -m "feat: add NetworkResult sealed interface"
```

---

### Task 2: `safeApiCall` top-level suspend function

**Files:**
- Create: `app/src/main/java/com/android/zubanx/core/network/SafeApiCall.kt`
- Test: `app/src/test/java/com/android/zubanx/core/network/SafeApiCallTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/android/zubanx/core/network/SafeApiCallTest.kt`:
```kotlin
package com.android.zubanx.core.network

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SafeApiCallTest {

    @Test
    fun `returns Success when block succeeds`() = runTest {
        val result = safeApiCall { "translation" }
        assertTrue(result is NetworkResult.Success)
        assertEquals("translation", (result as NetworkResult.Success).data)
    }

    @Test
    fun `returns Error with message when IOException is thrown`() = runTest {
        val result = safeApiCall<String> { throw IOException("network error") }
        assertTrue(result is NetworkResult.Error)
        val error = result as NetworkResult.Error
        assertTrue(error.message.contains("network error"))
        assertEquals(null, error.code)
    }

    @Test
    fun `returns Error with message when generic Exception is thrown`() = runTest {
        val result = safeApiCall<String> { throw RuntimeException("unexpected") }
        assertTrue(result is NetworkResult.Error)
        assertTrue((result as NetworkResult.Error).message.contains("unexpected"))
    }

    @Test
    fun `returns Error with code when ClientRequestException is thrown`() = runTest {
        // ClientRequestException carries an HttpStatusCode — stub via a generic exception
        // that matches the pattern we handle; actual Ktor exception needs a full response mock.
        // We use a subclass-agnostic fallback path here.
        val result = safeApiCall<String> { throw Exception("HTTP 400: Bad Request") }
        assertTrue(result is NetworkResult.Error)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.core.network.SafeApiCallTest"
```
Expected: FAILED — `safeApiCall` does not exist yet.

- [ ] **Step 3: Create `SafeApiCall.kt`**

Create `app/src/main/java/com/android/zubanx/core/network/SafeApiCall.kt`:
```kotlin
package com.android.zubanx.core.network

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import timber.log.Timber
import java.io.IOException

/**
 * Wraps a Ktor (or any suspend) network call and converts it into a [NetworkResult].
 *
 * - [NetworkResult.Success] — block completed without exception
 * - [NetworkResult.Error]   — any exception was caught; HTTP status code extracted when available
 *
 * Loading state is a UI/MVI concern and is NOT modelled here.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(block())
    } catch (e: ClientRequestException) {
        val code = e.response.status.value
        val message = e.message ?: "Client error $code"
        Timber.w(e, "safeApiCall ClientRequestException: $code")
        NetworkResult.Error(message, code)
    } catch (e: ServerResponseException) {
        val code = e.response.status.value
        val message = e.message ?: "Server error $code"
        Timber.w(e, "safeApiCall ServerResponseException: $code")
        NetworkResult.Error(message, code)
    } catch (e: IOException) {
        Timber.w(e, "safeApiCall IOException")
        NetworkResult.Error(e.message ?: "IO error", null)
    } catch (e: Exception) {
        Timber.e(e, "safeApiCall unexpected exception")
        NetworkResult.Error(e.message ?: "Unknown error", null)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.core.network.SafeApiCallTest"
```
Expected: BUILD SUCCESSFUL, 4 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/android/zubanx/core/network/SafeApiCall.kt \
        app/src/test/java/com/android/zubanx/core/network/SafeApiCallTest.kt
git commit -m "feat: add safeApiCall suspend function with NetworkResult mapping"
```

---

### Task 3: `KtorClientFactory`

**Files:**
- Create: `app/src/main/java/com/android/zubanx/core/network/KtorClientFactory.kt`
- Test: `app/src/test/java/com/android/zubanx/core/network/KtorClientFactoryTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/android/zubanx/core/network/KtorClientFactoryTest.kt`:
```kotlin
package com.android.zubanx.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import org.junit.Assert.assertNotNull
import org.junit.Test

class KtorClientFactoryTest {

    @Test
    fun `create returns non-null HttpClient`() {
        val client: HttpClient = KtorClientFactory.create()
        assertNotNull(client)
        client.close()
    }

    @Test
    fun `create installs ContentNegotiation plugin`() {
        val client: HttpClient = KtorClientFactory.create()
        // If ContentNegotiation is not installed this throws IllegalStateException
        val plugin = client.pluginOrNull(ContentNegotiation)
        assertNotNull(plugin)
        client.close()
    }

    @Test
    fun `create installs Logging plugin`() {
        val client: HttpClient = KtorClientFactory.create()
        val plugin = client.pluginOrNull(Logging)
        assertNotNull(plugin)
        client.close()
    }

    @Test
    fun `create produces distinct clients on each call`() {
        val a = KtorClientFactory.create()
        val b = KtorClientFactory.create()
        // Each call creates a new instance; DI layer is responsible for singleton scope
        assert(a !== b)
        a.close()
        b.close()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.core.network.KtorClientFactoryTest"
```
Expected: FAILED — `KtorClientFactory` does not exist yet.

- [ ] **Step 3: Create `KtorClientFactory.kt`**

Create `app/src/main/java/com/android/zubanx/core/network/KtorClientFactory.kt`:
```kotlin
package com.android.zubanx.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Factory that produces a pre-configured [HttpClient].
 *
 * Use [KtorClientFactory.create] inside Koin's `networkModule` as a `single { }` block
 * so the same client instance is shared across all services.
 */
object KtorClientFactory {

    fun create(): HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
        install(Logging) {
            level = LogLevel.BODY
            logger = object : Logger {
                override fun log(message: String) {
                    Timber.tag("Ktor").d(message)
                }
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.core.network.KtorClientFactoryTest"
```
Expected: BUILD SUCCESSFUL, 4 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/android/zubanx/core/network/KtorClientFactory.kt \
        app/src/test/java/com/android/zubanx/core/network/KtorClientFactoryTest.kt
git commit -m "feat: add KtorClientFactory (OkHttp engine, JSON, Timber logging)"
```

---

## Chunk 2: DTOs

### Task 4: `TranslateResponseDto`

**Files:**
- Create: `app/src/main/java/com/android/zubanx/data/remote/dto/TranslateResponseDto.kt`
- Test: `app/src/test/java/com/android/zubanx/data/remote/dto/DtoSerializationTest.kt` (extended in later tasks)

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/android/zubanx/data/remote/dto/DtoSerializationTest.kt`:
```kotlin
package com.android.zubanx.data.remote.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DtoSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ---- TranslateResponseDto ----

    @Test
    fun `TranslateResponseDto round-trips through JSON`() {
        val dto = TranslateResponseDto(
            translatedText = "Hola",
            sourceLang = "en",
            targetLang = "es",
            detectedSourceLang = null
        )
        val encoded = json.encodeToString(dto)
        val decoded = json.decodeFromString<TranslateResponseDto>(encoded)
        assertEquals("Hola", decoded.translatedText)
        assertEquals("es", decoded.targetLang)
        assertNull(decoded.detectedSourceLang)
    }

    @Test
    fun `TranslateResponseDto ignores unknown JSON keys`() {
        val raw = """{"translatedText":"Hi","sourceLang":"es","targetLang":"en","unknownField":"x"}"""
        val dto = json.decodeFromString<TranslateResponseDto>(raw)
        assertEquals("Hi", dto.translatedText)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.remote.dto.DtoSerializationTest.TranslateResponseDto round-trips through JSON"
```
Expected: FAILED — `TranslateResponseDto` does not exist yet.

- [ ] **Step 3: Create `TranslateResponseDto.kt`**

Create `app/src/main/java/com/android/zubanx/data/remote/dto/TranslateResponseDto.kt`:
```kotlin
package com.android.zubanx.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the Google Translate scraping response.
 *
 * [detectedSourceLang] is non-null only when [sourceLang] was "auto" and the
 * scraper resolved the actual language. Actual parsing logic is added in Plan 5.
 */
@Serializable
data class TranslateResponseDto(
    @SerialName("translatedText") val translatedText: String,
    @SerialName("sourceLang") val sourceLang: String,
    @SerialName("targetLang") val targetLang: String,
    @SerialName("detectedSourceLang") val detectedSourceLang: String? = null
)
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.remote.dto.DtoSerializationTest"
```
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/android/zubanx/data/remote/dto/TranslateResponseDto.kt \
        app/src/test/java/com/android/zubanx/data/remote/dto/DtoSerializationTest.kt
git commit -m "feat: add TranslateResponseDto with serialization tests"
```

---

### Task 5: `DictionaryResponseDto`

**Files:**
- Create: `app/src/main/java/com/android/zubanx/data/remote/dto/DictionaryResponseDto.kt`
- Modify (extend): `app/src/test/java/com/android/zubanx/data/remote/dto/DtoSerializationTest.kt`

The Free Dictionary API (`https://api.dictionaryapi.dev/api/v2/entries/en/<word>`) returns an array. Each element has: `word`, `phonetic`, `phonetics` (list), `meanings` (list of `partOfSpeech` + `definitions`). Each definition has `definition`, `example`, `synonyms`, `antonyms`.

- [ ] **Step 1: Add failing tests for `DictionaryResponseDto`**

Add these test methods to `DtoSerializationTest.kt` (the existing class — do not replace the file):
```kotlin
    // ---- DictionaryResponseDto ----

    @Test
    fun `DictionaryResponseDto round-trips through JSON`() {
        val dto = DictionaryResponseDto(
            word = "run",
            phonetic = "/rʌn/",
            phonetics = listOf(PhoneticDto(text = "/rʌn/", audio = null)),
            meanings = listOf(
                MeaningDto(
                    partOfSpeech = "verb",
                    definitions = listOf(
                        DefinitionDto(
                            definition = "to move fast",
                            example = "She runs every morning.",
                            synonyms = listOf("sprint"),
                            antonyms = emptyList()
                        )
                    )
                )
            )
        )
        val encoded = json.encodeToString(dto)
        val decoded = json.decodeFromString<DictionaryResponseDto>(encoded)
        assertEquals("run", decoded.word)
        assertEquals(1, decoded.meanings.size)
        assertEquals("verb", decoded.meanings[0].partOfSpeech)
        assertEquals("to move fast", decoded.meanings[0].definitions[0].definition)
    }

    @Test
    fun `DictionaryResponseDto example and audio are nullable`() {
        val raw = """
            {
              "word": "cat",
              "phonetics": [],
              "meanings": [
                {
                  "partOfSpeech": "noun",
                  "definitions": [{ "definition": "a small animal", "synonyms": [], "antonyms": [] }]
                }
              ]
            }
        """.trimIndent()
        val dto = json.decodeFromString<DictionaryResponseDto>(raw)
        assertEquals("cat", dto.word)
        assertNull(dto.phonetic)
        assertNull(dto.meanings[0].definitions[0].example)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.remote.dto.DtoSerializationTest.DictionaryResponseDto*"
```
Expected: FAILED — `DictionaryResponseDto` does not exist yet.

- [ ] **Step 3: Create `DictionaryResponseDto.kt`**

Create `app/src/main/java/com/android/zubanx/data/remote/dto/DictionaryResponseDto.kt`:
```kotlin
package com.android.zubanx.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Top-level DTO for a single entry from the Free Dictionary API.
 *
 * The API returns `List<DictionaryResponseDto>` — the list wrapping is handled
 * at the call-site in [DictionaryApiServiceImpl]. A word may have multiple
 * entries (different etymologies); callers use the first entry.
 */
@Serializable
data class DictionaryResponseDto(
    @SerialName("word") val word: String,
    @SerialName("phonetic") val phonetic: String? = null,
    @SerialName("phonetics") val phonetics: List<PhoneticDto> = emptyList(),
    @SerialName("meanings") val meanings: List<MeaningDto> = emptyList()
)

@Serializable
data class PhoneticDto(
    @SerialName("text") val text: String? = null,
    @SerialName("audio") val audio: String? = null
)

@Serializable
data class MeaningDto(
    @SerialName("partOfSpeech") val partOfSpeech: String,
    @SerialName("definitions") val definitions: List<DefinitionDto> = emptyList()
)

@Serializable
data class DefinitionDto(
    @SerialName("definition") val definition: String,
    @SerialName("example") val example: String? = null,
    @SerialName("synonyms") val synonyms: List<String> = emptyList(),
    @SerialName("antonyms") val antonyms: List<String> = emptyList()
)
```

- [ ] **Step 4: Run all DTO tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.remote.dto.DtoSerializationTest"
```
Expected: BUILD SUCCESSFUL, all tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/android/zubanx/data/remote/dto/DictionaryResponseDto.kt \
        app/src/test/java/com/android/zubanx/data/remote/dto/DtoSerializationTest.kt
git commit -m "feat: add DictionaryResponseDto with nested PhoneticDto, MeaningDto, DefinitionDto"
```

---

### Task 6: `AiExpertResponseDto`

**Files:**
- Create: `app/src/main/java/com/android/zubanx/data/remote/dto/AiExpertResponseDto.kt`
- Modify (extend): `app/src/test/java/com/android/zubanx/data/remote/dto/DtoSerializationTest.kt`

- [ ] **Step 1: Add failing tests for `AiExpertResponseDto`**

Add these test methods to `DtoSerializationTest.kt`:
```kotlin
    // ---- AiExpertResponseDto ----

    @Test
    fun `AiExpertResponseDto round-trips through JSON`() {
        val dto = AiExpertResponseDto(
            expert = "GPT",
            content = "The word 'run' means to move swiftly.",
            tokensUsed = 42,
            errorMessage = null
        )
        val encoded = json.encodeToString(dto)
        val decoded = json.decodeFromString<AiExpertResponseDto>(encoded)
        assertEquals("GPT", decoded.expert)
        assertEquals(42, decoded.tokensUsed)
        assertNull(decoded.errorMessage)
    }

    @Test
    fun `AiExpertResponseDto tokensUsed is nullable`() {
        val raw = """{"expert":"GEMINI","content":"hello"}"""
        val dto = json.decodeFromString<AiExpertResponseDto>(raw)
        assertEquals("GEMINI", dto.expert)
        assertNull(dto.tokensUsed)
    }

    @Test
    fun `AiExpertResponseDto models error response`() {
        val dto = AiExpertResponseDto(
            expert = "CLAUDE",
            content = "",
            tokensUsed = null,
            errorMessage = "rate limit exceeded"
        )
        assertEquals("rate limit exceeded", dto.errorMessage)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.remote.dto.DtoSerializationTest.AiExpertResponseDto*"
```
Expected: FAILED — `AiExpertResponseDto` does not exist yet.

- [ ] **Step 3: Create `AiExpertResponseDto.kt`**

Create `app/src/main/java/com/android/zubanx/data/remote/dto/AiExpertResponseDto.kt`:
```kotlin
package com.android.zubanx.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Common response wrapper for all AI expert backends (GPT, Gemini, Claude, Default).
 *
 * [expert] echoes back which backend produced this response.
 * [content] is the AI-generated text (translation, explanation, etc.).
 * [tokensUsed] is null for backends that do not expose token counts.
 * [errorMessage] is non-null when the backend returned an error body rather
 * than throwing an HTTP exception (e.g. quota exceeded with HTTP 200).
 */
@Serializable
data class AiExpertResponseDto(
    @SerialName("expert") val expert: String,
    @SerialName("content") val content: String,
    @SerialName("tokensUsed") val tokensUsed: Int? = null,
    @SerialName("errorMessage") val errorMessage: String? = null
)
```

- [ ] **Step 4: Run all DTO tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.remote.dto.DtoSerializationTest"
```
Expected: BUILD SUCCESSFUL, all tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/android/zubanx/data/remote/dto/AiExpertResponseDto.kt \
        app/src/test/java/com/android/zubanx/data/remote/dto/DtoSerializationTest.kt
git commit -m "feat: add AiExpertResponseDto for unified GPT/Gemini/Claude/Default responses"
```

---

## Chunk 3: Remote API Services

### Task 7: `TranslateApiService` interface and stub implementation

**Files:**
- Create: `app/src/main/java/com/android/zubanx/data/remote/api/TranslateApiService.kt`
- Create: `app/src/main/java/com/android/zubanx/data/remote/api/TranslateApiServiceImpl.kt`
- Test: `app/src/test/java/com/android/zubanx/data/remote/api/TranslateApiServiceImplTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/android/zubanx/data/remote/api/TranslateApiServiceImplTest.kt`:
```kotlin
package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.TranslateResponseDto
import io.ktor.client.HttpClient
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslateApiServiceImplTest {

    private val client: HttpClient = mockk(relaxed = true)
    private val service: TranslateApiService = TranslateApiServiceImpl(client)

    @Test
    fun `translate returns Error — stub not yet implemented`() = runTest {
        val result: NetworkResult<TranslateResponseDto> = service.translate(
            text = "Hello",
            sourceLang = "en",
            targetLang = "es"
        )
        // Stub implementations return NetworkResult.Error with a descriptive message
        // Real implementation added in Plan 5
        assertTrue(result is NetworkResult.Error)
        val msg = (result as NetworkResult.Error).message
        assertTrue(msg.contains("not implemented", ignoreCase = true))
    }

    @Test
    fun `TranslateApiServiceImpl satisfies TranslateApiService interface`() {
        // Verifies the class compiles and satisfies the interface contract
        val impl: TranslateApiService = TranslateApiServiceImpl(client)
        assertTrue(impl is TranslateApiServiceImpl)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.remote.api.TranslateApiServiceImplTest"
```
Expected: FAILED — `TranslateApiService` and `TranslateApiServiceImpl` do not exist yet.

- [ ] **Step 3: Create `TranslateApiService.kt`**

Create `app/src/main/java/com/android/zubanx/data/remote/api/TranslateApiService.kt`:
```kotlin
package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.TranslateResponseDto

/**
 * Network service for text translation.
 *
 * The default backend is Google Translate scraping. When the user selects an AI expert
 * (GPT/Gemini/Claude), [AiExpertService] is used instead. This service handles only
 * the scraping path.
 *
 * Implementation is a stub — real scraping logic is added in Plan 5.
 */
interface TranslateApiService {
    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): NetworkResult<TranslateResponseDto>
}
```

- [ ] **Step 4: Create `TranslateApiServiceImpl.kt`**

Create `app/src/main/java/com/android/zubanx/data/remote/api/TranslateApiServiceImpl.kt`:
```kotlin
package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.TranslateResponseDto
import io.ktor.client.HttpClient

/**
 * Stub implementation of [TranslateApiService].
 *
 * Returns [NetworkResult.Error] with a "not implemented" message until Plan 5
 * adds the actual Google Translate scraping logic.
 */
class TranslateApiServiceImpl(
    private val client: HttpClient
) : TranslateApiService {

    override suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): NetworkResult<TranslateResponseDto> {
        return NetworkResult.Error("TranslateApiService: not implemented yet — see Plan 5")
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.remote.api.TranslateApiServiceImplTest"
```
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/android/zubanx/data/remote/api/TranslateApiService.kt \
        app/src/main/java/com/android/zubanx/data/remote/api/TranslateApiServiceImpl.kt \
        app/src/test/java/com/android/zubanx/data/remote/api/TranslateApiServiceImplTest.kt
git commit -m "feat: add TranslateApiService interface and stub implementation"
```

---

### Task 8: `DictionaryApiService` interface and stub implementation

**Files:**
- Create: `app/src/main/java/com/android/zubanx/data/remote/api/DictionaryApiService.kt`
- Create: `app/src/main/java/com/android/zubanx/data/remote/api/DictionaryApiServiceImpl.kt`
- Test: `app/src/test/java/com/android/zubanx/data/remote/api/DictionaryApiServiceImplTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/android/zubanx/data/remote/api/DictionaryApiServiceImplTest.kt`:
```kotlin
package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.DictionaryResponseDto
import io.ktor.client.HttpClient
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryApiServiceImplTest {

    private val client: HttpClient = mockk(relaxed = true)
    private val service: DictionaryApiService = DictionaryApiServiceImpl(client)

    @Test
    fun `lookup returns Error — stub not yet implemented`() = runTest {
        val result: NetworkResult<DictionaryResponseDto> = service.lookup(
            word = "run",
            language = "en"
        )
        assertTrue(result is NetworkResult.Error)
        val msg = (result as NetworkResult.Error).message
        assertTrue(msg.contains("not implemented", ignoreCase = true))
    }

    @Test
    fun `DictionaryApiServiceImpl satisfies DictionaryApiService interface`() {
        val impl: DictionaryApiService = DictionaryApiServiceImpl(client)
        assertTrue(impl is DictionaryApiServiceImpl)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.remote.api.DictionaryApiServiceImplTest"
```
Expected: FAILED — `DictionaryApiService` does not exist yet.

- [ ] **Step 3: Create `DictionaryApiService.kt`**

Create `app/src/main/java/com/android/zubanx/data/remote/api/DictionaryApiService.kt`:
```kotlin
package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.DictionaryResponseDto

/**
 * Network service for the Free Dictionary API.
 *
 * Endpoint: `GET https://api.dictionaryapi.dev/api/v2/entries/{language}/{word}`
 *
 * Returns the first entry from the API's list response. Full implementation
 * is added in Plan 6 (Dictionary feature).
 */
interface DictionaryApiService {
    suspend fun lookup(word: String, language: String): NetworkResult<DictionaryResponseDto>
}
```

- [ ] **Step 4: Create `DictionaryApiServiceImpl.kt`**

Create `app/src/main/java/com/android/zubanx/data/remote/api/DictionaryApiServiceImpl.kt`:
```kotlin
package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.DictionaryResponseDto
import io.ktor.client.HttpClient

/**
 * Stub implementation of [DictionaryApiService].
 *
 * Returns [NetworkResult.Error] with a "not implemented" message until Plan 6
 * adds the actual Free Dictionary API call.
 */
class DictionaryApiServiceImpl(
    private val client: HttpClient
) : DictionaryApiService {

    override suspend fun lookup(
        word: String,
        language: String
    ): NetworkResult<DictionaryResponseDto> {
        return NetworkResult.Error("DictionaryApiService: not implemented yet — see Plan 6")
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.remote.api.DictionaryApiServiceImplTest"
```
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/android/zubanx/data/remote/api/DictionaryApiService.kt \
        app/src/main/java/com/android/zubanx/data/remote/api/DictionaryApiServiceImpl.kt \
        app/src/test/java/com/android/zubanx/data/remote/api/DictionaryApiServiceImplTest.kt
git commit -m "feat: add DictionaryApiService interface and stub implementation"
```

---

### Task 9: `AiExpertService` interface and stub implementation

**Files:**
- Create: `app/src/main/java/com/android/zubanx/data/remote/api/AiExpertService.kt`
- Create: `app/src/main/java/com/android/zubanx/data/remote/api/AiExpertServiceImpl.kt`
- Test: `app/src/test/java/com/android/zubanx/data/remote/api/AiExpertServiceImplTest.kt`

The `AiExpertService` is the unified entry point for GPT, Gemini, Claude, and the Default (rule-based fallback) expert. The selected expert is determined at call time via a `String` parameter matching the `selectedExpert` preference value (e.g. `"GPT"`, `"GEMINI"`, `"CLAUDE"`, `"DEFAULT"`). The stub always returns `NetworkResult.Error`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/android/zubanx/data/remote/api/AiExpertServiceImplTest.kt`:
```kotlin
package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.AiExpertResponseDto
import io.ktor.client.HttpClient
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AiExpertServiceImplTest {

    private val client: HttpClient = mockk(relaxed = true)
    private val service: AiExpertService = AiExpertServiceImpl(client)

    @Test
    fun `ask with GPT expert returns Error — stub not yet implemented`() = runTest {
        val result: NetworkResult<AiExpertResponseDto> = service.ask(
            expert = "GPT",
            prompt = "Translate 'Hello' to Spanish"
        )
        assertTrue(result is NetworkResult.Error)
        assertTrue((result as NetworkResult.Error).message.contains("not implemented", ignoreCase = true))
    }

    @Test
    fun `ask with GEMINI expert returns Error — stub not yet implemented`() = runTest {
        val result = service.ask(expert = "GEMINI", prompt = "Explain 'run'")
        assertTrue(result is NetworkResult.Error)
    }

    @Test
    fun `ask with CLAUDE expert returns Error — stub not yet implemented`() = runTest {
        val result = service.ask(expert = "CLAUDE", prompt = "Translate 'cat'")
        assertTrue(result is NetworkResult.Error)
    }

    @Test
    fun `ask with DEFAULT expert returns Error — stub not yet implemented`() = runTest {
        val result = service.ask(expert = "DEFAULT", prompt = "Translate 'dog'")
        assertTrue(result is NetworkResult.Error)
    }

    @Test
    fun `AiExpertServiceImpl satisfies AiExpertService interface`() {
        val impl: AiExpertService = AiExpertServiceImpl(client)
        assertTrue(impl is AiExpertServiceImpl)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.remote.api.AiExpertServiceImplTest"
```
Expected: FAILED — `AiExpertService` does not exist yet.

- [ ] **Step 3: Create `AiExpertService.kt`**

Create `app/src/main/java/com/android/zubanx/data/remote/api/AiExpertService.kt`:
```kotlin
package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.AiExpertResponseDto

/**
 * Unified AI expert service interface.
 *
 * [expert] must be one of: `"GPT"`, `"GEMINI"`, `"CLAUDE"`, `"DEFAULT"`.
 * The implementation delegates to the correct backend based on [expert].
 *
 * This service is used for:
 * - AI-powered translation (when the user selects a non-default expert)
 * - Dictionary detail enrichment (Plans 6)
 * - Idiom / phrase / story explanations (Plans 7–8)
 *
 * Full implementation is added in Plans 5–8 as each feature is built.
 */
interface AiExpertService {
    suspend fun ask(expert: String, prompt: String): NetworkResult<AiExpertResponseDto>
}
```

- [ ] **Step 4: Create `AiExpertServiceImpl.kt`**

Create `app/src/main/java/com/android/zubanx/data/remote/api/AiExpertServiceImpl.kt`:
```kotlin
package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.AiExpertResponseDto
import io.ktor.client.HttpClient

/**
 * Stub implementation of [AiExpertService].
 *
 * Always returns [NetworkResult.Error] with a "not implemented" message.
 * Real delegation to GPT / Gemini / Claude / Default backends is added
 * incrementally in Plans 5–8 as each feature is built.
 */
class AiExpertServiceImpl(
    private val client: HttpClient
) : AiExpertService {

    override suspend fun ask(
        expert: String,
        prompt: String
    ): NetworkResult<AiExpertResponseDto> {
        return NetworkResult.Error(
            "AiExpertService[$expert]: not implemented yet — see Plans 5-8"
        )
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.remote.api.AiExpertServiceImplTest"
```
Expected: BUILD SUCCESSFUL, 5 tests passed.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/android/zubanx/data/remote/api/AiExpertService.kt \
        app/src/main/java/com/android/zubanx/data/remote/api/AiExpertServiceImpl.kt \
        app/src/test/java/com/android/zubanx/data/remote/api/AiExpertServiceImplTest.kt
git commit -m "feat: add AiExpertService interface and stub implementation (GPT/Gemini/Claude/Default)"
```

---

## Chunk 4: Security and ML Kit Services

### Task 10: `KeyDecryptionModule`

**Files:**
- Create: `app/src/main/java/com/android/zubanx/security/KeyDecryptionModule.kt`
- Test: `app/src/test/java/com/android/zubanx/security/KeyDecryptionModuleTest.kt`

Firebase Remote Config stores AES-encrypted key strings (e.g. `"openai_key_enc"`, `"gemini_key_enc"`, `"claude_key_enc"`). `KeyDecryptionModule` fetches the value and decrypts it using Android `javax.crypto`. For now the stub provides a minimal interface with a safe placeholder — real AES + IV logic is added when the AI expert credentials are provisioned.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/android/zubanx/security/KeyDecryptionModuleTest.kt`:
```kotlin
package com.android.zubanx.security

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyDecryptionModuleTest {

    private val remoteConfig: FirebaseRemoteConfig = mockk(relaxed = true)
    private val module = KeyDecryptionModule(remoteConfig)

    @Test
    fun `getDecryptedKey returns empty string when remote config value is blank`() {
        every { remoteConfig.getString(any()) } returns ""
        val result = module.getDecryptedKey("openai_key_enc")
        assertEquals("", result)
    }

    @Test
    fun `getDecryptedKey calls remoteConfig getString with the given key name`() {
        every { remoteConfig.getString("gemini_key_enc") } returns ""
        module.getDecryptedKey("gemini_key_enc")
        verify { remoteConfig.getString("gemini_key_enc") }
    }

    @Test
    fun `getDecryptedKey returns placeholder for non-blank value until AES is implemented`() {
        every { remoteConfig.getString("claude_key_enc") } returns "encryptedPayload=="
        val result = module.getDecryptedKey("claude_key_enc")
        // Stub: returns the raw value unchanged until AES cipher is wired in Plans 5-8
        assertTrue(result.isNotEmpty())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.security.KeyDecryptionModuleTest"
```
Expected: FAILED — `KeyDecryptionModule` does not exist yet.

- [ ] **Step 3: Create `KeyDecryptionModule.kt`**

Create `app/src/main/java/com/android/zubanx/security/KeyDecryptionModule.kt`:
```kotlin
package com.android.zubanx.security

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import timber.log.Timber

/**
 * Fetches and decrypts API keys stored as AES-encrypted strings in Firebase Remote Config.
 *
 * Keys are never stored in plaintext on-device. Each key is fetched fresh for each
 * network call via [getDecryptedKey].
 *
 * STUB: The AES cipher wiring is added in Plans 5-8 when actual API keys are provisioned.
 * The current implementation returns the raw Remote Config value unchanged.
 *
 * @param remoteConfig Injected Firebase Remote Config singleton.
 */
class KeyDecryptionModule(
    private val remoteConfig: FirebaseRemoteConfig
) {

    /**
     * Returns the decrypted API key for [keyName].
     *
     * Returns an empty string if the Remote Config value is blank (not yet fetched
     * or the key is not published). Callers must guard against an empty result.
     *
     * @param keyName The Remote Config parameter name, e.g. `"openai_key_enc"`.
     */
    fun getDecryptedKey(keyName: String): String {
        val encrypted = remoteConfig.getString(keyName)
        if (encrypted.isBlank()) {
            Timber.w("KeyDecryptionModule: no value for key '$keyName' in Remote Config")
            return ""
        }
        // TODO Plan 5-8: replace with AES/GCM decryption using the master key
        // stored in Android Keystore. For now return the raw value as a placeholder.
        return encrypted
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.security.KeyDecryptionModuleTest"
```
Expected: BUILD SUCCESSFUL, 3 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/android/zubanx/security/KeyDecryptionModule.kt \
        app/src/test/java/com/android/zubanx/security/KeyDecryptionModuleTest.kt
git commit -m "feat: add KeyDecryptionModule stub (Firebase Remote Config key fetch)"
```

---

### Task 11: `MlKitOcrService`

**Files:**
- Create: `app/src/main/java/com/android/zubanx/data/mlkit/MlKitOcrService.kt`
- Test: `app/src/test/java/com/android/zubanx/data/mlkit/MlKitOcrServiceTest.kt`

ML Kit Text Recognition (`com.google.mlkit:text-recognition`) is fully on-device. It takes an `InputImage` and returns detected text blocks. The service wraps the callback-based ML Kit API into a coroutine using `suspendCoroutine`.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/android/zubanx/data/mlkit/MlKitOcrServiceTest.kt`:
```kotlin
package com.android.zubanx.data.mlkit

import com.google.mlkit.vision.text.TextRecognizer
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Test

class MlKitOcrServiceTest {

    private val recognizer: TextRecognizer = mockk(relaxed = true)
    private val service = MlKitOcrService(recognizer)

    @Test
    fun `MlKitOcrService instantiates with a TextRecognizer`() {
        assertNotNull(service)
    }

    @Test
    fun `MlKitOcrService exposes a recognizeText function`() {
        // Verifies the function exists with the expected signature at compile time.
        // Actual ML Kit execution requires a real InputImage and is tested on-device.
        val fn = MlKitOcrService::recognizeText
        assertNotNull(fn)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.mlkit.MlKitOcrServiceTest"
```
Expected: FAILED — `MlKitOcrService` does not exist yet.

- [ ] **Step 3: Create `MlKitOcrService.kt`**

Create `app/src/main/java/com/android/zubanx/data/mlkit/MlKitOcrService.kt`:
```kotlin
package com.android.zubanx.data.mlkit

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * On-device OCR service backed by ML Kit Text Recognition.
 *
 * This service is entirely offline — it requires no network access.
 * Registered in [mlKitModule] as a `single`.
 *
 * @param recognizer Injected [TextRecognizer] from `TextRecognition.getClient(...)`.
 */
class MlKitOcrService(
    private val recognizer: TextRecognizer
) {

    /**
     * Recognizes all text in [image] and returns it as a single concatenated [String].
     *
     * Blocks the calling coroutine until ML Kit's callback fires. The function is safe
     * to call on any dispatcher — ML Kit dispatches its callback on the main thread.
     *
     * @throws Exception if ML Kit fails to process the image.
     */
    suspend fun recognizeText(image: InputImage): String = suspendCoroutine { cont ->
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                cont.resume(visionText.text)
            }
            .addOnFailureListener { exception ->
                cont.resumeWithException(exception)
            }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.mlkit.MlKitOcrServiceTest"
```
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/android/zubanx/data/mlkit/MlKitOcrService.kt \
        app/src/test/java/com/android/zubanx/data/mlkit/MlKitOcrServiceTest.kt
git commit -m "feat: add MlKitOcrService — on-device text recognition coroutine wrapper"
```

---

### Task 12: `MlKitTranslateService`

**Files:**
- Create: `app/src/main/java/com/android/zubanx/data/mlkit/MlKitTranslateService.kt`
- Test: `app/src/test/java/com/android/zubanx/data/mlkit/MlKitTranslateServiceTest.kt`

ML Kit Translate (`com.google.mlkit:translate`) downloads language model packs on demand and translates text offline. The service wraps `TranslatorOptions`, `Translation.getClient()`, language pack download, and the translate call in coroutines.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/android/zubanx/data/mlkit/MlKitTranslateServiceTest.kt`:
```kotlin
package com.android.zubanx.data.mlkit

import org.junit.Assert.assertNotNull
import org.junit.Test

class MlKitTranslateServiceTest {

    private val service = MlKitTranslateService()

    @Test
    fun `MlKitTranslateService instantiates`() {
        assertNotNull(service)
    }

    @Test
    fun `MlKitTranslateService exposes translate function`() {
        val fn = MlKitTranslateService::translate
        assertNotNull(fn)
    }

    @Test
    fun `MlKitTranslateService exposes downloadModelIfNeeded function`() {
        val fn = MlKitTranslateService::downloadModelIfNeeded
        assertNotNull(fn)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.mlkit.MlKitTranslateServiceTest"
```
Expected: FAILED — `MlKitTranslateService` does not exist yet.

- [ ] **Step 3: Create `MlKitTranslateService.kt`**

Create `app/src/main/java/com/android/zubanx/data/mlkit/MlKitTranslateService.kt`:
```kotlin
package com.android.zubanx.data.mlkit

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * On-device translation service backed by ML Kit Translate.
 *
 * Language packs are downloaded on demand via [downloadModelIfNeeded].
 * Translation is performed offline after the pack is available.
 *
 * This service is entirely offline — it requires no network access once packs
 * are downloaded. Registered in [mlKitModule] as a `single`.
 *
 * Usage pattern for callers:
 * ```kotlin
 * mlKitTranslateService.downloadModelIfNeeded("en", "es")
 * val translated = mlKitTranslateService.translate(text, "en", "es")
 * ```
 */
class MlKitTranslateService {

    /**
     * Downloads the ML Kit language model for the given [sourceLang]→[targetLang] pair
     * if it is not already present on the device.
     *
     * Language codes follow BCP-47 format (e.g. `"en"`, `"es"`, `"fr"`).
     * ML Kit maps these via [TranslateLanguage].
     *
     * @throws Exception if the download fails.
     */
    suspend fun downloadModelIfNeeded(sourceLang: String, targetLang: String) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.fromLanguageTag(sourceLang) ?: TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.fromLanguageTag(targetLang) ?: TranslateLanguage.SPANISH)
            .build()
        val translator = Translation.getClient(options)
        val conditions = DownloadConditions.Builder().build()

        suspendCoroutine<Unit> { cont ->
            translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    Timber.d("MlKitTranslateService: model downloaded for $sourceLang→$targetLang")
                    cont.resume(Unit)
                }
                .addOnFailureListener { exception ->
                    Timber.e(exception, "MlKitTranslateService: model download failed")
                    cont.resumeWithException(exception)
                }
        }

        translator.close()
    }

    /**
     * Translates [text] from [sourceLang] to [targetLang] using the on-device ML Kit model.
     *
     * Call [downloadModelIfNeeded] first if the model may not be present. If the model
     * is not downloaded, ML Kit will attempt an automatic download (requires network).
     *
     * @throws Exception if the translation fails.
     */
    suspend fun translate(text: String, sourceLang: String, targetLang: String): String {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.fromLanguageTag(sourceLang) ?: TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.fromLanguageTag(targetLang) ?: TranslateLanguage.SPANISH)
            .build()
        val translator = Translation.getClient(options)

        return suspendCoroutine { cont ->
            translator.translate(text)
                .addOnSuccessListener { translatedText ->
                    cont.resume(translatedText)
                    translator.close()
                }
                .addOnFailureListener { exception ->
                    translator.close()
                    cont.resumeWithException(exception)
                }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.data.mlkit.MlKitTranslateServiceTest"
```
Expected: BUILD SUCCESSFUL, 3 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/android/zubanx/data/mlkit/MlKitTranslateService.kt \
        app/src/test/java/com/android/zubanx/data/mlkit/MlKitTranslateServiceTest.kt
git commit -m "feat: add MlKitTranslateService — offline ML Kit translate coroutine wrapper"
```

---

## Chunk 5: Koin Module Wiring

### Task 13: Wire `networkModule`

**Files:**
- Modify: `app/src/main/java/com/android/zubanx/core/di/NetworkModule.kt`

The `networkModule` provides a singleton `HttpClient` (from `KtorClientFactory.create()`), and binds each service implementation to its interface using `singleOf(...) bind ...::class`.

- [ ] **Step 1: Write the failing build verification test**

There is no isolated unit test for the Koin module itself — wiring correctness is verified by running a full build and confirming there are no unresolved symbols. The test here is a compile-time check.

Run the current build to establish baseline:
```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL (stub modules compile cleanly).

- [ ] **Step 2: Update `NetworkModule.kt`**

Replace the contents of `app/src/main/java/com/android/zubanx/core/di/NetworkModule.kt` with:
```kotlin
package com.android.zubanx.core.di

import com.android.zubanx.core.network.KtorClientFactory
import com.android.zubanx.data.remote.api.AiExpertService
import com.android.zubanx.data.remote.api.AiExpertServiceImpl
import com.android.zubanx.data.remote.api.DictionaryApiService
import com.android.zubanx.data.remote.api.DictionaryApiServiceImpl
import com.android.zubanx.data.remote.api.TranslateApiService
import com.android.zubanx.data.remote.api.TranslateApiServiceImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule = module {
    // Singleton HttpClient — all services share one connection pool
    single { KtorClientFactory.create() }

    singleOf(::TranslateApiServiceImpl) bind TranslateApiService::class
    singleOf(::DictionaryApiServiceImpl) bind DictionaryApiService::class
    singleOf(::AiExpertServiceImpl) bind AiExpertService::class
}
```

- [ ] **Step 3: Build to verify module compiles**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/android/zubanx/core/di/NetworkModule.kt
git commit -m "feat: wire networkModule — HttpClient singleton + all API service bindings"
```

---

### Task 14: Wire `securityModule`

**Files:**
- Modify: `app/src/main/java/com/android/zubanx/core/di/SecurityModule.kt`

`KeyDecryptionModule` takes a `FirebaseRemoteConfig` instance. Koin retrieves `FirebaseRemoteConfig.getInstance()` via a plain `single { }` block (not `singleOf`) because it uses a static factory, not a constructor.

- [ ] **Step 1: Update `SecurityModule.kt`**

Replace the contents of `app/src/main/java/com/android/zubanx/core/di/SecurityModule.kt` with:
```kotlin
package com.android.zubanx.core.di

import com.android.zubanx.security.KeyDecryptionModule
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val securityModule = module {
    // FirebaseRemoteConfig uses a static getInstance() — wrap in a plain single block
    single<FirebaseRemoteConfig> { FirebaseRemoteConfig.getInstance() }

    singleOf(::KeyDecryptionModule)
}
```

- [ ] **Step 2: Build to verify module compiles**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/android/zubanx/core/di/SecurityModule.kt
git commit -m "feat: wire securityModule — FirebaseRemoteConfig + KeyDecryptionModule"
```

---

### Task 15: Wire `mlKitModule`

**Files:**
- Modify: `app/src/main/java/com/android/zubanx/core/di/MlKitModule.kt`

`MlKitOcrService` requires a `TextRecognizer`. ML Kit's `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)` is a static factory so it goes in a plain `single { }` block. `MlKitTranslateService` has no constructor parameters.

- [ ] **Step 1: Update `MlKitModule.kt`**

Replace the contents of `app/src/main/java/com/android/zubanx/core/di/MlKitModule.kt` with:
```kotlin
package com.android.zubanx.core.di

import com.android.zubanx.data.mlkit.MlKitOcrService
import com.android.zubanx.data.mlkit.MlKitTranslateService
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val mlKitModule = module {
    // TextRecognizer is created via a static ML Kit factory
    single { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    singleOf(::MlKitOcrService)
    singleOf(::MlKitTranslateService)
}
```

- [ ] **Step 2: Build to verify module compiles**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run all unit tests to confirm nothing is broken**

```bash
./gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, all tests passed.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/android/zubanx/core/di/MlKitModule.kt
git commit -m "feat: wire mlKitModule — TextRecognizer + MlKitOcrService + MlKitTranslateService"
```

---

### Task 16: Final build smoke test

This task verifies the complete Plan 3 deliverable compiles and all tests pass. No new files are created.

- [ ] **Step 1: Run full unit test suite**

```bash
./gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL. All tests in the following packages pass:
- `com.android.zubanx.core.network.*`
- `com.android.zubanx.data.remote.dto.*`
- `com.android.zubanx.data.remote.api.*`
- `com.android.zubanx.security.*`
- `com.android.zubanx.data.mlkit.*`

- [ ] **Step 2: Run debug assemble to verify Koin wiring compiles**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Tag the Plan 3 completion commit**

```bash
git tag plan-3-network-layer
```

---

## Summary

After completing all chunks, the following are in place:

**New files created:**

| File | Purpose |
|---|---|
| `core/network/NetworkResult.kt` | Sealed interface `Success`/`Error` — no `Loading` |
| `core/network/SafeApiCall.kt` | `suspend fun safeApiCall { }` with Ktor exception mapping |
| `core/network/KtorClientFactory.kt` | `HttpClient` factory (OkHttp, JSON, Timber logging) |
| `data/remote/dto/TranslateResponseDto.kt` | Google Translate scrape response DTO |
| `data/remote/dto/DictionaryResponseDto.kt` | Free Dictionary API response DTO (nested) |
| `data/remote/dto/AiExpertResponseDto.kt` | Unified AI expert response wrapper DTO |
| `data/remote/api/TranslateApiService.kt` | Interface |
| `data/remote/api/TranslateApiServiceImpl.kt` | Stub — wired in Plan 5 |
| `data/remote/api/DictionaryApiService.kt` | Interface |
| `data/remote/api/DictionaryApiServiceImpl.kt` | Stub — wired in Plan 6 |
| `data/remote/api/AiExpertService.kt` | Unified interface for all AI experts |
| `data/remote/api/AiExpertServiceImpl.kt` | Stub — wired in Plans 5–8 |
| `security/KeyDecryptionModule.kt` | Firebase Remote Config fetch + AES stub |
| `data/mlkit/MlKitOcrService.kt` | On-device OCR coroutine wrapper |
| `data/mlkit/MlKitTranslateService.kt` | On-device translate + model download coroutine wrapper |

**Modified files:**

| File | Change |
|---|---|
| `core/di/NetworkModule.kt` | HttpClient singleton + 3 service bindings |
| `core/di/SecurityModule.kt` | FirebaseRemoteConfig + KeyDecryptionModule |
| `core/di/MlKitModule.kt` | TextRecognizer + MlKitOcrService + MlKitTranslateService |
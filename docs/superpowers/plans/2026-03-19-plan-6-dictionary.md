# Dictionary Feature Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete Dictionary feature — implement the Free Dictionary API call, AI word enrichment via the selected expert, domain use cases, `DictionaryContract`/`DictionaryViewModel`, `DictionaryFragment` (search + results + history), `WordDetailFragment` (full meanings/examples/synonyms), and navigation.

**Architecture:**
- **API:** `GET https://api.dictionaryapi.dev/api/v2/entries/{language}/{word}` — response is a JSON array; take the first element. Matches existing `DictionaryResponseDto`.
- **Cache-first:** `LookupWordUseCase` checks Room first (`DictionaryRepository.getCached()`) → on miss, calls `DictionaryApiService.lookup()` → saves result to Room.
- **AI Enrichment:** Optional tap — `AiExpertService.ask(expert, prompt)` adds richer explanation, usage tips, context. Displayed as a separate "AI Insight" card.
- **Two screens:** `DictionaryFragment` (search + quick results + history) → `WordDetailFragment` (full detail, all meanings, synonyms, antonyms, AI).
- **Mic:** Same `RecognizerIntent` pattern as Translate screen — Google Mic dialog for voice search.

**Tech Stack:** Ktor 3.1.2, kotlinx-serialization-json, Room, Koin 4.1.1, MVI, Navigation Safe Args, ViewBinding, RecognizerIntent, MockK, Turbine

---

## File Structure

**Service:**
- Modify: `data/remote/api/DictionaryApiServiceImpl.kt` — real API call

**Domain:**
- Create: `domain/usecase/dictionary/LookupWordUseCase.kt`
- Create: `domain/usecase/dictionary/GetDictionaryHistoryUseCase.kt`
- Create: `domain/usecase/dictionary/EnrichWithAiUseCase.kt`

**Feature — Dictionary:**
- Create: `feature/dictionary/DictionaryContract.kt`
- Create: `feature/dictionary/DictionaryViewModel.kt`
- Create: `feature/dictionary/DictionaryFragment.kt`
- Create: `feature/dictionary/WordDetailContract.kt`
- Create: `feature/dictionary/WordDetailViewModel.kt`
- Create: `feature/dictionary/WordDetailFragment.kt`

**DI:**
- Modify: `core/di/UseCaseModule.kt` — add dictionary use cases
- Modify: `core/di/ViewModelModule.kt` — add Dictionary + WordDetail ViewModels

**Layouts:**
- Create: `res/layout/fragment_dictionary.xml`
- Create: `res/layout/fragment_word_detail.xml`
- Create: `res/layout/item_dictionary_history.xml`
- Create: `res/layout/item_meaning.xml`
- Create: `res/layout/item_definition.xml`

**Navigation:**
- Modify: `res/navigation/nav_dictionary.xml` — add DictionaryFragment + WordDetailFragment with Safe Args

**Tests (JVM unit):**
- Create: `app/src/test/java/com/android/zubanx/data/remote/api/DictionaryApiServiceImplTest.kt`
- Create: `app/src/test/java/com/android/zubanx/domain/usecase/dictionary/LookupWordUseCaseTest.kt`
- Create: `app/src/test/java/com/android/zubanx/feature/dictionary/DictionaryViewModelTest.kt`

---

## Chunk 1: DictionaryApiServiceImpl

### Task 1: Implement real API call

**Files:**
- Modify: `data/remote/api/DictionaryApiServiceImpl.kt`
- Test: `app/src/test/java/com/android/zubanx/data/remote/api/DictionaryApiServiceImplTest.kt`

- [ ] **Step 1: Update existing stub test to reflect real contract**

Read existing `app/src/test/java/com/android/zubanx/data/remote/api/DictionaryApiServiceImplTest.kt` first, then replace with:
```kotlin
package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.DictionaryResponseDto
import com.android.zubanx.data.remote.dto.MeaningDto
import com.android.zubanx.data.remote.dto.DefinitionDto
import io.mockk.coEvery
import io.mockk.mockk
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryApiServiceImplTest {

    @Test
    fun `BASE_URL is correct`() {
        assertEquals(
            "https://api.dictionaryapi.dev/api/v2/entries",
            DictionaryApiServiceImpl.BASE_URL
        )
    }

    @Test
    fun `parseFirstEntry extracts first item from JSON array`() {
        val json = Json { ignoreUnknownKeys = true }
        val rawJson = """
            [{"word":"hello","phonetic":"/həˈloʊ/","meanings":[
              {"partOfSpeech":"exclamation","definitions":[
                {"definition":"used as greeting","example":"Hello there!"}
              ]}
            ]}]
        """.trimIndent()
        val result = DictionaryApiServiceImpl.parseFirstEntry(rawJson, json)
        assertEquals("hello", result.word)
        assertEquals("/həˈloʊ/", result.phonetic)
        assertEquals(1, result.meanings.size)
    }

    @Test
    fun `parseFirstEntry returns null for empty array`() {
        val json = Json { ignoreUnknownKeys = true }
        val result = DictionaryApiServiceImpl.parseFirstEntry("[]", json)
        assertTrue(result == null)
    }
}
```

Run: `./gradlew :app:testDebugUnitTest --tests "*.DictionaryApiServiceImplTest"`
Expected: FAILED (companion object not there yet)

- [ ] **Step 2: Implement DictionaryApiServiceImpl**

Replace `app/src/main/java/com/android/zubanx/data/remote/api/DictionaryApiServiceImpl.kt`:
```kotlin
package com.android.zubanx.data.remote.api

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.core.network.safeApiCall
import com.android.zubanx.data.remote.dto.DictionaryResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class DictionaryApiServiceImpl(
    private val client: HttpClient
) : DictionaryApiService {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun lookup(
        word: String,
        language: String
    ): NetworkResult<DictionaryResponseDto> = safeApiCall {
        val raw = client.get("$BASE_URL/$language/${word.trim().lowercase()}").bodyAsText()
        parseFirstEntry(raw, json)
            ?: throw IllegalStateException("No dictionary entry found for '$word'")
    }

    companion object {
        const val BASE_URL = "https://api.dictionaryapi.dev/api/v2/entries"

        fun parseFirstEntry(raw: String, json: Json = Json { ignoreUnknownKeys = true }): DictionaryResponseDto? {
            val list = json.decodeFromString(ListSerializer(DictionaryResponseDto.serializer()), raw)
            return list.firstOrNull()
        }
    }
}
```

- [ ] **Step 3: Run tests and verify**

Run: `./gradlew :app:testDebugUnitTest --tests "*.DictionaryApiServiceImplTest"`
Expected: BUILD SUCCESSFUL — all 3 tests pass

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/android/zubanx/data/remote/api/DictionaryApiServiceImpl.kt \
        app/src/test/java/com/android/zubanx/data/remote/api/DictionaryApiServiceImplTest.kt
git commit -m "feat: implement DictionaryApiServiceImpl with Free Dictionary API"
```

---

## Chunk 2: Domain Use Cases

### Task 2: Dictionary Use Cases

**Files:**
- Create: `domain/usecase/dictionary/LookupWordUseCase.kt`
- Create: `domain/usecase/dictionary/GetDictionaryHistoryUseCase.kt`
- Create: `domain/usecase/dictionary/EnrichWithAiUseCase.kt`
- Test: `app/src/test/java/com/android/zubanx/domain/usecase/dictionary/LookupWordUseCaseTest.kt`

- [ ] **Step 1: Write failing use case tests**

Create `app/src/test/java/com/android/zubanx/domain/usecase/dictionary/LookupWordUseCaseTest.kt`:
```kotlin
package com.android.zubanx.domain.usecase.dictionary

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.api.DictionaryApiService
import com.android.zubanx.data.remote.dto.DictionaryResponseDto
import com.android.zubanx.data.remote.dto.MeaningDto
import com.android.zubanx.data.remote.dto.DefinitionDto
import com.android.zubanx.domain.model.DictionaryEntry
import com.android.zubanx.domain.repository.DictionaryRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LookupWordUseCaseTest {

    private val apiService = mockk<DictionaryApiService>()
    private val repository = mockk<DictionaryRepository>(relaxed = true)
    private val useCase = LookupWordUseCase(apiService, repository)

    @Test
    fun `invoke returns cached result without calling API`() = runTest {
        val cached = DictionaryEntry(
            word = "hello", language = "en",
            definition = "A greeting", timestamp = 1000L
        )
        coEvery { repository.getCached("hello", "en") } returns cached
        val result = useCase("hello", "en")
        assertTrue(result is NetworkResult.Success)
        assertEquals("hello", (result as NetworkResult.Success).data.word)
        coVerify(exactly = 0) { apiService.lookup(any(), any()) }
    }

    @Test
    fun `invoke calls API on cache miss and saves result`() = runTest {
        coEvery { repository.getCached("run", "en") } returns null
        coEvery { apiService.lookup("run", "en") } returns NetworkResult.Success(
            DictionaryResponseDto(
                word = "run",
                phonetic = "/rʌn/",
                meanings = listOf(
                    MeaningDto(
                        partOfSpeech = "verb",
                        definitions = listOf(DefinitionDto(definition = "Move at speed"))
                    )
                )
            )
        )
        val result = useCase("run", "en")
        assertTrue(result is NetworkResult.Success)
        assertEquals("run", (result as NetworkResult.Success).data.word)
        coVerify(exactly = 1) { repository.saveToCache(any()) }
    }

    @Test
    fun `invoke with blank word returns error`() = runTest {
        val result = useCase("  ", "en")
        assertTrue(result is NetworkResult.Error)
        coVerify(exactly = 0) { apiService.lookup(any(), any()) }
    }

    @Test
    fun `invoke propagates API error`() = runTest {
        coEvery { repository.getCached("xyz", "en") } returns null
        coEvery { apiService.lookup("xyz", "en") } returns NetworkResult.Error("Not found")
        val result = useCase("xyz", "en")
        assertTrue(result is NetworkResult.Error)
        coVerify(exactly = 0) { repository.saveToCache(any()) }
    }
}
```

Run: `./gradlew :app:testDebugUnitTest --tests "*.LookupWordUseCaseTest"`
Expected: FAILED

- [ ] **Step 2: Create LookupWordUseCase**

Create `app/src/main/java/com/android/zubanx/domain/usecase/dictionary/LookupWordUseCase.kt`:
```kotlin
package com.android.zubanx.domain.usecase.dictionary

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.api.DictionaryApiService
import com.android.zubanx.domain.model.DictionaryEntry
import com.android.zubanx.domain.repository.DictionaryRepository

/**
 * Cache-first dictionary lookup.
 *
 * 1. Check Room cache — return immediately if found.
 * 2. Call Free Dictionary API on miss.
 * 3. Save successful result to Room cache.
 */
class LookupWordUseCase(
    private val apiService: DictionaryApiService,
    private val repository: DictionaryRepository
) {
    suspend operator fun invoke(word: String, language: String): NetworkResult<DictionaryEntry> {
        if (word.isBlank()) return NetworkResult.Error("Word must not be blank")

        val cached = repository.getCached(word.trim().lowercase(), language)
        if (cached != null) return NetworkResult.Success(cached)

        return when (val result = apiService.lookup(word.trim().lowercase(), language)) {
            is NetworkResult.Success -> {
                val dto = result.data
                // Flatten: combine all definitions from all meanings into one string
                val definition = dto.meanings.joinToString("\n\n") { meaning ->
                    val defs = meaning.definitions.mapIndexed { i, d ->
                        "${i + 1}. ${d.definition}"
                    }.joinToString("\n")
                    "[${meaning.partOfSpeech}]\n$defs"
                }.ifBlank { "No definition available" }

                val examples = dto.meanings.flatMap { meaning ->
                    meaning.definitions.mapNotNull { it.example }
                }

                val entry = DictionaryEntry(
                    word = dto.word,
                    language = language,
                    definition = definition,
                    phonetic = dto.phonetic ?: dto.phonetics.firstOrNull { !it.text.isNullOrBlank() }?.text,
                    partOfSpeech = dto.meanings.firstOrNull()?.partOfSpeech,
                    examples = examples,
                    timestamp = System.currentTimeMillis()
                )
                repository.saveToCache(entry)
                NetworkResult.Success(entry)
            }
            is NetworkResult.Error -> result
        }
    }
}
```

- [ ] **Step 3: Create GetDictionaryHistoryUseCase**

Create `app/src/main/java/com/android/zubanx/domain/usecase/dictionary/GetDictionaryHistoryUseCase.kt`:
```kotlin
package com.android.zubanx.domain.usecase.dictionary

import com.android.zubanx.domain.model.DictionaryEntry
import com.android.zubanx.domain.repository.DictionaryRepository
import kotlinx.coroutines.flow.Flow

class GetDictionaryHistoryUseCase(private val repository: DictionaryRepository) {
    operator fun invoke(): Flow<List<DictionaryEntry>> = repository.getAll()
}
```

- [ ] **Step 4: Create EnrichWithAiUseCase**

Create `app/src/main/java/com/android/zubanx/domain/usecase/dictionary/EnrichWithAiUseCase.kt`:
```kotlin
package com.android.zubanx.domain.usecase.dictionary

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.api.AiExpertService

/**
 * Asks the selected AI expert for a richer explanation of a word.
 * Returns the AI's response as a plain string.
 */
class EnrichWithAiUseCase(private val aiExpertService: AiExpertService) {
    suspend operator fun invoke(word: String, language: String, expert: String): NetworkResult<String> {
        if (expert == "DEFAULT") return NetworkResult.Error("AI enrichment requires a selected expert (GPT/Gemini/Claude)")
        val prompt = buildPrompt(word, language)
        return when (val result = aiExpertService.ask(expert, prompt)) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.content)
            is NetworkResult.Error -> result
        }
    }

    private fun buildPrompt(word: String, language: String): String =
        "Explain the word \"$word\" in the $language language. Include: " +
        "1) Clear definition in simple terms, " +
        "2) 2-3 example sentences, " +
        "3) Common synonyms, " +
        "4) Usage tips or common mistakes. " +
        "Be concise and practical."
}
```

- [ ] **Step 5: Run all use case tests**

Run: `./gradlew :app:testDebugUnitTest --tests "*.LookupWordUseCaseTest"`
Expected: BUILD SUCCESSFUL — all 4 tests pass

- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/android/zubanx/domain/usecase/dictionary/ \
        app/src/test/java/com/android/zubanx/domain/usecase/dictionary/
git commit -m "feat: add dictionary domain use cases (lookup cache-first, history, AI enrich)"
```

---

## Chunk 3: MVI — Contracts + ViewModels + Koin

### Task 3: DictionaryContract + DictionaryViewModel

**Files:**
- Create: `feature/dictionary/DictionaryContract.kt`
- Create: `feature/dictionary/DictionaryViewModel.kt`
- Test: `app/src/test/java/com/android/zubanx/feature/dictionary/DictionaryViewModelTest.kt`

- [ ] **Step 1: Create DictionaryContract**

Create `app/src/main/java/com/android/zubanx/feature/dictionary/DictionaryContract.kt`:
```kotlin
package com.android.zubanx.feature.dictionary

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState
import com.android.zubanx.domain.model.DictionaryEntry

object DictionaryContract {

    sealed interface State : UiState {
        data object Idle : State
        data object Searching : State
        data class Success(
            val entry: DictionaryEntry,
            val history: List<DictionaryEntry> = emptyList()
        ) : State
        data class Error(
            val message: String,
            val history: List<DictionaryEntry> = emptyList()
        ) : State
    }

    sealed class Event : UiEvent {
        data class QueryChanged(val text: String) : Event()
        data object SearchClicked : Event()
        data class MicResult(val text: String) : Event()
        data object ClearSearch : Event()
        data class HistoryItemClicked(val entry: DictionaryEntry) : Event()
        data class NavigateToDetail(val entry: DictionaryEntry) : Event()
    }

    sealed class Effect : UiEffect {
        data class ShowToast(val message: String) : Effect()
        data class OpenWordDetail(val entry: DictionaryEntry) : Effect()
        data class LaunchMic(val langCode: String?) : Effect()
    }
}
```

- [ ] **Step 2: Write failing ViewModel tests**

Create `app/src/test/java/com/android/zubanx/feature/dictionary/DictionaryViewModelTest.kt`:
```kotlin
package com.android.zubanx.feature.dictionary

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.domain.model.DictionaryEntry
import com.android.zubanx.domain.usecase.dictionary.GetDictionaryHistoryUseCase
import com.android.zubanx.domain.usecase.dictionary.LookupWordUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DictionaryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val lookupUseCase = mockk<LookupWordUseCase>()
    private val historyUseCase = mockk<GetDictionaryHistoryUseCase>()

    private lateinit var viewModel: DictionaryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { historyUseCase() } returns flowOf(emptyList())
        viewModel = DictionaryViewModel(lookupUseCase, historyUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() {
        assertTrue(viewModel.state.value is DictionaryContract.State.Idle)
    }

    @Test
    fun `SearchClicked with blank query emits Error`() = runTest {
        viewModel.onEvent(DictionaryContract.Event.QueryChanged(""))
        viewModel.onEvent(DictionaryContract.Event.SearchClicked)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value is DictionaryContract.State.Error)
    }

    @Test
    fun `SearchClicked with valid word transitions to Success`() = runTest {
        val entry = DictionaryEntry(
            word = "hello", language = "en",
            definition = "[exclamation]\n1. Used as a greeting",
            phonetic = "/həˈloʊ/",
            timestamp = 1000L
        )
        coEvery { lookupUseCase("hello", "en") } returns NetworkResult.Success(entry)
        viewModel.onEvent(DictionaryContract.Event.QueryChanged("hello"))
        viewModel.onEvent(DictionaryContract.Event.SearchClicked)
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state is DictionaryContract.State.Success)
        assertEquals("hello", (state as DictionaryContract.State.Success).entry.word)
    }

    @Test
    fun `MicResult triggers search`() = runTest {
        val entry = DictionaryEntry(
            word = "run", language = "en",
            definition = "[verb]\n1. Move at speed",
            timestamp = 2000L
        )
        coEvery { lookupUseCase("run", "en") } returns NetworkResult.Success(entry)
        viewModel.onEvent(DictionaryContract.Event.MicResult("run"))
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value is DictionaryContract.State.Success)
    }

    @Test
    fun `ClearSearch resets state to Idle`() = runTest {
        viewModel.onEvent(DictionaryContract.Event.ClearSearch)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value is DictionaryContract.State.Idle)
    }
}
```

Run: `./gradlew :app:testDebugUnitTest --tests "*.DictionaryViewModelTest"`
Expected: FAILED

- [ ] **Step 3: Create DictionaryViewModel**

Create `app/src/main/java/com/android/zubanx/feature/dictionary/DictionaryViewModel.kt`:
```kotlin
package com.android.zubanx.feature.dictionary

import androidx.lifecycle.viewModelScope
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.domain.model.DictionaryEntry
import com.android.zubanx.domain.usecase.dictionary.GetDictionaryHistoryUseCase
import com.android.zubanx.domain.usecase.dictionary.LookupWordUseCase
import kotlinx.coroutines.launch

class DictionaryViewModel(
    private val lookupUseCase: LookupWordUseCase,
    private val historyUseCase: GetDictionaryHistoryUseCase
) : BaseViewModel<DictionaryContract.State, DictionaryContract.Event, DictionaryContract.Effect>(
    DictionaryContract.State.Idle
) {
    private var currentQuery = ""
    private var historyList: List<DictionaryEntry> = emptyList()

    init {
        viewModelScope.launch {
            historyUseCase().collect { list ->
                historyList = list
                val current = state.value
                if (current is DictionaryContract.State.Success) {
                    setState { (current as DictionaryContract.State.Success).copy(history = list) }
                } else if (current is DictionaryContract.State.Error) {
                    setState { (current as DictionaryContract.State.Error).copy(history = list) }
                }
            }
        }
    }

    override fun onEvent(event: DictionaryContract.Event) {
        when (event) {
            is DictionaryContract.Event.QueryChanged -> currentQuery = event.text
            is DictionaryContract.Event.SearchClicked -> search(currentQuery)
            is DictionaryContract.Event.MicResult -> {
                currentQuery = event.text
                search(event.text)
            }
            is DictionaryContract.Event.ClearSearch -> {
                currentQuery = ""
                setState { DictionaryContract.State.Idle }
            }
            is DictionaryContract.Event.HistoryItemClicked -> loadFromHistory(event.entry)
            is DictionaryContract.Event.NavigateToDetail -> {
                val entry = (state.value as? DictionaryContract.State.Success)?.entry ?: return
                sendEffect(DictionaryContract.Effect.OpenWordDetail(entry))
            }
        }
    }

    private fun search(word: String, language: String = "en") {
        if (word.isBlank()) {
            setState { DictionaryContract.State.Error("Enter a word to search", history = historyList) }
            return
        }
        setState { DictionaryContract.State.Searching }
        viewModelScope.launch {
            when (val result = lookupUseCase(word, language)) {
                is NetworkResult.Success -> setState {
                    DictionaryContract.State.Success(entry = result.data, history = historyList)
                }
                is NetworkResult.Error -> setState {
                    DictionaryContract.State.Error(message = result.message, history = historyList)
                }
            }
        }
    }

    private fun loadFromHistory(entry: DictionaryEntry) {
        currentQuery = entry.word
        setState { DictionaryContract.State.Success(entry = entry, history = historyList) }
    }
}
```

- [ ] **Step 4: Create WordDetailContract + WordDetailViewModel**

Create `app/src/main/java/com/android/zubanx/feature/dictionary/WordDetailContract.kt`:
```kotlin
package com.android.zubanx.feature.dictionary

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState
import com.android.zubanx.domain.model.DictionaryEntry
import com.android.zubanx.data.remote.dto.DictionaryResponseDto

object WordDetailContract {

    sealed interface State : UiState {
        data class Loaded(
            val entry: DictionaryEntry,
            val aiInsight: String? = null,
            val aiLoading: Boolean = false
        ) : State
        data class Error(val message: String) : State
    }

    sealed class Event : UiEvent {
        data class Load(val entry: DictionaryEntry) : Event()
        data class EnrichWithAi(val expert: String) : Event()
        data object SpeakWord : Event()
        data object CopyDefinition : Event()
    }

    sealed class Effect : UiEffect {
        data class ShowToast(val message: String) : Effect()
        data class SpeakText(val text: String, val langCode: String) : Effect()
        data class CopyToClipboard(val text: String) : Effect()
    }
}
```

Create `app/src/main/java/com/android/zubanx/feature/dictionary/WordDetailViewModel.kt`:
```kotlin
package com.android.zubanx.feature.dictionary

import androidx.lifecycle.viewModelScope
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.domain.model.DictionaryEntry
import com.android.zubanx.domain.usecase.dictionary.EnrichWithAiUseCase
import kotlinx.coroutines.launch

class WordDetailViewModel(
    private val enrichWithAiUseCase: EnrichWithAiUseCase
) : BaseViewModel<WordDetailContract.State, WordDetailContract.Event, WordDetailContract.Effect>(
    WordDetailContract.State.Loaded(DictionaryEntry(word = "", language = "", definition = "", timestamp = 0L))
) {

    override fun onEvent(event: WordDetailContract.Event) {
        when (event) {
            is WordDetailContract.Event.Load -> setState {
                WordDetailContract.State.Loaded(entry = event.entry)
            }
            is WordDetailContract.Event.EnrichWithAi -> enrichWithAi(event.expert)
            is WordDetailContract.Event.SpeakWord -> speakWord()
            is WordDetailContract.Event.CopyDefinition -> copyDefinition()
        }
    }

    private fun enrichWithAi(expert: String) {
        val loaded = state.value as? WordDetailContract.State.Loaded ?: return
        setState { (loaded as WordDetailContract.State.Loaded).copy(aiLoading = true) }
        viewModelScope.launch {
            val entry = (state.value as? WordDetailContract.State.Loaded)?.entry ?: return@launch
            when (val result = enrichWithAiUseCase(entry.word, entry.language, expert)) {
                is NetworkResult.Success -> setState {
                    (state.value as? WordDetailContract.State.Loaded)?.copy(
                        aiInsight = result.data,
                        aiLoading = false
                    ) ?: this
                }
                is NetworkResult.Error -> {
                    setState {
                        (state.value as? WordDetailContract.State.Loaded)?.copy(aiLoading = false) ?: this
                    }
                    sendEffect(WordDetailContract.Effect.ShowToast(result.message))
                }
            }
        }
    }

    private fun speakWord() {
        val entry = (state.value as? WordDetailContract.State.Loaded)?.entry ?: return
        sendEffect(WordDetailContract.Effect.SpeakText(entry.word, entry.language))
    }

    private fun copyDefinition() {
        val entry = (state.value as? WordDetailContract.State.Loaded)?.entry ?: return
        sendEffect(WordDetailContract.Effect.CopyToClipboard(entry.definition))
        sendEffect(WordDetailContract.Effect.ShowToast("Definition copied"))
    }
}
```

- [ ] **Step 5: Update Koin modules**

Modify `core/di/UseCaseModule.kt` — add:
```kotlin
import com.android.zubanx.domain.usecase.dictionary.EnrichWithAiUseCase
import com.android.zubanx.domain.usecase.dictionary.GetDictionaryHistoryUseCase
import com.android.zubanx.domain.usecase.dictionary.LookupWordUseCase
// inside module { ... } add:
factoryOf(::LookupWordUseCase)
factoryOf(::GetDictionaryHistoryUseCase)
factoryOf(::EnrichWithAiUseCase)
```

Modify `core/di/ViewModelModule.kt` — add:
```kotlin
import com.android.zubanx.feature.dictionary.DictionaryViewModel
import com.android.zubanx.feature.dictionary.WordDetailViewModel
// inside module { ... } add:
viewModelOf(::DictionaryViewModel)
viewModelOf(::WordDetailViewModel)
```

- [ ] **Step 6: Run all tests and verify**

Run: `./gradlew :app:testDebugUnitTest --tests "*.DictionaryViewModelTest"`
Expected: BUILD SUCCESSFUL — all 5 tests pass

- [ ] **Step 7: Commit**
```bash
git add app/src/main/java/com/android/zubanx/feature/dictionary/ \
        app/src/main/java/com/android/zubanx/core/di/ \
        app/src/test/java/com/android/zubanx/feature/dictionary/
git commit -m "feat: DictionaryContract/ViewModel, WordDetailContract/ViewModel + Koin wiring"
```

---

## Chunk 4: UI — Layouts + Fragments + Navigation

### Task 4: Layouts

**Files:**
- Create: `res/layout/fragment_dictionary.xml`
- Create: `res/layout/fragment_word_detail.xml`
- Create: `res/layout/item_dictionary_history.xml`

- [ ] **Step 1: Create fragment_dictionary.xml**

Create `app/src/main/res/layout/fragment_dictionary.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="?attr/colorSurface">

    <!-- Search bar -->
    <LinearLayout
        android:id="@+id/searchBar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:padding="12dp"
        android:background="?attr/colorSurface">

        <com.google.android.material.textfield.TextInputLayout
            android:id="@+id/tilSearch"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
            app:endIconMode="clear_text"
            app:startIconDrawable="@drawable/ic_search">

            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etSearch"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="Search word"
                android:imeOptions="actionSearch"
                android:inputType="text"
                android:maxLines="1" />
        </com.google.android.material.textfield.TextInputLayout>

        <ImageButton
            android:id="@+id/btnMic"
            android:layout_width="44dp"
            android:layout_height="44dp"
            android:layout_marginStart="8dp"
            android:src="@drawable/ic_mic"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="Voice search" />
    </LinearLayout>

    <androidx.core.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:layout_marginTop="72dp"
        android:fillViewport="true">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp">

            <!-- Loading -->
            <com.google.android.material.progressindicator.LinearProgressIndicator
                android:id="@+id/progressSearch"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:indeterminate="true"
                android:visibility="gone" />

            <!-- Error -->
            <TextView
                android:id="@+id/tvError"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center"
                android:textColor="?attr/colorError"
                android:visibility="gone" />

            <!-- Result card -->
            <com.google.android.material.card.MaterialCardView
                android:id="@+id/resultCard"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                app:cardElevation="2dp"
                app:cardCornerRadius="12dp"
                android:visibility="gone">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="vertical"
                    android:padding="16dp">

                    <!-- Word + phonetic -->
                    <TextView
                        android:id="@+id/tvWord"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:textSize="28sp"
                        android:textStyle="bold"
                        android:textColor="?attr/colorOnSurface" />

                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:gravity="center_vertical"
                        android:layout_marginTop="4dp">

                        <TextView
                            android:id="@+id/tvPhonetic"
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:textSize="16sp"
                            android:textColor="?attr/colorPrimary" />

                        <ImageButton
                            android:id="@+id/btnSpeak"
                            android:layout_width="36dp"
                            android:layout_height="36dp"
                            android:src="@drawable/ic_volume_up"
                            android:background="?attr/selectableItemBackgroundBorderless"
                            android:contentDescription="Pronounce word" />
                    </LinearLayout>

                    <!-- Part of speech chip -->
                    <com.google.android.material.chip.Chip
                        android:id="@+id/chipPartOfSpeech"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:layout_marginTop="8dp"
                        android:visibility="gone" />

                    <com.google.android.material.divider.MaterialDivider
                        android:layout_width="match_parent"
                        android:layout_height="1dp"
                        android:layout_marginVertical="12dp" />

                    <!-- Definition -->
                    <TextView
                        android:id="@+id/tvDefinition"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:textSize="15sp"
                        android:textColor="?attr/colorOnSurface"
                        android:lineSpacingMultiplier="1.4" />

                    <!-- Action buttons -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:orientation="horizontal"
                        android:layout_marginTop="12dp">

                        <com.google.android.material.button.MaterialButton
                            android:id="@+id/btnViewDetail"
                            style="@style/Widget.Material3.Button.OutlinedButton"
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="Full Detail"
                            android:layout_marginEnd="4dp" />

                        <com.google.android.material.button.MaterialButton
                            android:id="@+id/btnCopy"
                            style="@style/Widget.Material3.Button.OutlinedButton"
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_weight="1"
                            android:text="Copy"
                            android:layout_marginStart="4dp" />
                    </LinearLayout>
                </LinearLayout>
            </com.google.android.material.card.MaterialCardView>

            <!-- History section -->
            <TextView
                android:id="@+id/tvHistoryLabel"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="24dp"
                android:text="Recent Lookups"
                android:textSize="14sp"
                android:textStyle="bold"
                android:textColor="?attr/colorOnSurfaceVariant"
                android:visibility="gone" />

            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rvHistory"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:nestedScrollingEnabled="false"
                android:visibility="gone" />
        </LinearLayout>
    </androidx.core.widget.NestedScrollView>

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

- [ ] **Step 2: Create fragment_word_detail.xml**

Create `app/src/main/res/layout/fragment_word_detail.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.core.widget.NestedScrollView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="?attr/colorSurface">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <!-- Word header -->
        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            app:cardElevation="2dp"
            app:cardCornerRadius="12dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical">

                    <TextView
                        android:id="@+id/tvDetailWord"
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:textSize="32sp"
                        android:textStyle="bold"
                        android:textColor="?attr/colorOnSurface" />

                    <ImageButton
                        android:id="@+id/btnDetailSpeak"
                        android:layout_width="40dp"
                        android:layout_height="40dp"
                        android:src="@drawable/ic_volume_up"
                        android:background="?attr/selectableItemBackgroundBorderless"
                        android:contentDescription="Pronounce word" />

                    <ImageButton
                        android:id="@+id/btnDetailCopy"
                        android:layout_width="40dp"
                        android:layout_height="40dp"
                        android:src="@drawable/ic_copy"
                        android:background="?attr/selectableItemBackgroundBorderless"
                        android:contentDescription="Copy definition" />
                </LinearLayout>

                <TextView
                    android:id="@+id/tvDetailPhonetic"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:textSize="18sp"
                    android:textColor="?attr/colorPrimary" />

                <com.google.android.material.chip.Chip
                    android:id="@+id/chipDetailPartOfSpeech"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:visibility="gone" />
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>

        <!-- Full definition -->
        <com.google.android.material.card.MaterialCardView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            app:cardElevation="1dp"
            app:cardCornerRadius="12dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="Definition"
                    android:textSize="12sp"
                    android:textAllCaps="true"
                    android:letterSpacing="0.1"
                    android:textColor="?attr/colorPrimary"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvDetailDefinition"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:textSize="15sp"
                    android:lineSpacingMultiplier="1.5"
                    android:textColor="?attr/colorOnSurface" />
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>

        <!-- Examples -->
        <com.google.android.material.card.MaterialCardView
            android:id="@+id/cardExamples"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            app:cardElevation="1dp"
            app:cardCornerRadius="12dp"
            android:visibility="gone">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">

                <TextView
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:text="Examples"
                    android:textSize="12sp"
                    android:textAllCaps="true"
                    android:letterSpacing="0.1"
                    android:textColor="?attr/colorPrimary"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvDetailExamples"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:textSize="14sp"
                    android:lineSpacingMultiplier="1.5"
                    android:textColor="?attr/colorOnSurfaceVariant"
                    android:fontFamily="serif" />
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>

        <!-- AI Insight card -->
        <com.google.android.material.card.MaterialCardView
            android:id="@+id/cardAiInsight"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            app:cardElevation="1dp"
            app:cardCornerRadius="12dp"
            app:strokeColor="?attr/colorPrimary"
            app:strokeWidth="1dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:orientation="vertical"
                android:padding="16dp">

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:gravity="center_vertical">

                    <TextView
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:text="AI Insight"
                        android:textSize="12sp"
                        android:textAllCaps="true"
                        android:letterSpacing="0.1"
                        android:textColor="?attr/colorPrimary"
                        android:textStyle="bold" />

                    <com.google.android.material.button.MaterialButton
                        android:id="@+id/btnAiEnrich"
                        style="@style/Widget.Material3.Button.TonalButton"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Explain with AI"
                        android:textSize="12sp" />
                </LinearLayout>

                <com.google.android.material.progressindicator.LinearProgressIndicator
                    android:id="@+id/progressAi"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:indeterminate="true"
                    android:visibility="gone" />

                <TextView
                    android:id="@+id/tvAiInsight"
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:textSize="14sp"
                    android:lineSpacingMultiplier="1.5"
                    android:textColor="?attr/colorOnSurface"
                    android:visibility="gone" />
            </LinearLayout>
        </com.google.android.material.card.MaterialCardView>
    </LinearLayout>
</androidx.core.widget.NestedScrollView>
```

- [ ] **Step 3: Create item_dictionary_history.xml**

Create `app/src/main/res/layout/item_dictionary_history.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginBottom="6dp"
    app:cardElevation="0dp"
    app:strokeWidth="1dp"
    app:strokeColor="?attr/colorOutline"
    app:cardCornerRadius="8dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:padding="12dp">

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical">

            <TextView
                android:id="@+id/tvHistoryWord"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textSize="16sp"
                android:textStyle="bold"
                android:textColor="?attr/colorOnSurface" />

            <TextView
                android:id="@+id/tvHistoryPhonetic"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textSize="13sp"
                android:textColor="?attr/colorPrimary"
                android:visibility="gone" />
        </LinearLayout>

        <ImageButton
            android:id="@+id/btnHistoryDetail"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:src="@drawable/ic_arrow_forward"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="View detail" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 4: Add ic_search and ic_arrow_forward drawables**

Create `app/src/main/res/drawable/ic_search.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorOnSurface">
  <path
      android:fillColor="@android:color/white"
      android:pathData="M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z"/>
</vector>
```

Create `app/src/main/res/drawable/ic_arrow_forward.xml`:
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorOnSurfaceVariant">
  <path
      android:fillColor="@android:color/white"
      android:pathData="M12,4l-1.41,1.41L16.17,11H4v2h12.17l-5.58,5.59L12,20l8,-8z"/>
</vector>
```

- [ ] **Step 5: Commit layouts and drawables**
```bash
git add app/src/main/res/
git commit -m "feat: add dictionary screen layouts and drawables"
```

---

### Task 5: DictionaryFragment + WordDetailFragment + Navigation

**Files:**
- Create: `feature/dictionary/DictionaryFragment.kt`
- Create: `feature/dictionary/WordDetailFragment.kt`
- Modify: `res/navigation/nav_dictionary.xml`

- [ ] **Step 1: Create DictionaryFragment**

Create `app/src/main/java/com/android/zubanx/feature/dictionary/DictionaryFragment.kt`:
```kotlin
package com.android.zubanx.feature.dictionary

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.core.utils.toast
import com.android.zubanx.databinding.FragmentDictionaryBinding
import com.android.zubanx.databinding.ItemDictionaryHistoryBinding
import com.android.zubanx.domain.model.DictionaryEntry
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

class DictionaryFragment : BaseFragment<FragmentDictionaryBinding>(FragmentDictionaryBinding::inflate) {

    private val viewModel: DictionaryViewModel by viewModel()

    private val historyAdapter = HistoryAdapter(
        onItemClick = { viewModel.onEvent(DictionaryContract.Event.HistoryItemClicked(it)) },
        onDetailClick = { viewModel.onEvent(DictionaryContract.Event.NavigateToDetail(it)) }
    )

    private val micLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                binding.etSearch.setText(spoken)
                viewModel.onEvent(DictionaryContract.Event.MicResult(spoken))
            }
        }
    }

    override fun setupViews() {
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.onEvent(DictionaryContract.Event.QueryChanged(s?.toString() ?: ""))
            }
        })

        binding.etSearch.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                hideKeyboard()
                viewModel.onEvent(DictionaryContract.Event.SearchClicked)
                true
            } else false
        }

        binding.btnMic.setOnClickListener { launchMic() }
        binding.btnSpeak.setOnClickListener {
            val word = binding.tvWord.text?.toString() ?: return@setOnClickListener
            requireContext().toast("Speaking: $word")
        }
        binding.btnCopy.setOnClickListener {
            val def = binding.tvDefinition.text?.toString() ?: return@setOnClickListener
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("definition", def))
            requireContext().toast("Copied")
        }
        binding.btnViewDetail.setOnClickListener {
            viewModel.onEvent(DictionaryContract.Event.NavigateToDetail(
                (viewModel.state.value as? DictionaryContract.State.Success)?.entry ?: return@setOnClickListener
            ))
        }
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            when (state) {
                is DictionaryContract.State.Idle -> renderIdle()
                is DictionaryContract.State.Searching -> renderSearching()
                is DictionaryContract.State.Success -> renderSuccess(state)
                is DictionaryContract.State.Error -> renderError(state)
            }
        }
        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is DictionaryContract.Effect.ShowToast -> requireContext().toast(effect.message)
                is DictionaryContract.Effect.OpenWordDetail -> {
                    val action = DictionaryFragmentDirections.actionDictionaryToWordDetail(
                        word = effect.entry.word,
                        language = effect.entry.language
                    )
                    com.android.zubanx.core.navigation.safeNavigate(findNavController(), action)
                }
                is DictionaryContract.Effect.LaunchMic -> launchMic()
            }
        }
    }

    private fun renderIdle() {
        binding.resultCard.isVisible = false
        binding.progressSearch.isVisible = false
        binding.tvError.isVisible = false
        binding.tvHistoryLabel.isVisible = false
        binding.rvHistory.isVisible = false
    }

    private fun renderSearching() {
        binding.progressSearch.isVisible = true
        binding.resultCard.isVisible = false
        binding.tvError.isVisible = false
    }

    private fun renderSuccess(state: DictionaryContract.State.Success) {
        binding.progressSearch.isVisible = false
        binding.tvError.isVisible = false
        binding.resultCard.isVisible = true

        val entry = state.entry
        binding.tvWord.text = entry.word
        binding.tvPhonetic.text = entry.phonetic ?: ""
        binding.tvPhonetic.isVisible = !entry.phonetic.isNullOrBlank()
        binding.tvDefinition.text = entry.definition

        if (!entry.partOfSpeech.isNullOrBlank()) {
            binding.chipPartOfSpeech.isVisible = true
            binding.chipPartOfSpeech.text = entry.partOfSpeech
        } else {
            binding.chipPartOfSpeech.isVisible = false
        }

        updateHistory(state.history)
    }

    private fun renderError(state: DictionaryContract.State.Error) {
        binding.progressSearch.isVisible = false
        binding.resultCard.isVisible = false
        binding.tvError.isVisible = true
        binding.tvError.text = state.message
        updateHistory(state.history)
    }

    private fun updateHistory(history: List<DictionaryEntry>) {
        val visible = history.isNotEmpty()
        binding.tvHistoryLabel.isVisible = visible
        binding.rvHistory.isVisible = visible
        historyAdapter.submitList(history)
    }

    private fun launchMic() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a word")
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, requireContext().packageName)
            val lang = "en-EN"
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en")
        }
        micLauncher.launch(intent)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun findNavController() =
        androidx.navigation.fragment.NavHostFragment.findNavController(this)

    // --- History adapter ---
    class HistoryAdapter(
        private val onItemClick: (DictionaryEntry) -> Unit,
        private val onDetailClick: (DictionaryEntry) -> Unit
    ) : ListAdapter<DictionaryEntry, HistoryAdapter.VH>(DIFF) {

        inner class VH(val b: ItemDictionaryHistoryBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemDictionaryHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = getItem(position)
            holder.b.tvHistoryWord.text = item.word
            holder.b.tvHistoryPhonetic.text = item.phonetic ?: ""
            holder.b.tvHistoryPhonetic.isVisible = !item.phonetic.isNullOrBlank()
            holder.b.root.setOnClickListener { onItemClick(item) }
            holder.b.btnHistoryDetail.setOnClickListener { onDetailClick(item) }
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<DictionaryEntry>() {
                override fun areItemsTheSame(a: DictionaryEntry, b: DictionaryEntry) = a.id == b.id && a.word == b.word
                override fun areContentsTheSame(a: DictionaryEntry, b: DictionaryEntry) = a == b
            }
        }
    }
}
```

- [ ] **Step 2: Create WordDetailFragment**

Create `app/src/main/java/com/android/zubanx/feature/dictionary/WordDetailFragment.kt`:
```kotlin
package com.android.zubanx.feature.dictionary

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.core.utils.toast
import com.android.zubanx.databinding.FragmentWordDetailBinding
import com.android.zubanx.domain.model.DictionaryEntry
import com.android.zubanx.domain.repository.DictionaryRepository
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class WordDetailFragment : BaseFragment<FragmentWordDetailBinding>(FragmentWordDetailBinding::inflate) {

    private val viewModel: WordDetailViewModel by viewModel()
    private val args: WordDetailFragmentArgs by navArgs()
    private val repository: DictionaryRepository by inject()

    override fun setupViews() {
        // Load entry from Room (already cached by DictionaryFragment lookup)
        val entry = runBlocking { repository.getCached(args.word, args.language) }
            ?: DictionaryEntry(word = args.word, language = args.language, definition = "", timestamp = 0L)

        viewModel.onEvent(WordDetailContract.Event.Load(entry))

        binding.btnDetailSpeak.setOnClickListener {
            viewModel.onEvent(WordDetailContract.Event.SpeakWord)
        }
        binding.btnDetailCopy.setOnClickListener {
            viewModel.onEvent(WordDetailContract.Event.CopyDefinition)
        }
        binding.btnAiEnrich.setOnClickListener {
            // Use DEFAULT fallback message — real expert selected in Settings
            viewModel.onEvent(WordDetailContract.Event.EnrichWithAi("GPT"))
        }
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            when (state) {
                is WordDetailContract.State.Loaded -> renderLoaded(state)
                is WordDetailContract.State.Error -> requireContext().toast(state.message)
            }
        }
        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is WordDetailContract.Effect.ShowToast -> requireContext().toast(effect.message)
                is WordDetailContract.Effect.SpeakText -> requireContext().toast("Speaking: ${effect.text}")
                is WordDetailContract.Effect.CopyToClipboard -> {
                    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("definition", effect.text))
                }
            }
        }
    }

    private fun renderLoaded(state: WordDetailContract.State.Loaded) {
        val entry = state.entry
        binding.tvDetailWord.text = entry.word
        binding.tvDetailPhonetic.text = entry.phonetic ?: ""
        binding.tvDetailPhonetic.isVisible = !entry.phonetic.isNullOrBlank()
        binding.tvDetailDefinition.text = entry.definition

        if (!entry.partOfSpeech.isNullOrBlank()) {
            binding.chipDetailPartOfSpeech.isVisible = true
            binding.chipDetailPartOfSpeech.text = entry.partOfSpeech
        }

        if (entry.examples.isNotEmpty()) {
            binding.cardExamples.isVisible = true
            binding.tvDetailExamples.text = entry.examples.joinToString("\n\n") { "\"$it\"" }
        } else {
            binding.cardExamples.isVisible = false
        }

        // AI insight state
        binding.progressAi.isVisible = state.aiLoading
        binding.btnAiEnrich.isVisible = !state.aiLoading && state.aiInsight == null
        if (state.aiInsight != null) {
            binding.tvAiInsight.isVisible = true
            binding.tvAiInsight.text = state.aiInsight
            binding.btnAiEnrich.isVisible = false
        }
    }
}
```

- [ ] **Step 3: Update nav_dictionary.xml with Safe Args**

Replace `app/src/main/res/navigation/nav_dictionary.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/nav_dictionary"
    android:label="Dictionary"
    app:startDestination="@id/dictionaryFragment">

    <fragment
        android:id="@+id/dictionaryFragment"
        android:name="com.android.zubanx.feature.dictionary.DictionaryFragment"
        android:label="Dictionary"
        tools:layout="@layout/fragment_dictionary">

        <action
            android:id="@+id/action_dictionary_to_wordDetail"
            app:destination="@id/wordDetailFragment"
            app:enterAnim="@anim/slide_in_right"
            app:exitAnim="@anim/slide_out_left"
            app:popEnterAnim="@anim/slide_in_left"
            app:popExitAnim="@anim/slide_out_right" />
    </fragment>

    <fragment
        android:id="@+id/wordDetailFragment"
        android:name="com.android.zubanx.feature.dictionary.WordDetailFragment"
        android:label="Word Detail"
        tools:layout="@layout/fragment_word_detail">

        <argument
            android:name="word"
            app:argType="string" />
        <argument
            android:name="language"
            app:argType="string"
            android:defaultValue="en" />
    </fragment>

</navigation>
```

- [ ] **Step 4: Fix DictionaryFragment navigation call**

In `DictionaryFragment.kt`, the `OpenWordDetail` effect uses `DictionaryFragmentDirections`. This is generated by Safe Args — verify it compiles. Also replace the navigation call with the correct extension:

Replace the effect handling in `observeState()`:
```kotlin
is DictionaryContract.Effect.OpenWordDetail -> {
    val action = DictionaryFragmentDirections.actionDictionaryToWordDetail(
        word = effect.entry.word,
        language = effect.entry.language
    )
    findNavController().navigate(action)
}
```

- [ ] **Step 5: Build and verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Run all tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL — all tests pass

- [ ] **Step 7: Final commit**
```bash
git add app/src/main/java/com/android/zubanx/feature/dictionary/ \
        app/src/main/res/navigation/nav_dictionary.xml
git commit -m "feat: DictionaryFragment, WordDetailFragment, navigation with Safe Args"
```

---

## Chunk 5: Final Verification

- [ ] **Step 1: Full test suite**
```bash
./gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Full debug build**
```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Final commit if any fixes**
```bash
git add -p
git commit -m "fix: resolve any compile issues in Plan 6 dictionary feature"
```

---

## Summary

Plan 6 delivers:
| Component | |
|---|---|
| `DictionaryApiServiceImpl` — Free Dictionary API | ✅ |
| `LookupWordUseCase` — cache-first strategy | ✅ |
| `GetDictionaryHistoryUseCase` | ✅ |
| `EnrichWithAiUseCase` — AI insight for words | ✅ |
| `DictionaryContract` + `DictionaryViewModel` | ✅ |
| `WordDetailContract` + `WordDetailViewModel` | ✅ |
| `DictionaryFragment` — search, results, history, mic | ✅ |
| `WordDetailFragment` — full detail + AI enrichment | ✅ |
| Navigation with Safe Args (word + language) | ✅ |
| Koin DI wiring | ✅ |
| Unit tests throughout (TDD) | ✅ |

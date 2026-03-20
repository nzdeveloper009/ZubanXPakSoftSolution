# Plan 8: Phrases Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a static-base, dynamically-translated Phrases feature with 10 categories, expandable phrase cards with speak/copy/zoom actions, and a full-screen zoom view; also move Settings to the Translate screen app bar and replace the Settings bottom nav tab with Phrases.

**Architecture:** Three screens (categories grid → expandable list → zoom) backed by MVI ViewModels. Phrases are hardcoded in English; translation is on-demand via `TranslateApiService` with a ViewModel-scoped in-memory cache keyed by `"$src:$tgt:$index"`. The zoom screen is stateless — it receives translated text via Safe Args and injects `TtsManager` directly.

**Tech Stack:** Kotlin, MVI (BaseViewModel), Koin 4.1.1, Ktor (via TranslateApiService), RecyclerView + ListAdapter, Navigation Safe Args, ViewBinding, Material Design 3.

**Prerequisites:**
- `TranslateApiService` exists and is registered in Koin
- `TtsManager` exists and is registered in Koin
- `LanguageItem` is in `feature/translate/LanguageItem.kt` with `code`, `name`, companion `ALL`, `DETECT`, `fromCode(code)`
- `nav_phrases.xml` exists as empty placeholder
- `NetworkResult.Success` / `NetworkResult.Error` are in `core/network/NetworkResult.kt`
- `collectFlow()` is in `core/utils/FragmentExt.kt`
- `Context.toast()` is in `core/utils/ContextExt.kt`

---

## File Map

| Action | Path | Responsibility |
|--------|------|----------------|
| Create | `domain/usecase/phrases/TranslatePhraseUseCase.kt` | Calls TranslateApiService, no history saving |
| Create | `feature/phrases/data/PhraseCategory.kt` | Sealed class with 10 category objects |
| Create | `feature/phrases/data/PhrasesData.kt` | Static English phrases map + categoryById() |
| Create | `feature/phrases/PhrasesContract.kt` | MVI contract for categories grid |
| Create | `feature/phrases/PhrasesViewModel.kt` | Holds category list, no API calls |
| Create | `feature/phrases/PhrasesFragment.kt` | 3×3 grid, navigates to category detail |
| Create | `feature/phrases/PhrasesCategoryContract.kt` | MVI contract for category detail |
| Create | `feature/phrases/PhrasesCategoryViewModel.kt` | Translation, caching, expand state |
| Create | `feature/phrases/PhrasesCategoryAdapter.kt` | ListAdapter for expandable phrase items |
| Create | `feature/phrases/PhrasesCategoryFragment.kt` | Language bar, RecyclerView, navigates to zoom |
| Create | `feature/phrases/PhrasesZoomFragment.kt` | Stateless zoom screen, TTS via by inject() |
| Create | `res/layout/fragment_phrases.xml` | RecyclerView grid |
| Create | `res/layout/fragment_phrases_category.xml` | Toolbar + language bar + RecyclerView |
| Create | `res/layout/fragment_phrases_zoom.xml` | Full-screen translated text + speak button |
| Create | `res/layout/item_phrase_category.xml` | Category card: icon + label |
| Create | `res/layout/item_phrase.xml` | Phrase row: collapsed + expanded states |
| Create | `res/drawable/ic_nav_phrases.xml` | Bottom nav icon for Phrases tab |
| Create | `res/drawable/ic_category_dining.xml` | Category icons (×10) |
| Create | `res/drawable/ic_category_emergency.xml` | |
| Create | `res/drawable/ic_category_travel.xml` | |
| Create | `res/drawable/ic_category_greeting.xml` | |
| Create | `res/drawable/ic_category_shopping.xml` | |
| Create | `res/drawable/ic_category_hotel.xml` | |
| Create | `res/drawable/ic_category_office.xml` | |
| Create | `res/drawable/ic_category_trouble.xml` | |
| Create | `res/drawable/ic_category_entertainment.xml` | |
| Create | `res/drawable/ic_category_medicine.xml` | |
| Modify | `res/navigation/nav_phrases.xml` | Add 3 fragments + 2 actions |
| Modify | `res/menu/bottom_nav_menu.xml` | Replace Settings with Phrases |
| Modify | `res/layout/fragment_translate.xml` | Add AppBarLayout + MaterialToolbar |
| Modify | `feature/translate/TranslateFragment.kt` | Wire toolbar, settings/premium navigation |
| Modify | `core/di/UseCaseModule.kt` | Register TranslatePhraseUseCase |
| Modify | `core/di/ViewModelModule.kt` | Register PhrasesViewModel, PhrasesCategoryViewModel |
| Modify | `app/MainActivity.kt` | Add settingsFragment to hidden destinations |

---

## Task 1: Static Data Layer

**Files:**
- Create: `app/src/main/java/com/android/zubanx/feature/phrases/data/PhraseCategory.kt`
- Create: `app/src/main/java/com/android/zubanx/feature/phrases/data/PhrasesData.kt`

No unit tests needed — pure static data with no logic.

- [ ] **Step 1: Create `PhraseCategory.kt`**

```kotlin
// app/src/main/java/com/android/zubanx/feature/phrases/data/PhraseCategory.kt
package com.android.zubanx.feature.phrases.data

import com.android.zubanx.R

sealed class PhraseCategory(
    val id: String,
    val displayName: String,
    val iconRes: Int
) {
    object Dining        : PhraseCategory("dining",        "Dining",        R.drawable.ic_category_dining)
    object Emergency     : PhraseCategory("emergency",     "Emergency",     R.drawable.ic_category_emergency)
    object Travel        : PhraseCategory("travel",        "Travel",        R.drawable.ic_category_travel)
    object Greeting      : PhraseCategory("greeting",      "Greeting",      R.drawable.ic_category_greeting)
    object Shopping      : PhraseCategory("shopping",      "Shopping",      R.drawable.ic_category_shopping)
    object Hotel         : PhraseCategory("hotel",         "Hotel",         R.drawable.ic_category_hotel)
    object Office        : PhraseCategory("office",        "Office",        R.drawable.ic_category_office)
    object Trouble       : PhraseCategory("trouble",       "Trouble",       R.drawable.ic_category_trouble)
    object Entertainment : PhraseCategory("entertainment", "Entertainment", R.drawable.ic_category_entertainment)
    object Medicine      : PhraseCategory("medicine",      "Medicine",      R.drawable.ic_category_medicine)
}
```

- [ ] **Step 2: Create `PhrasesData.kt`**

```kotlin
// app/src/main/java/com/android/zubanx/feature/phrases/data/PhrasesData.kt
package com.android.zubanx.feature.phrases.data

object PhrasesData {

    val categories: List<PhraseCategory> = listOf(
        PhraseCategory.Dining,
        PhraseCategory.Emergency,
        PhraseCategory.Travel,
        PhraseCategory.Greeting,
        PhraseCategory.Shopping,
        PhraseCategory.Hotel,
        PhraseCategory.Office,
        PhraseCategory.Trouble,
        PhraseCategory.Entertainment,
        PhraseCategory.Medicine
    )

    fun categoryById(id: String): PhraseCategory =
        categories.first { it.id == id }

    val phrases: Map<PhraseCategory, List<String>> = mapOf(
        PhraseCategory.Dining to listOf(
            "A table for two, please.",
            "Can I see the menu?",
            "I am allergic to nuts.",
            "What do you recommend?",
            "Could we have the bill, please?",
            "Is there a vegetarian option?",
            "No spice, please.",
            "Water, please.",
            "This is delicious!",
            "Can I have a takeaway?"
        ),
        PhraseCategory.Emergency to listOf(
            "Call an ambulance!",
            "I need a doctor.",
            "I have lost my passport.",
            "Call the police, please.",
            "There is a fire!",
            "I have been robbed.",
            "I need help.",
            "Where is the nearest hospital?",
            "I am injured.",
            "Please hurry!"
        ),
        PhraseCategory.Travel to listOf(
            "Where is the bus station?",
            "How much is the ticket?",
            "Does this bus go to the city centre?",
            "Can you call me a taxi?",
            "Where is the airport?",
            "I missed my flight.",
            "I need to go to this address.",
            "Is this the right platform?",
            "How far is it?",
            "Can I have a map?"
        ),
        PhraseCategory.Greeting to listOf(
            "Good morning!",
            "Good evening!",
            "How are you?",
            "Nice to meet you.",
            "My name is ...",
            "I do not speak this language.",
            "Do you speak English?",
            "Thank you very much.",
            "You are welcome.",
            "Goodbye!"
        ),
        PhraseCategory.Shopping to listOf(
            "How much does this cost?",
            "Do you have a smaller size?",
            "Can I try this on?",
            "I would like to buy this.",
            "Do you accept cards?",
            "Can I get a discount?",
            "Where is the fitting room?",
            "I am just looking.",
            "Can I get a receipt?",
            "Do you have this in another colour?"
        ),
        PhraseCategory.Hotel to listOf(
            "I have a reservation.",
            "I would like to check in.",
            "What time is checkout?",
            "Can I have an extra pillow?",
            "The air conditioning is not working.",
            "Can I have a wake-up call at 7?",
            "Where is the elevator?",
            "Can I have room service?",
            "I would like to extend my stay.",
            "Can you store my luggage?"
        ),
        PhraseCategory.Office to listOf(
            "I have a meeting at 10.",
            "Can I use the Wi-Fi?",
            "Where is the conference room?",
            "I need to print a document.",
            "Can I speak to the manager?",
            "I am here for an interview.",
            "Please send me the report.",
            "The projector is not working.",
            "I need a pen and paper.",
            "Can you reschedule the meeting?"
        ),
        PhraseCategory.Trouble to listOf(
            "I am lost.",
            "Can you help me?",
            "I do not understand.",
            "Please speak slowly.",
            "Can you write that down?",
            "I need a translator.",
            "This is not what I ordered.",
            "There is a problem with my room.",
            "I want to make a complaint.",
            "Can I speak to a supervisor?"
        ),
        PhraseCategory.Entertainment to listOf(
            "Two tickets, please.",
            "What time does the show start?",
            "Where is the entrance?",
            "Is there a student discount?",
            "Can I take photos here?",
            "Where is the nearest cinema?",
            "What is showing tonight?",
            "I would like to book in advance.",
            "Is this show suitable for children?",
            "Can I get a programme?"
        ),
        PhraseCategory.Medicine to listOf(
            "I need a pharmacy.",
            "I have a headache.",
            "I feel nauseous.",
            "I am diabetic.",
            "I am allergic to penicillin.",
            "I need my prescription filled.",
            "How many times a day should I take this?",
            "Do you have pain relief?",
            "I have a fever.",
            "I need to see a doctor urgently."
        )
    )
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/android/zubanx/feature/phrases/data/
git commit -m "feat(phrases): add static PhraseCategory and PhrasesData"
```

---

## Task 2: TranslatePhraseUseCase (TDD)

**Files:**
- Create: `app/src/main/java/com/android/zubanx/domain/usecase/phrases/TranslatePhraseUseCase.kt`
- Create: `app/src/test/java/com/android/zubanx/domain/usecase/phrases/TranslatePhraseUseCaseTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// app/src/test/java/com/android/zubanx/domain/usecase/phrases/TranslatePhraseUseCaseTest.kt
package com.android.zubanx.domain.usecase.phrases

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.api.TranslateApiService
import com.android.zubanx.data.remote.dto.TranslateResponseDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslatePhraseUseCaseTest {

    private val apiService: TranslateApiService = mockk()
    private val useCase = TranslatePhraseUseCase(apiService)

    @Test
    fun `returns success with translated text`() = runTest {
        coEvery { apiService.translate("Hello", "en", "ur") } returns
            NetworkResult.Success(TranslateResponseDto("ہیلو", "en", "ur"))

        val result = useCase("Hello", "en", "ur")

        assertTrue(result is NetworkResult.Success)
        assertEquals("ہیلو", (result as NetworkResult.Success).data.translatedText)
    }

    @Test
    fun `returns error when text is blank`() = runTest {
        val result = useCase("   ", "en", "ur")
        assertTrue(result is NetworkResult.Error)
    }

    @Test
    fun `returns api error when api fails`() = runTest {
        coEvery { apiService.translate(any(), any(), any()) } returns
            NetworkResult.Error("Network error")

        val result = useCase("A table for two, please.", "en", "ur")
        assertTrue(result is NetworkResult.Error)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:testDebugUnitTest \
  --tests "com.android.zubanx.domain.usecase.phrases.TranslatePhraseUseCaseTest" 2>&1 | tail -20
```
Expected: FAIL — `TranslatePhraseUseCase` not found

- [ ] **Step 3: Implement the use case**

```kotlin
// app/src/main/java/com/android/zubanx/domain/usecase/phrases/TranslatePhraseUseCase.kt
package com.android.zubanx.domain.usecase.phrases

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.api.TranslateApiService
import com.android.zubanx.data.remote.dto.TranslateResponseDto

class TranslatePhraseUseCase(
    private val apiService: TranslateApiService
) {
    suspend operator fun invoke(
        text: String,
        sourceLang: String,
        targetLang: String
    ): NetworkResult<TranslateResponseDto> {
        if (text.isBlank()) return NetworkResult.Error("Text must not be blank")
        return apiService.translate(text.trim(), sourceLang, targetLang)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:testDebugUnitTest \
  --tests "com.android.zubanx.domain.usecase.phrases.TranslatePhraseUseCaseTest" 2>&1 | tail -10
```
Expected: 3 tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/android/zubanx/domain/usecase/phrases/TranslatePhraseUseCase.kt \
        app/src/test/java/com/android/zubanx/domain/usecase/phrases/TranslatePhraseUseCaseTest.kt
git commit -m "feat(phrases): add TranslatePhraseUseCase"
```

---

## Task 3: PhrasesCategoryViewModel (TDD)

**Files:**
- Create: `app/src/main/java/com/android/zubanx/feature/phrases/PhrasesCategoryContract.kt`
- Create: `app/src/main/java/com/android/zubanx/feature/phrases/PhrasesCategoryViewModel.kt`
- Create: `app/src/test/java/com/android/zubanx/feature/phrases/PhrasesCategoryViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// app/src/test/java/com/android/zubanx/feature/phrases/PhrasesCategoryViewModelTest.kt
package com.android.zubanx.feature.phrases

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.TranslateResponseDto
import com.android.zubanx.domain.usecase.phrases.TranslatePhraseUseCase
import com.android.zubanx.feature.phrases.data.PhrasesData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhrasesCategoryViewModelTest {

    private val translateUseCase: TranslatePhraseUseCase = mockk()
    private lateinit var viewModel: PhrasesCategoryViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = PhrasesCategoryViewModel(translateUseCase, "dining")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads dining category with English phrases`() = runTest {
        val state = viewModel.state.first() as PhrasesCategoryContract.State.Active
        assertEquals("dining", state.category.id)
        assertEquals(PhrasesData.phrases[state.category]!!.size, state.displayPhrases.size)
        assertNull(state.expandedIndex)
    }

    @Test
    fun `ExpandPhrase sets expandedIndex`() = runTest {
        viewModel.onEvent(PhrasesCategoryContract.Event.ExpandPhrase(2))
        val state = viewModel.state.first() as PhrasesCategoryContract.State.Active
        assertEquals(2, state.expandedIndex)
    }

    @Test
    fun `CollapsePhrase clears expandedIndex`() = runTest {
        viewModel.onEvent(PhrasesCategoryContract.Event.ExpandPhrase(2))
        viewModel.onEvent(PhrasesCategoryContract.Event.CollapsePhrase)
        val state = viewModel.state.first() as PhrasesCategoryContract.State.Active
        assertNull(state.expandedIndex)
    }

    @Test
    fun `SwapLanguages reverses source and target`() = runTest {
        val before = viewModel.state.first() as PhrasesCategoryContract.State.Active
        val srcBefore = before.langSource.code
        val tgtBefore = before.langTarget.code

        viewModel.onEvent(PhrasesCategoryContract.Event.SwapLanguages)

        val after = viewModel.state.first() as PhrasesCategoryContract.State.Active
        assertEquals(tgtBefore, after.langSource.code)
        assertEquals(srcBefore, after.langTarget.code)
        assertNull(after.expandedIndex)
    }

    @Test
    fun `ExpandPhrase with same index collapses it`() = runTest {
        viewModel.onEvent(PhrasesCategoryContract.Event.ExpandPhrase(1))
        viewModel.onEvent(PhrasesCategoryContract.Event.ExpandPhrase(1))
        val state = viewModel.state.first() as PhrasesCategoryContract.State.Active
        assertNull(state.expandedIndex)
    }

    @Test
    fun `translation result is cached and stored`() = runTest {
        coEvery { translateUseCase(any(), "en", "ur") } returns
            NetworkResult.Success(TranslateResponseDto("ہیلو", "en", "ur"))

        viewModel.onEvent(PhrasesCategoryContract.Event.ExpandPhrase(0))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.first() as PhrasesCategoryContract.State.Active
        assertTrue(state.translationCache.containsKey("en:ur:0"))
        assertEquals(0, state.loadingIndices.size)
    }

    @Test
    fun `failed translation adds index to errorIndices`() = runTest {
        coEvery { translateUseCase(any(), "en", "ur") } returns
            NetworkResult.Error("Network error")

        viewModel.onEvent(PhrasesCategoryContract.Event.ExpandPhrase(0))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.first() as PhrasesCategoryContract.State.Active
        assertTrue(state.errorIndices.contains(0))
        assertEquals(0, state.loadingIndices.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:testDebugUnitTest \
  --tests "com.android.zubanx.feature.phrases.PhrasesCategoryViewModelTest" 2>&1 | tail -20
```
Expected: FAIL — classes not found

- [ ] **Step 3: Implement `PhrasesCategoryContract.kt`**

```kotlin
// app/src/main/java/com/android/zubanx/feature/phrases/PhrasesCategoryContract.kt
package com.android.zubanx.feature.phrases

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState
import com.android.zubanx.feature.phrases.data.PhraseCategory
import com.android.zubanx.feature.translate.LanguageItem

object PhrasesCategoryContract {

    sealed interface State : UiState {
        data class Active(
            val category: PhraseCategory,
            val displayPhrases: List<String>,           // phrases in source language
            val langSource: LanguageItem = LanguageItem.fromCode("en"),
            val langTarget: LanguageItem = LanguageItem.fromCode("ur"),
            val expandedIndex: Int? = null,
            val translationCache: Map<String, String> = emptyMap(), // key: "$src:$tgt:$index"
            val loadingIndices: Set<Int> = emptySet(),
            val errorIndices: Set<Int> = emptySet()
        ) : State
    }

    sealed class Event : UiEvent {
        data class ExpandPhrase(val index: Int) : Event()
        data object CollapsePhrase : Event()
        data class LangSourceSelected(val lang: LanguageItem) : Event()
        data class LangTargetSelected(val lang: LanguageItem) : Event()
        data object SwapLanguages : Event()
        data class RetryTranslation(val index: Int) : Event()
    }

    sealed class Effect : UiEffect {
        data class SpeakText(val text: String, val langCode: String) : Effect()
        data class CopyToClipboard(val text: String) : Effect()
        data class ShowToast(val message: String) : Effect()
        data class NavigateToZoom(val translatedText: String, val langCode: String) : Effect()
    }
}
```

- [ ] **Step 4: Implement `PhrasesCategoryViewModel.kt`**

```kotlin
// app/src/main/java/com/android/zubanx/feature/phrases/PhrasesCategoryViewModel.kt
package com.android.zubanx.feature.phrases

import androidx.lifecycle.viewModelScope
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.domain.usecase.phrases.TranslatePhraseUseCase
import com.android.zubanx.feature.phrases.data.PhrasesData
import com.android.zubanx.feature.translate.LanguageItem
import kotlinx.coroutines.launch

class PhrasesCategoryViewModel(
    private val translateUseCase: TranslatePhraseUseCase,
    categoryId: String
) : BaseViewModel<PhrasesCategoryContract.State, PhrasesCategoryContract.Event, PhrasesCategoryContract.Effect>(
    run {
        val category = PhrasesData.categoryById(categoryId)
        PhrasesCategoryContract.State.Active(
            category = category,
            displayPhrases = PhrasesData.phrases[category] ?: emptyList()
        )
    }
) {

    override fun onEvent(event: PhrasesCategoryContract.Event) {
        when (event) {
            is PhrasesCategoryContract.Event.ExpandPhrase -> handleExpand(event.index)
            is PhrasesCategoryContract.Event.CollapsePhrase -> collapse()
            is PhrasesCategoryContract.Event.LangSourceSelected -> changeSource(event.lang)
            is PhrasesCategoryContract.Event.LangTargetSelected -> changeTarget(event.lang)
            is PhrasesCategoryContract.Event.SwapLanguages -> swapLanguages()
            is PhrasesCategoryContract.Event.RetryTranslation -> retryTranslation(event.index)
        }
    }

    private val activeState get() = state.value as? PhrasesCategoryContract.State.Active

    private fun handleExpand(index: Int) {
        val s = activeState ?: return
        if (s.expandedIndex == index) {
            setState { (this as PhrasesCategoryContract.State.Active).copy(expandedIndex = null) }
            return
        }
        setState { (this as PhrasesCategoryContract.State.Active).copy(expandedIndex = index) }
        triggerTranslationIfNeeded(index)
    }

    private fun collapse() {
        setState { (this as PhrasesCategoryContract.State.Active).copy(expandedIndex = null) }
    }

    private fun changeSource(lang: LanguageItem) {
        setState {
            (this as PhrasesCategoryContract.State.Active).copy(
                langSource = lang,
                expandedIndex = null
            )
        }
        refreshDisplayPhrases()
    }

    private fun changeTarget(lang: LanguageItem) {
        setState {
            (this as PhrasesCategoryContract.State.Active).copy(
                langTarget = lang,
                expandedIndex = null
            )
        }
    }

    private fun swapLanguages() {
        val s = activeState ?: return
        setState {
            (this as PhrasesCategoryContract.State.Active).copy(
                langSource = s.langTarget,
                langTarget = s.langSource,
                expandedIndex = null
            )
        }
        refreshDisplayPhrases()
    }

    private fun refreshDisplayPhrases() {
        val s = activeState ?: return
        val basePhrases = PhrasesData.phrases[s.category] ?: return
        if (s.langSource.code == "en") {
            setState { (this as PhrasesCategoryContract.State.Active).copy(displayPhrases = basePhrases) }
            return
        }
        // Translate each base phrase to source language
        basePhrases.forEachIndexed { index, phrase ->
            val cacheKey = "en:${s.langSource.code}:$index"
            if (s.translationCache.containsKey(cacheKey)) {
                val cached = s.translationCache.toMutableMap()
                val updatedDisplay = (activeState?.displayPhrases?.toMutableList() ?: mutableListOf()).also {
                    if (index < it.size) it[index] = cached[cacheKey]!!
                    else it.add(cached[cacheKey]!!)
                }
                setState { (this as PhrasesCategoryContract.State.Active).copy(displayPhrases = updatedDisplay) }
            } else {
                viewModelScope.launch {
                    when (val result = translateUseCase(phrase, "en", s.langSource.code)) {
                        is NetworkResult.Success -> {
                            val translated = result.data.translatedText
                            setState {
                                val active = this as PhrasesCategoryContract.State.Active
                                val updatedDisplay = active.displayPhrases.toMutableList().also {
                                    if (index < it.size) it[index] = translated
                                }
                                active.copy(
                                    displayPhrases = updatedDisplay,
                                    translationCache = active.translationCache + (cacheKey to translated)
                                )
                            }
                        }
                        is NetworkResult.Error -> { /* keep English fallback */ }
                    }
                }
            }
        }
    }

    private fun triggerTranslationIfNeeded(index: Int) {
        val s = activeState ?: return
        val cacheKey = "${s.langSource.code}:${s.langTarget.code}:$index"
        if (s.translationCache.containsKey(cacheKey)) return
        if (s.loadingIndices.contains(index)) return

        val phraseToTranslate = s.displayPhrases.getOrNull(index) ?: return

        setState {
            (this as PhrasesCategoryContract.State.Active).copy(
                loadingIndices = loadingIndices + index,
                errorIndices = errorIndices - index
            )
        }

        viewModelScope.launch {
            when (val result = translateUseCase(phraseToTranslate, s.langSource.code, s.langTarget.code)) {
                is NetworkResult.Success -> {
                    val translated = result.data.translatedText
                    setState {
                        (this as PhrasesCategoryContract.State.Active).copy(
                            translationCache = translationCache + (cacheKey to translated),
                            loadingIndices = loadingIndices - index
                        )
                    }
                }
                is NetworkResult.Error -> {
                    setState {
                        (this as PhrasesCategoryContract.State.Active).copy(
                            loadingIndices = loadingIndices - index,
                            errorIndices = errorIndices + index
                        )
                    }
                }
            }
        }
    }

    private fun retryTranslation(index: Int) {
        setState {
            (this as PhrasesCategoryContract.State.Active).copy(errorIndices = errorIndices - index)
        }
        triggerTranslationIfNeeded(index)
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:testDebugUnitTest \
  --tests "com.android.zubanx.feature.phrases.PhrasesCategoryViewModelTest" 2>&1 | tail -10
```
Expected: 7 tests PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/android/zubanx/feature/phrases/PhrasesCategoryContract.kt \
        app/src/main/java/com/android/zubanx/feature/phrases/PhrasesCategoryViewModel.kt \
        app/src/test/java/com/android/zubanx/feature/phrases/PhrasesCategoryViewModelTest.kt
git commit -m "feat(phrases): add PhrasesCategoryContract and PhrasesCategoryViewModel"
```

---

## Task 4: PhrasesViewModel + PhrasesContract

**Files:**
- Create: `app/src/main/java/com/android/zubanx/feature/phrases/PhrasesContract.kt`
- Create: `app/src/main/java/com/android/zubanx/feature/phrases/PhrasesViewModel.kt`

- [ ] **Step 1: Create `PhrasesContract.kt`**

```kotlin
// app/src/main/java/com/android/zubanx/feature/phrases/PhrasesContract.kt
package com.android.zubanx.feature.phrases

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState
import com.android.zubanx.feature.phrases.data.PhraseCategory

object PhrasesContract {

    sealed interface State : UiState {
        data class Active(
            val categories: List<PhraseCategory>
        ) : State
    }

    sealed class Event : UiEvent {
        data class CategorySelected(val category: PhraseCategory) : Event()
    }

    sealed class Effect : UiEffect {
        data class NavigateToCategory(val categoryId: String) : Effect()
    }
}
```

- [ ] **Step 2: Create `PhrasesViewModel.kt`**

```kotlin
// app/src/main/java/com/android/zubanx/feature/phrases/PhrasesViewModel.kt
package com.android.zubanx.feature.phrases

import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.feature.phrases.data.PhrasesData

class PhrasesViewModel : BaseViewModel<PhrasesContract.State, PhrasesContract.Event, PhrasesContract.Effect>(
    PhrasesContract.State.Active(categories = PhrasesData.categories)
) {
    override fun onEvent(event: PhrasesContract.Event) {
        when (event) {
            is PhrasesContract.Event.CategorySelected ->
                sendEffect(PhrasesContract.Effect.NavigateToCategory(event.category.id))
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/android/zubanx/feature/phrases/PhrasesContract.kt \
        app/src/main/java/com/android/zubanx/feature/phrases/PhrasesViewModel.kt
git commit -m "feat(phrases): add PhrasesContract and PhrasesViewModel"
```

---

## Task 5: Drawable Resources

**Files:**
- Create: 11 drawable XMLs (1 nav icon + 10 category icons)

- [ ] **Step 1: Create `ic_nav_phrases.xml`**

```xml
<!-- app/src/main/res/drawable/ic_nav_phrases.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M4,6h16v2H4zm0,5h16v2H4zm0,5h16v2H4z"/>
</vector>
```

- [ ] **Step 2: Create the 10 category icon drawables**

```xml
<!-- app/src/main/res/drawable/ic_category_dining.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/white"
        android:pathData="M11,9H9V2H7v7H5V2H3v7c0,2.12 1.66,3.84 3.75,3.97V22h2.5v-9.03C11.34,12.84 13,11.12 13,9V2h-2v7zM16,6v8h2.5v8H21V2c-2.76,0 -5,2.24 -5,4z"/>
</vector>

<!-- app/src/main/res/drawable/ic_category_emergency.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/white"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13,17h-2v-2h2v2zM13,13h-2V7h2v6z"/>
</vector>

<!-- app/src/main/res/drawable/ic_category_travel.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/white"
        android:pathData="M21,16v-2l-8,-5V3.5c0,-0.83 -0.67,-1.5 -1.5,-1.5S10,2.67 10,3.5V9l-8,5v2l8,-2.5V19l-2,1.5V22l3.5,-1 3.5,1v-1.5L13,19v-5.5l8,2.5z"/>
</vector>

<!-- app/src/main/res/drawable/ic_category_greeting.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/white"
        android:pathData="M7.5,13C7.5,11 9,9.5 11,9.5c0.35,0 0.68,0.05 1,0.14V7.5C11.67,7.19 11.35,7 11,7c-2.76,0 -5,2.24 -5,5s2.24,5 5,5c0.35,0 0.67,-0.05 1,-0.12V14.3C11.68,14.44 11.35,14.5 11,14.5 9,14.5 7.5,13 7.5,13zM16,7l-1,1 1,1 1,-1zM18,5l-1,1 1,1 1,-1zM20,9l-1,-1 -1,1 1,1zM13,10v4l3,3 1,-1 -2.5,-2.5V10z"/>
</vector>

<!-- app/src/main/res/drawable/ic_category_shopping.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/white"
        android:pathData="M18,6h-2c0,-2.21 -1.79,-4 -4,-4S8,3.79 8,6H6c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2zM12,4c1.1,0 2,0.9 2,2h-4c0,-1.1 0.9,-2 2,-2zM18,20H6V8h2v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1V8h4v2c0,0.55 0.45,1 1,1s1,-0.45 1,-1V8h2v12z"/>
</vector>

<!-- app/src/main/res/drawable/ic_category_hotel.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/white"
        android:pathData="M7,13c1.66,0 3,-1.34 3,-3S8.66,7 7,7s-3,1.34 -3,3 1.34,3 3,3zM19,7h-8v7H3V5H1v15h2v-3h18v3h2v-9c0,-2.21 -1.79,-4 -4,-4z"/>
</vector>

<!-- app/src/main/res/drawable/ic_category_office.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/white"
        android:pathData="M20,6h-2.18c0.07,-0.44 0.18,-0.86 0.18,-1.3C18,2.12 15.88,0 13.3,0c-1.42,0 -2.68,0.62 -3.56,1.6L8,3.4 6.26,1.6C5.38,0.62 4.12,0 2.7,0 1.12,0 0,1.12 0,2.7c0,0.44 0.11,0.86 0.18,1.3H-2v16h24V6zM13.3,2c0.83,0 1.5,0.67 1.5,1.5S14.13,5 13.3,5c-0.83,0 -1.5,-0.67 -1.5,-1.5S12.47,2 13.3,2zM2.7,2c0.83,0 1.5,0.67 1.5,1.5S3.53,5 2.7,5C1.87,5 1.2,4.33 1.2,3.5S1.87,2 2.7,2zM11,20H2V8h9v12zM22,20h-9V8h9v12z"/>
</vector>

<!-- app/src/main/res/drawable/ic_category_trouble.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/white"
        android:pathData="M1,21h22L12,2 1,21zM13,18h-2v-2h2v2zM13,14h-2v-4h2v4z"/>
</vector>

<!-- app/src/main/res/drawable/ic_category_entertainment.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/white"
        android:pathData="M18,4l2,4h-3l-2,-4h-2l2,4h-3l-2,-4H8l2,4H7L5,4H4c-1.1,0 -1.99,0.9 -1.99,2L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V4h-4z"/>
</vector>

<!-- app/src/main/res/drawable/ic_category_medicine.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="@android:color/white"
        android:pathData="M20,6h-4V4c0,-1.1 -0.9,-2 -2,-2h-4c-1.1,0 -2,0.9 -2,2v2H4c-1.1,0 -2,0.9 -2,2v12c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V8c0,-1.1 -0.9,-2 -2,-2zM10,4h4v2h-4V4zM15,15h-2v2c0,0.55 -0.45,1 -1,1s-1,-0.45 -1,-1v-2H9c-0.55,0 -1,-0.45 -1,-1s0.45,-1 1,-1h2v-2c0,-0.55 0.45,-1 1,-1s1,0.45 1,1v2h2c0.55,0 1,0.45 1,1s-0.45,1 -1,1z"/>
</vector>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/drawable/ic_nav_phrases.xml \
        app/src/main/res/drawable/ic_category_*.xml
git commit -m "feat(phrases): add category and nav drawable resources"
```

---

## Task 6: Layouts

**Files:**
- Create: `app/src/main/res/layout/fragment_phrases.xml`
- Create: `app/src/main/res/layout/fragment_phrases_category.xml`
- Create: `app/src/main/res/layout/fragment_phrases_zoom.xml`
- Create: `app/src/main/res/layout/item_phrase_category.xml`
- Create: `app/src/main/res/layout/item_phrase.xml`

- [ ] **Step 1: Create `fragment_phrases.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvCategories"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:padding="12dp"
        android:clipToPadding="false" />

</FrameLayout>
```

- [ ] **Step 2: Create `item_phrase_category.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/cardCategory"
    android:layout_width="match_parent"
    android:layout_height="100dp"
    android:layout_margin="6dp"
    app:cardElevation="2dp"
    app:cardCornerRadius="12dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:gravity="center"
        android:padding="8dp">

        <ImageView
            android:id="@+id/ivCategoryIcon"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:contentDescription="@null" />

        <TextView
            android:id="@+id/tvCategoryName"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="6dp"
            android:textAppearance="?attr/textAppearanceLabelSmall"
            android:gravity="center"
            android:maxLines="1" />

    </LinearLayout>

</com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 3: Create `fragment_phrases_category.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize" />

    <!-- Language bar -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingStart="4dp"
        android:paddingEnd="4dp"
        android:background="?attr/colorSurface">

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnSourceLang"
            style="@style/Widget.Material3.Button.TextButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="English" />

        <ImageButton
            android:id="@+id/btnSwapLang"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:src="@drawable/ic_swap"
            android:contentDescription="Swap languages" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnTargetLang"
            style="@style/Widget.Material3.Button.TextButton"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="Urdu" />

    </LinearLayout>

    <com.google.android.material.divider.MaterialDivider
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvPhrases"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:padding="8dp"
        android:clipToPadding="false" />

</LinearLayout>
```

- [ ] **Step 4: Create `item_phrase.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical">

    <!-- Collapsed row -->
    <LinearLayout
        android:id="@+id/rowCollapsed"
        android:layout_width="match_parent"
        android:layout_height="48dp"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingStart="16dp"
        android:paddingEnd="8dp">

        <TextView
            android:id="@+id/tvPhrase"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:textAppearance="?attr/textAppearanceBodyMedium" />

        <ImageButton
            android:id="@+id/btnExpand"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:src="@drawable/ic_expand_more"
            android:contentDescription="Expand" />

    </LinearLayout>

    <com.google.android.material.divider.MaterialDivider
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:marginStart="16dp" />

    <!-- Expanded content (GONE by default) -->
    <LinearLayout
        android:id="@+id/layoutExpanded"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingStart="16dp"
        android:paddingEnd="16dp"
        android:paddingBottom="12dp"
        android:visibility="gone">

        <!-- Loading -->
        <ProgressBar
            android:id="@+id/progressTranslation"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center_horizontal"
            android:visibility="gone" />

        <!-- Translated text -->
        <TextView
            android:id="@+id/tvTranslated"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textAppearance="?attr/textAppearanceBodyMedium"
            android:paddingTop="4dp"
            android:paddingBottom="8dp"
            android:visibility="gone" />

        <!-- Error row -->
        <LinearLayout
            android:id="@+id/layoutError"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:visibility="gone">

            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Translation failed"
                android:textAppearance="?attr/textAppearanceBodySmall" />

            <ImageButton
                android:id="@+id/btnRetry"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:src="@drawable/ic_refresh"
                android:contentDescription="Retry" />

        </LinearLayout>

        <!-- Action icons -->
        <LinearLayout
            android:id="@+id/layoutActions"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:visibility="gone">

            <ImageButton
                android:id="@+id/btnSpeak"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:src="@drawable/ic_volume_up"
                android:contentDescription="Speak" />

            <ImageButton
                android:id="@+id/btnCopy"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:src="@drawable/ic_copy"
                android:contentDescription="Copy" />

            <ImageButton
                android:id="@+id/btnZoom"
                android:layout_width="40dp"
                android:layout_height="40dp"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:src="@drawable/ic_zoom_in"
                android:contentDescription="Zoom" />

        </LinearLayout>

    </LinearLayout>

</LinearLayout>
```

**Note:** `ic_swap`, `ic_expand_more`, `ic_volume_up`, `ic_copy`, `ic_zoom_in`, `ic_refresh` are existing drawables in the project used by other features. Verify with:
```bash
ls app/src/main/res/drawable/ic_swap.xml \
   app/src/main/res/drawable/ic_expand_more.xml \
   app/src/main/res/drawable/ic_volume_up.xml \
   app/src/main/res/drawable/ic_copy.xml \
   app/src/main/res/drawable/ic_zoom_in.xml \
   app/src/main/res/drawable/ic_refresh.xml 2>&1
```
Create any that are missing as 24dp Material-style vectors.

- [ ] **Step 5: Create `fragment_phrases_zoom.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize" />

    <TextView
        android:id="@+id/tvZoomText"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:gravity="center"
        android:padding="24dp"
        android:textAppearance="?attr/textAppearanceDisplaySmall" />

    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnSpeak"
        style="@style/Widget.Material3.Button.FilledButton"
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:layout_margin="16dp"
        android:text="Speak"
        app:icon="@drawable/ic_volume_up"
        xmlns:app="http://schemas.android.com/apk/res-auto" />

</LinearLayout>
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/layout/fragment_phrases.xml \
        app/src/main/res/layout/fragment_phrases_category.xml \
        app/src/main/res/layout/fragment_phrases_zoom.xml \
        app/src/main/res/layout/item_phrase_category.xml \
        app/src/main/res/layout/item_phrase.xml
git commit -m "feat(phrases): add phrases layouts"
```

---

## Task 7: PhrasesFragment + PhrasesCategoryAdapter + PhrasesCategoryFragment + PhrasesZoomFragment

**Files:**
- Create: `app/src/main/java/com/android/zubanx/feature/phrases/PhrasesFragment.kt`
- Create: `app/src/main/java/com/android/zubanx/feature/phrases/PhrasesCategoryAdapter.kt`
- Create: `app/src/main/java/com/android/zubanx/feature/phrases/PhrasesCategoryFragment.kt`
- Create: `app/src/main/java/com/android/zubanx/feature/phrases/PhrasesZoomFragment.kt`

- [ ] **Step 1: Create `PhrasesFragment.kt`**

```kotlin
// app/src/main/java/com/android/zubanx/feature/phrases/PhrasesFragment.kt
package com.android.zubanx.feature.phrases

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.databinding.FragmentPhrasesBinding
import com.android.zubanx.databinding.ItemPhraseCategoryBinding
import com.android.zubanx.feature.phrases.data.PhraseCategory
import org.koin.androidx.viewmodel.ext.android.viewModel

class PhrasesFragment : BaseFragment<FragmentPhrasesBinding>(FragmentPhrasesBinding::inflate) {

    private val viewModel: PhrasesViewModel by viewModel()

    private val adapter = CategoryAdapter { category ->
        viewModel.onEvent(PhrasesContract.Event.CategorySelected(category))
    }

    override fun setupViews() {
        binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvCategories.adapter = adapter
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            if (state is PhrasesContract.State.Active) {
                adapter.submitList(state.categories)
            }
        }
        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is PhrasesContract.Effect.NavigateToCategory ->
                    findNavController().navigate(
                        PhrasesFragmentDirections.actionPhrasesToCategory(effect.categoryId)
                    )
            }
        }
    }

    class CategoryAdapter(
        private val onClick: (PhraseCategory) -> Unit
    ) : ListAdapter<PhraseCategory, CategoryAdapter.VH>(DIFF) {

        inner class VH(val b: ItemPhraseCategoryBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemPhraseCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val cat = getItem(position)
            holder.b.ivCategoryIcon.setImageResource(cat.iconRes)
            holder.b.tvCategoryName.text = cat.displayName
            holder.b.cardCategory.setOnClickListener { onClick(cat) }
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<PhraseCategory>() {
                override fun areItemsTheSame(a: PhraseCategory, b: PhraseCategory) = a.id == b.id
                override fun areContentsTheSame(a: PhraseCategory, b: PhraseCategory) = a.id == b.id
            }
        }
    }
}
```

- [ ] **Step 2: Create `PhrasesCategoryAdapter.kt`**

```kotlin
// app/src/main/java/com/android/zubanx/feature/phrases/PhrasesCategoryAdapter.kt
package com.android.zubanx.feature.phrases

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.databinding.ItemPhraseBinding

data class PhraseItem(
    val index: Int,
    val displayText: String,
    val translatedText: String?,
    val isExpanded: Boolean,
    val isLoading: Boolean,
    val isError: Boolean
)

class PhrasesCategoryAdapter(
    private val onExpand: (Int) -> Unit,
    private val onSpeak: (String, String) -> Unit,   // text, langCode
    private val onCopy: (String) -> Unit,
    private val onZoom: (String, String) -> Unit,    // text, langCode
    private val onRetry: (Int) -> Unit
) : ListAdapter<PhraseItem, PhrasesCategoryAdapter.VH>(DIFF) {

    private var targetLangCode: String = "ur"

    fun updateTargetLang(code: String) {
        targetLangCode = code
    }

    inner class VH(val b: ItemPhraseBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemPhraseBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        holder.b.tvPhrase.text = item.displayText

        // Collapsed/expanded toggle
        holder.b.rowCollapsed.setOnClickListener { onExpand(item.index) }
        holder.b.btnExpand.setOnClickListener { onExpand(item.index) }

        holder.b.layoutExpanded.isVisible = item.isExpanded
        holder.b.progressTranslation.isVisible = item.isExpanded && item.isLoading
        holder.b.tvTranslated.isVisible = item.isExpanded && !item.isLoading && !item.isError && item.translatedText != null
        holder.b.layoutError.isVisible = item.isExpanded && item.isError
        holder.b.layoutActions.isVisible = item.isExpanded && !item.isLoading && !item.isError && item.translatedText != null

        item.translatedText?.let { translated ->
            holder.b.tvTranslated.text = translated
            holder.b.btnSpeak.setOnClickListener { onSpeak(translated, targetLangCode) }
            holder.b.btnCopy.setOnClickListener { onCopy(translated) }
            holder.b.btnZoom.setOnClickListener { onZoom(translated, targetLangCode) }
        }
        holder.b.btnRetry.setOnClickListener { onRetry(item.index) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<PhraseItem>() {
            override fun areItemsTheSame(a: PhraseItem, b: PhraseItem) = a.index == b.index
            override fun areContentsTheSame(a: PhraseItem, b: PhraseItem) = a == b
        }
    }
}
```

- [ ] **Step 3: Create `PhrasesCategoryFragment.kt`**

```kotlin
// app/src/main/java/com/android/zubanx/feature/phrases/PhrasesCategoryFragment.kt
package com.android.zubanx.feature.phrases

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.core.utils.toast
import com.android.zubanx.databinding.FragmentPhrasesCategoryBinding
import com.android.zubanx.feature.translate.LanguageItem
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class PhrasesCategoryFragment : BaseFragment<FragmentPhrasesCategoryBinding>(
    FragmentPhrasesCategoryBinding::inflate
) {

    private val args: PhrasesCategoryFragmentArgs by navArgs()

    private val viewModel: PhrasesCategoryViewModel by viewModel {
        parametersOf(args.categoryId)
    }

    private val adapter = PhrasesCategoryAdapter(
        onExpand = { index -> viewModel.onEvent(PhrasesCategoryContract.Event.ExpandPhrase(index)) },
        onSpeak = { text, lang -> viewModel.onEvent(PhrasesCategoryContract.Event.ExpandPhrase(-1)).also {
            // Effect handled in observeState
        }; /* direct effect */
            viewModel.sendSpeakEffect(text, lang)
        },
        onCopy = { text ->
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("phrase", text))
            requireContext().toast("Copied")
        },
        onZoom = { text, lang ->
            findNavController().navigate(
                PhrasesCategoryFragmentDirections.actionCategoryToZoom(text, lang)
            )
        },
        onRetry = { index -> viewModel.onEvent(PhrasesCategoryContract.Event.RetryTranslation(index)) }
    )

    override fun setupViews() {
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.rvPhrases.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPhrases.adapter = adapter

        binding.btnSourceLang.setOnClickListener { showLanguagePicker(isSource = true) }
        binding.btnTargetLang.setOnClickListener { showLanguagePicker(isSource = false) }
        binding.btnSwapLang.setOnClickListener { viewModel.onEvent(PhrasesCategoryContract.Event.SwapLanguages) }
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            if (state is PhrasesCategoryContract.State.Active) {
                binding.toolbar.title = state.category.displayName
                binding.btnSourceLang.text = state.langSource.name
                binding.btnTargetLang.text = state.langTarget.name
                adapter.updateTargetLang(state.langTarget.code)

                val items = state.displayPhrases.mapIndexed { index, phrase ->
                    PhraseItem(
                        index = index,
                        displayText = phrase,
                        translatedText = state.translationCache["${state.langSource.code}:${state.langTarget.code}:$index"],
                        isExpanded = state.expandedIndex == index,
                        isLoading = state.loadingIndices.contains(index),
                        isError = state.errorIndices.contains(index)
                    )
                }
                adapter.submitList(items)
            }
        }
        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is PhrasesCategoryContract.Effect.SpeakText ->
                    requireContext().toast("Speaking: ${effect.text}")  // TTS stub — replaced in full TTS integration
                is PhrasesCategoryContract.Effect.CopyToClipboard -> {
                    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("phrase", effect.text))
                    requireContext().toast("Copied")
                }
                is PhrasesCategoryContract.Effect.ShowToast ->
                    requireContext().toast(effect.message)
                is PhrasesCategoryContract.Effect.NavigateToZoom ->
                    findNavController().navigate(
                        PhrasesCategoryFragmentDirections.actionCategoryToZoom(effect.translatedText, effect.langCode)
                    )
            }
        }
    }

    private fun showLanguagePicker(isSource: Boolean) {
        val languages = LanguageItem.ALL.filter { it != LanguageItem.DETECT }
        val names = languages.map { it.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Select language")
            .setItems(names) { _, which ->
                val selected = languages[which]
                viewModel.onEvent(
                    if (isSource) PhrasesCategoryContract.Event.LangSourceSelected(selected)
                    else PhrasesCategoryContract.Event.LangTargetSelected(selected)
                )
            }
            .show()
    }
}
```

**Note:** The adapter's `onSpeak` callback above uses a placeholder. Simplify by having the adapter call the ViewModel effect directly via the fragment. Replace the `onSpeak` and `onCopy` lambdas in `setupViews` to use effects:

```kotlin
onSpeak = { text, lang ->
    viewModel.onEvent(PhrasesCategoryContract.Event.ExpandPhrase(
        (viewModel.state.value as? PhrasesCategoryContract.State.Active)?.expandedIndex ?: return@PhrasesCategoryAdapter
    ))
    // Fire speak effect directly:
    findNavController() // no-op placeholder
    requireContext().toast("Speaking: $text") // stub until TTS wired
},
```

Actually, simplify: remove `onSpeak`/`onCopy` from the adapter and handle them via the fragment collecting effects. Update the adapter to emit callbacks that the fragment routes to ViewModel events:

**Final simplified adapter callbacks:**
- `onSpeak(index)` → fragment calls `viewModel.onEvent(SpeakPhrase(index))` — add this event to the contract
- `onCopy(index)` → fragment calls `viewModel.onEvent(CopyPhrase(index))` — add this event
- Keep `onZoom` in the adapter pointing directly to `findNavController().navigate(...)`

Add to `PhrasesCategoryContract.Event`:
```kotlin
data class SpeakPhrase(val index: Int) : Event()
data class CopyPhrase(val index: Int) : Event()
```

Add to `PhrasesCategoryViewModel.onEvent`:
```kotlin
is PhrasesCategoryContract.Event.SpeakPhrase -> {
    val s = activeState ?: return
    val key = "${s.langSource.code}:${s.langTarget.code}:${event.index}"
    val text = s.translationCache[key] ?: return
    sendEffect(PhrasesCategoryContract.Effect.SpeakText(text, s.langTarget.code))
}
is PhrasesCategoryContract.Event.CopyPhrase -> {
    val s = activeState ?: return
    val key = "${s.langSource.code}:${s.langTarget.code}:${event.index}"
    val text = s.translationCache[key] ?: return
    sendEffect(PhrasesCategoryContract.Effect.CopyToClipboard(text))
}
```

Update `PhrasesCategoryAdapter` to use `onSpeak: (Int) -> Unit` and `onCopy: (Int) -> Unit`.

- [ ] **Step 4: Create `PhrasesZoomFragment.kt`**

```kotlin
// app/src/main/java/com/android/zubanx/feature/phrases/PhrasesZoomFragment.kt
package com.android.zubanx.feature.phrases

import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.tts.TtsManager
import com.android.zubanx.databinding.FragmentPhrasesZoomBinding
import org.koin.android.ext.android.inject

class PhrasesZoomFragment : BaseFragment<FragmentPhrasesZoomBinding>(
    FragmentPhrasesZoomBinding::inflate
) {

    private val args: PhrasesZoomFragmentArgs by navArgs()
    private val ttsManager: TtsManager by inject()

    override fun setupViews() {
        (activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        binding.tvZoomText.text = args.translatedText
        binding.btnSpeak.setOnClickListener {
            ttsManager.speak(args.translatedText, args.langCode)
        }
    }
}
```

**Note:** Verify the `TtsManager` package — check its actual import path:
```bash
grep -r "class TtsManager" app/src/main/java/ | head -3
```
Update the import in `PhrasesZoomFragment.kt` to match.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/android/zubanx/feature/phrases/
git commit -m "feat(phrases): add PhrasesFragment, PhrasesCategoryAdapter, PhrasesCategoryFragment, PhrasesZoomFragment"
```

---

## Task 8: Navigation, DI, Bottom Nav, Translate App Bar

**Files:**
- Modify: `app/src/main/res/navigation/nav_phrases.xml`
- Modify: `app/src/main/res/menu/bottom_nav_menu.xml`
- Modify: `app/src/main/java/com/android/zubanx/core/di/UseCaseModule.kt`
- Modify: `app/src/main/java/com/android/zubanx/core/di/ViewModelModule.kt`
- Modify: `app/src/main/java/com/android/zubanx/app/MainActivity.kt`
- Modify: `app/src/main/res/layout/fragment_translate.xml`
- Modify: `app/src/main/java/com/android/zubanx/feature/translate/TranslateFragment.kt`

- [ ] **Step 1: Update `nav_phrases.xml`**

Replace the empty placeholder with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_phrases"
    android:label="Phrases"
    app:startDestination="@id/phrasesFragment">

    <fragment
        android:id="@+id/phrasesFragment"
        android:name="com.android.zubanx.feature.phrases.PhrasesFragment"
        android:label="Phrases">
        <action
            android:id="@+id/action_phrases_to_category"
            app:destination="@id/phrasesCategoryFragment" />
    </fragment>

    <fragment
        android:id="@+id/phrasesCategoryFragment"
        android:name="com.android.zubanx.feature.phrases.PhrasesCategoryFragment"
        android:label="Category">
        <argument
            android:name="categoryId"
            app:argType="string" />
        <action
            android:id="@+id/action_category_to_zoom"
            app:destination="@id/phrasesZoomFragment" />
    </fragment>

    <fragment
        android:id="@+id/phrasesZoomFragment"
        android:name="com.android.zubanx.feature.phrases.PhrasesZoomFragment"
        android:label="Zoom">
        <argument
            android:name="translatedText"
            app:argType="string" />
        <argument
            android:name="langCode"
            app:argType="string" />
    </fragment>

</navigation>
```

- [ ] **Step 2: Update `bottom_nav_menu.xml`**

Replace the Settings item with Phrases:

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">

    <item
        android:id="@+id/nav_translate"
        android:icon="@drawable/ic_nav_translate"
        android:title="Translate" />

    <item
        android:id="@+id/nav_conversation"
        android:icon="@drawable/ic_nav_conversation"
        android:title="Conversation" />

    <item
        android:id="@+id/nav_dictionary"
        android:icon="@drawable/ic_nav_dictionary"
        android:title="Dictionary" />

    <item
        android:id="@+id/nav_phrases"
        android:icon="@drawable/ic_nav_phrases"
        android:title="Phrases" />

    <item
        android:id="@+id/nav_favourite"
        android:icon="@drawable/ic_nav_favourite"
        android:title="Favourites" />

</menu>
```

- [ ] **Step 3: Update `UseCaseModule.kt`**

Add the import and factory entry:

```kotlin
import com.android.zubanx.domain.usecase.phrases.TranslatePhraseUseCase

// inside the module block, add:
factoryOf(::TranslatePhraseUseCase)
```

- [ ] **Step 4: Update `ViewModelModule.kt`**

Add imports and viewModel entries:

```kotlin
import com.android.zubanx.feature.phrases.PhrasesViewModel
import com.android.zubanx.feature.phrases.PhrasesCategoryViewModel

// inside the module block, add:
viewModelOf(::PhrasesViewModel)
viewModel { params -> PhrasesCategoryViewModel(get(), params.get()) }
```

Note: `PhrasesCategoryViewModel` takes a `categoryId: String` parameter passed at runtime, so use `viewModel { params -> ... }` instead of `viewModelOf`.

- [ ] **Step 5: Update `MainActivity.kt`**

Add `R.id.settingsFragment` to `BOTTOM_NAV_HIDDEN_DESTINATIONS`:

```kotlin
private val BOTTOM_NAV_HIDDEN_DESTINATIONS = setOf<Int>(
    R.id.splashFragment,
    R.id.onboardingFragment,
    R.id.settingsFragment,
)
```

- [ ] **Step 6: Add `AppBarLayout` + `MaterialToolbar` to `fragment_translate.xml`**

In `fragment_translate.xml`, the root is `CoordinatorLayout`. Add an `AppBarLayout` as the **first child** before the existing `langSelectorBar`:

```xml
<com.google.android.material.appbar.AppBarLayout
    android:id="@+id/appBarLayout"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <com.google.android.material.appbar.MaterialToolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        app:title="ZubanX"
        app:menu="@menu/menu_translate_toolbar" />

</com.google.android.material.appbar.AppBarLayout>
```

Also create the toolbar menu file `app/src/main/res/menu/menu_translate_toolbar.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <item
        android:id="@+id/action_premium"
        android:icon="@drawable/ic_premium"
        android:title="Premium"
        app:showAsAction="always" />

    <item
        android:id="@+id/action_settings"
        android:icon="@drawable/ic_settings"
        android:title="Settings"
        app:showAsAction="always" />

</menu>
```

**Note:** Verify `@drawable/ic_premium` and `@drawable/ic_settings` exist:
```bash
ls app/src/main/res/drawable/ic_premium.xml \
   app/src/main/res/drawable/ic_settings.xml 2>&1
```
Create any that are missing as 24dp Material-style vectors.

- [ ] **Step 7: Wire toolbar in `TranslateFragment.kt`**

In `TranslateFragment.setupViews()`, add:

```kotlin
(activity as? AppCompatActivity)?.setSupportActionBar(binding.toolbar)
binding.toolbar.setOnMenuItemClickListener { item ->
    when (item.itemId) {
        R.id.action_settings -> {
            findNavController().navigate(R.id.settingsFragment)
            true
        }
        R.id.action_premium -> {
            findNavController().navigate(R.id.premiumFragment)
            true
        }
        else -> false
    }
}
```

Add the necessary imports:
```kotlin
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
```

- [ ] **Step 8: Build and run all tests**

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:assembleDebug 2>&1 | tail -20
```
Expected: BUILD SUCCESSFUL

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew :app:testDebugUnitTest 2>&1 | tail -20
```
Expected: All tests PASS

- [ ] **Step 9: Commit**

```bash
git add app/src/main/res/navigation/nav_phrases.xml \
        app/src/main/res/menu/bottom_nav_menu.xml \
        app/src/main/res/menu/menu_translate_toolbar.xml \
        app/src/main/java/com/android/zubanx/core/di/UseCaseModule.kt \
        app/src/main/java/com/android/zubanx/core/di/ViewModelModule.kt \
        app/src/main/java/com/android/zubanx/app/MainActivity.kt \
        app/src/main/res/layout/fragment_translate.xml \
        app/src/main/java/com/android/zubanx/feature/translate/TranslateFragment.kt
git commit -m "feat(phrases): wire navigation, DI, bottom nav, and translate toolbar"
```

---

## Manual Verification Checklist

After all tasks are committed, install on device/emulator and verify:

- [ ] Bottom nav shows: Translate | Conversation | Dictionary | Phrases | Favourites (no Settings tab)
- [ ] Translate screen shows "ZubanX" title in app bar with settings and premium icons
- [ ] Tapping settings icon navigates to Settings screen; bottom nav is hidden there
- [ ] Tapping Phrases tab shows 3×3 category grid
- [ ] Tapping a category card navigates to detail screen with correct title
- [ ] Language bar shows "English" → "Urdu" defaults with swap button
- [ ] Tapping a phrase row expands it showing loading spinner, then translated text
- [ ] Expanded card shows speak, copy, zoom action icons
- [ ] Tapping copy shows "Copied" toast
- [ ] Tapping zoom navigates to full-screen zoom view with large translated text and speak button
- [ ] Tapping back from zoom returns to category detail
- [ ] Changing language re-translates uncached phrases; cached phrases show immediately
- [ ] Swapping languages reverses source/target and collapses any expanded item
- [ ] Network error shows "Translation failed" with retry icon; retry re-fetches

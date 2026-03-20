# Plan 7: Conversation Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a bilingual two-person conversation screen where Speaker A and Speaker B each speak in their own language, the app translates each turn and speaks the translation aloud.

**Architecture:** Session-only (no Room persistence) — conversation messages live in ViewModel state. Reuse `TranslateApiService` via a new `ConversationTranslateUseCase` that skips the translation history repository. MVI contract with a single `Active` state holding the message list and per-speaker language selection.

**Tech Stack:** Kotlin, MVI (BaseViewModel), Koin 4.1.1, Ktor (via TranslateApiService), RecyclerView + ListAdapter, Navigation Safe Args, ViewBinding, RecognizerIntent for mic input.

**Prerequisites confirmed:** `LanguageItem` has `code: String`, `name: String`, and companion members `DETECT`, `ALL`, `fromCode(code)`. `RECORD_AUDIO` permission is already in `AndroidManifest.xml`.

---

## File Map

| Action | Path | Responsibility |
|--------|------|----------------|
| Create | `domain/usecase/conversation/ConversationTranslateUseCase.kt` | Calls TranslateApiService without saving to history |
| Create | `feature/conversation/ConversationContract.kt` | State / Event / Effect sealed types + ConversationMessage data class |
| Create | `feature/conversation/ConversationViewModel.kt` | MVI ViewModel — translates mic results, drives TTS effects |
| Create | `feature/conversation/ConversationFragment.kt` | Two-panel UI, dual mic buttons, language pickers, RecyclerView chat |
| Create | `res/layout/fragment_conversation.xml` | Two-panel layout — Speaker A (top), Speaker B (bottom), message list in middle |
| Create | `res/layout/item_conversation_message.xml` | Chat bubble row — text aligned left (A) or right (B) |
| Modify | `res/navigation/nav_conversation.xml` | Add `conversationFragment` as start destination |
| Modify | `core/di/UseCaseModule.kt` | Register `ConversationTranslateUseCase` |
| Modify | `core/di/ViewModelModule.kt` | Register `ConversationViewModel` |

---

## Task 1: ConversationTranslateUseCase

**Files:**
- Create: `app/src/main/java/com/android/zubanx/domain/usecase/conversation/ConversationTranslateUseCase.kt`
- Create: `app/src/test/java/com/android/zubanx/domain/usecase/conversation/ConversationTranslateUseCaseTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// app/src/test/java/com/android/zubanx/domain/usecase/conversation/ConversationTranslateUseCaseTest.kt
package com.android.zubanx.domain.usecase.conversation

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.api.TranslateApiService
import com.android.zubanx.data.remote.dto.TranslateResponseDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationTranslateUseCaseTest {

    private val apiService: TranslateApiService = mockk()
    private val useCase = ConversationTranslateUseCase(apiService)

    @Test
    fun `returns success when api succeeds`() = runTest {
        coEvery { apiService.translate("hello", "en", "ur") } returns
            NetworkResult.Success(TranslateResponseDto("سلام", "en", "ur"))

        val result = useCase("hello", "en", "ur")

        assertTrue(result is NetworkResult.Success)
        assertEquals("سلام", (result as NetworkResult.Success).data.translatedText)
    }

    @Test
    fun `returns error when text is blank`() = runTest {
        val result = useCase("  ", "en", "ur")
        assertTrue(result is NetworkResult.Error)
    }

    @Test
    fun `returns api error when api fails`() = runTest {
        coEvery { apiService.translate(any(), any(), any()) } returns
            NetworkResult.Error("Network error")

        val result = useCase("hello", "en", "ur")
        assertTrue(result is NetworkResult.Error)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/nokhaiz/AndroidStudioProjects/ZubanXPakSoftSolution
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.domain.usecase.conversation.ConversationTranslateUseCaseTest" --info 2>&1 | tail -20
```
Expected: FAIL — `ConversationTranslateUseCase` not found

- [ ] **Step 3: Implement the use case**

```kotlin
// app/src/main/java/com/android/zubanx/domain/usecase/conversation/ConversationTranslateUseCase.kt
package com.android.zubanx.domain.usecase.conversation

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.api.TranslateApiService
import com.android.zubanx.data.remote.dto.TranslateResponseDto

class ConversationTranslateUseCase(
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
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.domain.usecase.conversation.ConversationTranslateUseCaseTest" 2>&1 | tail -10
```
Expected: 3 tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/android/zubanx/domain/usecase/conversation/ConversationTranslateUseCase.kt \
        app/src/test/java/com/android/zubanx/domain/usecase/conversation/ConversationTranslateUseCaseTest.kt
git commit -m "feat(conversation): add ConversationTranslateUseCase"
```

---

## Task 2: ConversationContract + ConversationViewModel

**Files:**
- Create: `app/src/main/java/com/android/zubanx/feature/conversation/ConversationContract.kt`
- Create: `app/src/main/java/com/android/zubanx/feature/conversation/ConversationViewModel.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// app/src/test/java/com/android/zubanx/feature/conversation/ConversationViewModelTest.kt
package com.android.zubanx.feature.conversation

import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.data.remote.dto.TranslateResponseDto
import com.android.zubanx.domain.usecase.conversation.ConversationTranslateUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelTest {

    private val translateUseCase: ConversationTranslateUseCase = mockk()
    private lateinit var viewModel: ConversationViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ConversationViewModel(translateUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has no messages`() = runTest {
        val state = viewModel.state.first()
        assertTrue(state is ConversationContract.State.Active)
        assertTrue((state as ConversationContract.State.Active).messages.isEmpty())
    }

    @Test
    fun `MicResultA translates and adds message to list`() = runTest {
        coEvery { translateUseCase("hello", "en", "ur") } returns
            NetworkResult.Success(TranslateResponseDto("سلام", "en", "ur"))

        viewModel.onEvent(ConversationContract.Event.MicResultA("hello"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.first() as ConversationContract.State.Active
        assertEquals(1, state.messages.size)
        assertEquals("hello", state.messages[0].originalText)
        assertEquals("سلام", state.messages[0].translatedText)
        assertEquals(ConversationContract.SpeakerSide.A, state.messages[0].speakerSide)
    }

    @Test
    fun `MicResultB translates using reversed language direction`() = runTest {
        coEvery { translateUseCase("marhaba", "ur", "en") } returns
            NetworkResult.Success(TranslateResponseDto("Hello", "ur", "en"))

        viewModel.onEvent(ConversationContract.Event.MicResultB("marhaba"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.first() as ConversationContract.State.Active
        assertEquals(1, state.messages.size)
        assertEquals("marhaba", state.messages[0].originalText)
        assertEquals("Hello", state.messages[0].translatedText)
        assertEquals(ConversationContract.SpeakerSide.B, state.messages[0].speakerSide)
    }

    @Test
    fun `ClearConversation empties the message list`() = runTest {
        coEvery { translateUseCase(any(), any(), any()) } returns
            NetworkResult.Success(TranslateResponseDto("سلام", "en", "ur"))

        viewModel.onEvent(ConversationContract.Event.MicResultA("hello"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ConversationContract.Event.ClearConversation)

        val state = viewModel.state.first() as ConversationContract.State.Active
        assertTrue(state.messages.isEmpty())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.feature.conversation.ConversationViewModelTest" 2>&1 | tail -20
```
Expected: FAIL — classes not found

- [ ] **Step 3: Implement ConversationContract**

```kotlin
// app/src/main/java/com/android/zubanx/feature/conversation/ConversationContract.kt
package com.android.zubanx.feature.conversation

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState
import com.android.zubanx.feature.translate.LanguageItem

object ConversationContract {

    enum class SpeakerSide { A, B }

    data class ConversationMessage(
        val id: Long,
        val speakerSide: SpeakerSide,
        val originalText: String,
        val translatedText: String,
        val originalLang: String,
        val targetLang: String,
        val timestamp: Long
    )

    sealed interface State : UiState {
        data class Active(
            val messages: List<ConversationMessage> = emptyList(),
            val langA: LanguageItem = LanguageItem.fromCode("en"),
            val langB: LanguageItem = LanguageItem.fromCode("ur"),
            val isTranslatingA: Boolean = false,
            val isTranslatingB: Boolean = false
        ) : State
    }

    sealed class Event : UiEvent {
        data class MicResultA(val text: String) : Event()
        data class MicResultB(val text: String) : Event()
        data class LangASelected(val lang: LanguageItem) : Event()
        data class LangBSelected(val lang: LanguageItem) : Event()
        data object LaunchMicA : Event()
        data object LaunchMicB : Event()
        data object ClearConversation : Event()
    }

    sealed class Effect : UiEffect {
        data class ShowToast(val message: String) : Effect()
        data class SpeakText(val text: String, val langCode: String) : Effect()
        data class LaunchMic(val langCode: String, val speaker: SpeakerSide) : Effect()
    }
}
```

- [ ] **Step 4: Implement ConversationViewModel**

```kotlin
// app/src/main/java/com/android/zubanx/feature/conversation/ConversationViewModel.kt
package com.android.zubanx.feature.conversation

import androidx.lifecycle.viewModelScope
import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.core.network.NetworkResult
import com.android.zubanx.domain.usecase.conversation.ConversationTranslateUseCase
import com.android.zubanx.feature.translate.LanguageItem
import kotlinx.coroutines.launch

class ConversationViewModel(
    private val translateUseCase: ConversationTranslateUseCase
) : BaseViewModel<ConversationContract.State, ConversationContract.Event, ConversationContract.Effect>(
    ConversationContract.State.Active()
) {

    private var nextId = 0L

    override fun onEvent(event: ConversationContract.Event) {
        when (event) {
            is ConversationContract.Event.LaunchMicA -> {
                val state = activeState ?: return
                sendEffect(ConversationContract.Effect.LaunchMic(state.langA.code, ConversationContract.SpeakerSide.A))
            }
            is ConversationContract.Event.LaunchMicB -> {
                val state = activeState ?: return
                sendEffect(ConversationContract.Effect.LaunchMic(state.langB.code, ConversationContract.SpeakerSide.B))
            }
            is ConversationContract.Event.MicResultA -> translateTurn(event.text, ConversationContract.SpeakerSide.A)
            is ConversationContract.Event.MicResultB -> translateTurn(event.text, ConversationContract.SpeakerSide.B)
            is ConversationContract.Event.LangASelected -> setState { (this as ConversationContract.State.Active).copy(langA = event.lang) }
            is ConversationContract.Event.LangBSelected -> setState { (this as ConversationContract.State.Active).copy(langB = event.lang) }
            is ConversationContract.Event.ClearConversation -> setState { (this as ConversationContract.State.Active).copy(messages = emptyList()) }
        }
    }

    private val activeState: ConversationContract.State.Active?
        get() = state.value as? ConversationContract.State.Active

    private fun translateTurn(text: String, speaker: ConversationContract.SpeakerSide) {
        val currentState = activeState ?: return
        val (sourceLang, targetLang) = when (speaker) {
            ConversationContract.SpeakerSide.A -> currentState.langA.code to currentState.langB.code
            ConversationContract.SpeakerSide.B -> currentState.langB.code to currentState.langA.code
        }

        setState {
            (this as ConversationContract.State.Active).copy(
                isTranslatingA = speaker == ConversationContract.SpeakerSide.A,
                isTranslatingB = speaker == ConversationContract.SpeakerSide.B
            )
        }

        viewModelScope.launch {
            when (val result = translateUseCase(text, sourceLang, targetLang)) {
                is NetworkResult.Success -> {
                    val message = ConversationContract.ConversationMessage(
                        id = nextId++,
                        speakerSide = speaker,
                        originalText = text,
                        translatedText = result.data.translatedText,
                        originalLang = sourceLang,
                        targetLang = targetLang,
                        timestamp = System.currentTimeMillis()
                    )
                    setState {
                        (this as ConversationContract.State.Active).copy(
                            messages = messages + message,
                            isTranslatingA = false,
                            isTranslatingB = false
                        )
                    }
                    sendEffect(ConversationContract.Effect.SpeakText(result.data.translatedText, targetLang))
                }
                is NetworkResult.Error -> {
                    setState {
                        (this as ConversationContract.State.Active).copy(
                            isTranslatingA = false,
                            isTranslatingB = false
                        )
                    }
                    sendEffect(ConversationContract.Effect.ShowToast(result.message))
                }
            }
        }
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.feature.conversation.ConversationViewModelTest" 2>&1 | tail -10
```
Expected: 4 tests PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/android/zubanx/feature/conversation/ \
        app/src/test/java/com/android/zubanx/feature/conversation/
git commit -m "feat(conversation): add ConversationContract and ConversationViewModel"
```

---

## Task 3: Layouts

**Files:**
- Create: `app/src/main/res/layout/fragment_conversation.xml`
- Create: `app/src/main/res/layout/item_conversation_message.xml`

- [ ] **Step 1: Create `fragment_conversation.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <!-- Speaker A panel (top) -->
    <LinearLayout
        android:id="@+id/panelSpeakerA"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:background="?attr/colorSurface"
        android:padding="12dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Speaker A"
                android:textAppearance="?attr/textAppearanceLabelMedium" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btnLangA"
                style="@style/Widget.Material3.Button.OutlinedButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="English" />

        </LinearLayout>

        <ProgressBar
            android:id="@+id/progressA"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center_horizontal"
            android:visibility="gone" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnMicA"
            style="@style/Widget.Material3.Button.FilledButton"
            android:layout_width="match_parent"
            android:layout_height="56dp"
            android:layout_marginTop="8dp"
            android:text="Speak (A)"
            app:icon="@drawable/ic_mic" />

    </LinearLayout>

    <com.google.android.material.divider.MaterialDivider
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

    <!-- Message list -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvMessages"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:clipToPadding="false"
        android:padding="8dp" />

    <!-- Clear button -->
    <com.google.android.material.button.MaterialButton
        android:id="@+id/btnClear"
        style="@style/Widget.Material3.Button.TextButton"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Clear conversation" />

    <com.google.android.material.divider.MaterialDivider
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

    <!-- Speaker B panel (bottom) -->
    <LinearLayout
        android:id="@+id/panelSpeakerB"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:background="?attr/colorSurface"
        android:padding="12dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Speaker B"
                android:textAppearance="?attr/textAppearanceLabelMedium" />

            <com.google.android.material.button.MaterialButton
                android:id="@+id/btnLangB"
                style="@style/Widget.Material3.Button.OutlinedButton"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="Urdu" />

        </LinearLayout>

        <ProgressBar
            android:id="@+id/progressB"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="center_horizontal"
            android:visibility="gone" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnMicB"
            style="@style/Widget.Material3.Button.FilledButton"
            android:layout_width="match_parent"
            android:layout_height="56dp"
            android:layout_marginTop="8dp"
            android:text="Speak (B)"
            app:icon="@drawable/ic_mic" />

    </LinearLayout>

</LinearLayout>
```

- [ ] **Step 2: Create `item_conversation_message.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/itemRoot"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="4dp">

    <!-- Original text (what was spoken) -->
    <com.google.android.material.card.MaterialCardView
        android:id="@+id/cardOriginal"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginBottom="2dp">

        <TextView
            android:id="@+id/tvOriginal"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:padding="8dp"
            android:maxWidth="240dp"
            android:textAppearance="?attr/textAppearanceBodyMedium" />

    </com.google.android.material.card.MaterialCardView>

    <!-- Translation -->
    <com.google.android.material.card.MaterialCardView
        android:id="@+id/cardTranslated"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content">

        <TextView
            android:id="@+id/tvTranslated"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:padding="8dp"
            android:maxWidth="240dp"
            android:textAppearance="?attr/textAppearanceBodySmall" />

    </com.google.android.material.card.MaterialCardView>

</LinearLayout>
```

**Note:** The alignment (left/right) for Speaker A vs B is set programmatically in the adapter using `layout_gravity` on `cardOriginal` and `cardTranslated`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/fragment_conversation.xml \
        app/src/main/res/layout/item_conversation_message.xml
git commit -m "feat(conversation): add conversation layouts"
```

---

## Task 4: ConversationFragment

**Files:**
- Create: `app/src/main/java/com/android/zubanx/feature/conversation/ConversationFragment.kt`

**Note:** The `ic_mic` drawable is already used by the Translate feature — verify it exists at `res/drawable/ic_mic.xml`. If not, create a vector drawable with `android:name="microphone"` using Material Icons.

- [ ] **Step 1: Check ic_mic exists**

```bash
ls /Users/nokhaiz/AndroidStudioProjects/ZubanXPakSoftSolution/app/src/main/res/drawable/ic_mic.xml
```

If missing, create it:

```xml
<!-- app/src/main/res/drawable/ic_mic.xml -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M12,14c1.66,0 3,-1.34 3,-3L15,5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5l0,6c0,1.66 1.34,3 3,3zM17.3,11c0,3 -2.54,5.1 -5.3,5.1S6.7,14 6.7,11L5,11c0,3.41 2.72,6.23 6,6.72L11,21h2v-3.28c3.28,-0.48 6,-3.3 6,-6.72h-1.7z"/>
</vector>
```

- [ ] **Step 2: Implement ConversationFragment**

```kotlin
// app/src/main/java/com/android/zubanx/feature/conversation/ConversationFragment.kt
package com.android.zubanx.feature.conversation

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.core.utils.toast
import com.android.zubanx.databinding.FragmentConversationBinding
import com.android.zubanx.databinding.ItemConversationMessageBinding
import com.android.zubanx.feature.translate.LanguageItem
import org.koin.androidx.viewmodel.ext.android.viewModel

class ConversationFragment : BaseFragment<FragmentConversationBinding>(FragmentConversationBinding::inflate) {

    private val viewModel: ConversationViewModel by viewModel()

    // Track which speaker launched the mic so the result routes correctly
    private var pendingMicSpeaker: ConversationContract.SpeakerSide? = null

    private val micLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                when (pendingMicSpeaker) {
                    ConversationContract.SpeakerSide.A ->
                        viewModel.onEvent(ConversationContract.Event.MicResultA(spoken))
                    ConversationContract.SpeakerSide.B ->
                        viewModel.onEvent(ConversationContract.Event.MicResultB(spoken))
                    null -> Unit
                }
            }
            pendingMicSpeaker = null
        }
    }

    private val adapter = MessageAdapter()

    override fun setupViews() {
        binding.rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.adapter = adapter

        binding.btnMicA.setOnClickListener {
            viewModel.onEvent(ConversationContract.Event.LaunchMicA)
        }
        binding.btnMicB.setOnClickListener {
            viewModel.onEvent(ConversationContract.Event.LaunchMicB)
        }
        binding.btnClear.setOnClickListener {
            viewModel.onEvent(ConversationContract.Event.ClearConversation)
        }
        binding.btnLangA.setOnClickListener { showLanguagePicker(ConversationContract.SpeakerSide.A) }
        binding.btnLangB.setOnClickListener { showLanguagePicker(ConversationContract.SpeakerSide.B) }
    }

    override fun observeState() {
        collectFlow(viewModel.state) { state ->
            if (state is ConversationContract.State.Active) {
                renderActive(state)
            }
        }
        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is ConversationContract.Effect.ShowToast -> requireContext().toast(effect.message)
                is ConversationContract.Effect.SpeakText -> {
                    // TTS stub — toast until TTS integration is added
                    requireContext().toast("Speaking: ${effect.text}")
                }
                is ConversationContract.Effect.LaunchMic -> launchMic(effect.langCode, effect.speaker)
            }
        }
    }

    private fun renderActive(state: ConversationContract.State.Active) {
        binding.btnLangA.text = state.langA.name
        binding.btnLangB.text = state.langB.name
        binding.progressA.isVisible = state.isTranslatingA
        binding.progressB.isVisible = state.isTranslatingB
        binding.btnMicA.isEnabled = !state.isTranslatingA
        binding.btnMicB.isEnabled = !state.isTranslatingB
        adapter.submitList(state.messages) {
            binding.rvMessages.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun launchMic(langCode: String, speaker: ConversationContract.SpeakerSide) {
        pendingMicSpeaker = speaker
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, requireContext().packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "$langCode-${langCode.uppercase()}")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langCode)
        }
        micLauncher.launch(intent)
    }

    private fun showLanguagePicker(speaker: ConversationContract.SpeakerSide) {
        // Exclude DETECT (auto) — both speakers must have a specific language
        val languages = LanguageItem.ALL.filter { it != LanguageItem.DETECT }
        val names = languages.map { it.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Select language")
            .setItems(names) { _, which ->
                val selected = languages[which]
                when (speaker) {
                    ConversationContract.SpeakerSide.A ->
                        viewModel.onEvent(ConversationContract.Event.LangASelected(selected))
                    ConversationContract.SpeakerSide.B ->
                        viewModel.onEvent(ConversationContract.Event.LangBSelected(selected))
                }
            }
            .show()
    }

    // --- Adapter ---
    class MessageAdapter : ListAdapter<ConversationContract.ConversationMessage, MessageAdapter.VH>(DIFF) {

        inner class VH(val b: ItemConversationMessageBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemConversationMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val msg = getItem(position)
            holder.b.tvOriginal.text = msg.originalText
            holder.b.tvTranslated.text = msg.translatedText

            // Align Speaker A to left, Speaker B to right
            val gravity = if (msg.speakerSide == ConversationContract.SpeakerSide.A) {
                android.view.Gravity.START
            } else {
                android.view.Gravity.END
            }
            (holder.b.cardOriginal.layoutParams as? android.widget.LinearLayout.LayoutParams)?.gravity = gravity
            (holder.b.cardTranslated.layoutParams as? android.widget.LinearLayout.LayoutParams)?.gravity = gravity
            holder.b.cardOriginal.requestLayout()
            holder.b.cardTranslated.requestLayout()
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<ConversationContract.ConversationMessage>() {
                override fun areItemsTheSame(a: ConversationContract.ConversationMessage, b: ConversationContract.ConversationMessage) = a.id == b.id
                override fun areContentsTheSame(a: ConversationContract.ConversationMessage, b: ConversationContract.ConversationMessage) = a == b
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/android/zubanx/feature/conversation/ConversationFragment.kt
git commit -m "feat(conversation): add ConversationFragment with dual mic panels"
```

---

## Task 5: Navigation, DI Registration

**Files:**
- Modify: `app/src/main/res/navigation/nav_conversation.xml`
- Modify: `app/src/main/java/com/android/zubanx/core/di/UseCaseModule.kt`
- Modify: `app/src/main/java/com/android/zubanx/core/di/ViewModelModule.kt`

- [ ] **Step 1: Update nav_conversation.xml**

Replace the empty navigation file with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_conversation"
    android:label="Conversation"
    app:startDestination="@id/conversationFragment">

    <fragment
        android:id="@+id/conversationFragment"
        android:name="com.android.zubanx.feature.conversation.ConversationFragment"
        android:label="Conversation" />

</navigation>
```

- [ ] **Step 2: Register ConversationTranslateUseCase in UseCaseModule**

In `app/src/main/java/com/android/zubanx/core/di/UseCaseModule.kt`, add the import and factory:

```kotlin
import com.android.zubanx.domain.usecase.conversation.ConversationTranslateUseCase

val useCaseModule = module {
    // ... existing entries ...
    factoryOf(::ConversationTranslateUseCase)
}
```

- [ ] **Step 3: Register ConversationViewModel in ViewModelModule**

In `app/src/main/java/com/android/zubanx/core/di/ViewModelModule.kt`, add:

```kotlin
import com.android.zubanx.feature.conversation.ConversationViewModel

val viewModelModule = module {
    // ... existing entries ...
    viewModelOf(::ConversationViewModel)
}
```

- [ ] **Step 4: Build to verify no compile errors**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -20
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Run all unit tests**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -20
```
Expected: All tests pass

- [ ] **Step 6: Commit**

```bash
git add app/src/main/res/navigation/nav_conversation.xml \
        app/src/main/java/com/android/zubanx/core/di/UseCaseModule.kt \
        app/src/main/java/com/android/zubanx/core/di/ViewModelModule.kt
git commit -m "feat(conversation): wire navigation and DI for Conversation feature"
```

---

## Manual Verification Checklist

After all tasks are committed, install on device/emulator and verify:

- [ ] Bottom nav "Conversation" tab navigates to the conversation screen
- [ ] Speaker A mic button launches Google mic (STT)
- [ ] Spoken text from A is translated and a message bubble appears left-aligned
- [ ] Spoken text from B is translated and a message bubble appears right-aligned
- [ ] Language picker (btnLangA / btnLangB) shows a dialog and updates the button label
- [ ] Clear button empties the message list
- [ ] Mic buttons disable during translation and re-enable after
- [ ] Error toast shown when network is unavailable

# ZubanX TTS/STT + Splash + Onboarding Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the TTS/STT service layer, Splash screen with Lottie animation, and multi-page Onboarding flow — routing new vs. returning users correctly, wiring all Koin DI, and removing the temporary PlaceholderFragment.

**Architecture:** `TtsManager`/`SttManager` are Koin singletons in `ttsModule` that wrap Android `TextToSpeech`/`SpeechRecognizer` and expose `StateFlow<TtsState>`/`StateFlow<SttState>`. `SplashFragment` reads `AppPreferences.onboardingComplete` via `SplashViewModel` after a 2-second delay and navigates to either `OnboardingFragment` or the main translate screen via a global nav action. `OnboardingFragment` uses `ViewPager2` with a `RecyclerView.Adapter` for 3 static pages and sets `onboardingComplete = true` on completion.

**Tech Stack:** Kotlin 2.1.20, Koin 4.1.1, Navigation Component 2.9.0, Lottie 6.6.6, ViewPager2, DataStore (via AppPreferences), kotlinx.coroutines 1.10.1, kotlinx.coroutines.test, mockk

---

## File Structure

**New — TTS/STT (`tts/`):**
- Create: `app/src/main/java/com/android/zubanx/tts/TtsState.kt` — sealed interface: `Idle`, `Speaking(text)`, `Error(message)`
- Create: `app/src/main/java/com/android/zubanx/tts/SttState.kt` — sealed interface: `Idle`, `Listening`, `Result(text)`, `Error(message)`
- Create: `app/src/main/java/com/android/zubanx/tts/TtsManager.kt` — wraps `TextToSpeech`, Context constructor, `StateFlow<TtsState>`, `speak(text, languageTag)`, `stop()`, `release()`
- Create: `app/src/main/java/com/android/zubanx/tts/SttManager.kt` — wraps `SpeechRecognizer`, Context constructor, `StateFlow<SttState>`, `startListening(languageTag)`, `stopListening()`, `release()`

**New — Splash (`feature/splash/`):**
- Create: `app/src/main/java/com/android/zubanx/feature/splash/SplashContract.kt`
- Create: `app/src/main/java/com/android/zubanx/feature/splash/SplashViewModel.kt`
- Create: `app/src/main/java/com/android/zubanx/feature/splash/SplashFragment.kt`

**New — Onboarding (`feature/onboarding/`):**
- Create: `app/src/main/java/com/android/zubanx/feature/onboarding/OnboardingContract.kt`
- Create: `app/src/main/java/com/android/zubanx/feature/onboarding/OnboardingViewModel.kt`
- Create: `app/src/main/java/com/android/zubanx/feature/onboarding/OnboardingPagerAdapter.kt`
- Create: `app/src/main/java/com/android/zubanx/feature/onboarding/OnboardingFragment.kt`

**New — Translate stub (`feature/translate/`):**
- Create: `app/src/main/java/com/android/zubanx/feature/translate/TranslateFragment.kt` — empty stub, Plan 5 replaces

**New — Layouts:**
- Create: `app/src/main/res/layout/fragment_splash.xml`
- Create: `app/src/main/res/layout/fragment_onboarding.xml`
- Create: `app/src/main/res/layout/fragment_onboarding_page.xml`
- Create: `app/src/main/res/layout/fragment_translate.xml` — stub FrameLayout

**New — Resources:**
- Create: `app/src/main/res/raw/splash_animation.json` — minimal valid Lottie placeholder

**Modified:**
- Modify: `app/src/main/java/com/android/zubanx/core/di/TtsModule.kt`
- Modify: `app/src/main/java/com/android/zubanx/core/di/ViewModelModule.kt`
- Modify: `app/src/main/res/navigation/nav_onboarding.xml` — replace placeholder with real destinations
- Modify: `app/src/main/res/navigation/nav_translate.xml` — add TranslateFragment as startDestination
- Modify: `app/src/main/res/navigation/nav_graph.xml` — add global action onboarding → translate
- Modify: `app/src/main/java/com/android/zubanx/app/MainActivity.kt` — uncomment hidden destinations

**Deleted:**
- Delete: `app/src/main/java/com/android/zubanx/feature/placeholder/PlaceholderFragment.kt`
- Delete: `app/src/main/res/layout/fragment_placeholder.xml`

**Tests:**
- Create: `app/src/test/java/com/android/zubanx/tts/TtsStateTest.kt`
- Create: `app/src/test/java/com/android/zubanx/tts/SttStateTest.kt`
- Create: `app/src/test/java/com/android/zubanx/feature/splash/SplashViewModelTest.kt`
- Create: `app/src/test/java/com/android/zubanx/feature/onboarding/OnboardingViewModelTest.kt`

---

## Chunk 1: TTS/STT Layer

### Task 1: `TtsState` sealed interface

**Files:**
- Create: `app/src/main/java/com/android/zubanx/tts/TtsState.kt`
- Test: `app/src/test/java/com/android/zubanx/tts/TtsStateTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/android/zubanx/tts/TtsStateTest.kt`:
```kotlin
package com.android.zubanx.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsStateTest {

    @Test
    fun `TtsState Idle is the initial state sentinel`() {
        val state: TtsState = TtsState.Idle
        assertTrue(state is TtsState.Idle)
    }

    @Test
    fun `TtsState Speaking holds the text being spoken`() {
        val state: TtsState = TtsState.Speaking(text = "Hello")
        assertTrue(state is TtsState.Speaking)
        assertEquals("Hello", (state as TtsState.Speaking).text)
    }

    @Test
    fun `TtsState Error holds an error message`() {
        val state: TtsState = TtsState.Error(message = "TTS init failed")
        assertTrue(state is TtsState.Error)
        assertEquals("TTS init failed", (state as TtsState.Error).message)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.tts.TtsStateTest"
```
Expected: BUILD FAILED — unresolved reference: `TtsState` (the class does not exist yet).

- [ ] **Step 3: Create `TtsState.kt`**

Create `app/src/main/java/com/android/zubanx/tts/TtsState.kt`:
```kotlin
package com.android.zubanx.tts

/**
 * UI-observable state for the Text-to-Speech engine.
 *
 * Collected via [TtsManager.state] — a `StateFlow<TtsState>` registered as
 * a `single` in [ttsModule].
 */
sealed interface TtsState {
    /** Engine is initialised and idle. */
    data object Idle : TtsState

    /** Engine is currently speaking [text]. */
    data class Speaking(val text: String) : TtsState

    /** Engine encountered an error described by [message]. */
    data class Error(val message: String) : TtsState
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.tts.TtsStateTest"
```
Expected: BUILD SUCCESSFUL, 3 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/android/zubanx/tts/TtsState.kt \
        app/src/test/java/com/android/zubanx/tts/TtsStateTest.kt
git commit -m "feat: add TtsState sealed interface"
```

---

### Task 2: `SttState` sealed interface

**Files:**
- Create: `app/src/main/java/com/android/zubanx/tts/SttState.kt`
- Test: `app/src/test/java/com/android/zubanx/tts/SttStateTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/android/zubanx/tts/SttStateTest.kt`:
```kotlin
package com.android.zubanx.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SttStateTest {

    @Test
    fun `SttState Idle is the initial state`() {
        val state: SttState = SttState.Idle
        assertTrue(state is SttState.Idle)
    }

    @Test
    fun `SttState Listening represents active recording`() {
        val state: SttState = SttState.Listening
        assertTrue(state is SttState.Listening)
    }

    @Test
    fun `SttState Result holds recognized text`() {
        val state: SttState = SttState.Result(text = "Translate this")
        assertTrue(state is SttState.Result)
        assertEquals("Translate this", (state as SttState.Result).text)
    }

    @Test
    fun `SttState Error holds an error message`() {
        val state: SttState = SttState.Error(message = "Permission denied")
        assertTrue(state is SttState.Error)
        assertEquals("Permission denied", (state as SttState.Error).message)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.tts.SttStateTest"
```
Expected: BUILD FAILED — unresolved reference: `SttState` (the class does not exist yet).

- [ ] **Step 3: Create `SttState.kt`**

Create `app/src/main/java/com/android/zubanx/tts/SttState.kt`:
```kotlin
package com.android.zubanx.tts

/**
 * UI-observable state for the Speech-to-Text engine.
 *
 * Collected via [SttManager.state] — a `StateFlow<SttState>` registered as
 * a `single` in [ttsModule].
 */
sealed interface SttState {
    /** Recogniser is ready but not actively listening. */
    data object Idle : SttState

    /** Recogniser is actively recording audio. */
    data object Listening : SttState

    /** Recogniser produced a final transcript. */
    data class Result(val text: String) : SttState

    /** Recogniser encountered an error described by [message]. */
    data class Error(val message: String) : SttState
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.tts.SttStateTest"
```
Expected: BUILD SUCCESSFUL, 4 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/android/zubanx/tts/SttState.kt \
        app/src/test/java/com/android/zubanx/tts/SttStateTest.kt
git commit -m "feat: add SttState sealed interface"
```

---

### Task 3: `TtsManager`

**Files:**
- Create: `app/src/main/java/com/android/zubanx/tts/TtsManager.kt`

Note: `TtsManager` wraps Android's `TextToSpeech`, which requires a device TTS engine to function. Behaviour tests (speak/stop) require instrumented tests. This task verifies compilation only.

- [ ] **Step 1: Create `TtsManager.kt`**

Create `app/src/main/java/com/android/zubanx/tts/TtsManager.kt`:
```kotlin
package com.android.zubanx.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.Locale
import java.util.UUID

/**
 * Singleton wrapper around Android [TextToSpeech].
 *
 * Exposes [state] as a `StateFlow<TtsState>` so any Fragment or ViewModel can observe
 * the current TTS status without polling. Registered as a `single` in [ttsModule].
 *
 * **Lifecycle:** Call [release] when the Application is destroyed (handled by Koin scope teardown).
 *
 * @param context Application context — must be the Application context to avoid leaks.
 */
class TtsManager(context: Context) {

    private val _state = MutableStateFlow<TtsState>(TtsState.Idle)

    /** Current TTS state. Observe via `StateFlow.collect`. */
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.ERROR) {
            Timber.e("TtsManager: TextToSpeech initialisation failed (status=$status)")
            _state.value = TtsState.Error("TTS initialisation failed")
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                _state.value = TtsState.Idle
            }

            @Deprecated("Deprecated in API 21")
            override fun onError(utteranceId: String?) {
                _state.value = TtsState.Error("TTS utterance failed")
                Timber.w("TtsManager: utterance error for id=$utteranceId")
            }
        })
    }

    /**
     * Speaks [text] using the locale for [languageTag] (BCP-47, e.g. `"en"`, `"es-MX"`).
     *
     * Sets state to [TtsState.Speaking] immediately and returns to [TtsState.Idle] when done.
     * If the engine is already speaking, the current utterance is interrupted.
     */
    fun speak(text: String, languageTag: String = "en") {
        tts.language = Locale.forLanguageTag(languageTag)
        _state.value = TtsState.Speaking(text)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    /**
     * Stops any in-progress speech immediately. State returns to [TtsState.Idle].
     */
    fun stop() {
        tts.stop()
        _state.value = TtsState.Idle
    }

    /**
     * Releases TTS resources. After calling this, [speak] and [stop] must not be called.
     */
    fun release() {
        tts.stop()
        tts.shutdown()
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/android/zubanx/tts/TtsManager.kt
git commit -m "feat: add TtsManager — Android TextToSpeech StateFlow wrapper"
```

---

### Task 4: `SttManager`

**Files:**
- Create: `app/src/main/java/com/android/zubanx/tts/SttManager.kt`

Same note as Task 3 — SpeechRecognizer requires a device environment. Compile verification only.

- [ ] **Step 1: Create `SttManager.kt`**

Create `app/src/main/java/com/android/zubanx/tts/SttManager.kt`:
```kotlin
package com.android.zubanx.tts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Singleton wrapper around Android [SpeechRecognizer].
 *
 * Exposes [state] as `StateFlow<SttState>` for reactive observation.
 * Registered as a `single` in [ttsModule].
 *
 * **Threading:** [SpeechRecognizer] must be created and used on the main thread.
 * All public functions must be called from the main thread.
 *
 * @param context Application context.
 */
class SttManager(context: Context) {

    private val _state = MutableStateFlow<SttState>(SttState.Idle)

    /** Current STT state. Observe via `StateFlow.collect`. */
    val state: StateFlow<SttState> = _state.asStateFlow()

    private val recognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context)

    init {
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = SttState.Listening
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val message = sttErrorMessage(error)
                Timber.w("SttManager: recognition error $error — $message")
                _state.value = SttState.Error(message)
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                _state.value = SttState.Result(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    /**
     * Starts listening for speech in the locale for [languageTag] (BCP-47).
     * Must be called from the main thread.
     */
    fun startListening(languageTag: String = "en-US") {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer.startListening(intent)
    }

    /**
     * Stops listening. Final results may still arrive after this call.
     * Must be called from the main thread.
     */
    fun stopListening() {
        recognizer.stopListening()
    }

    /**
     * Releases recogniser resources. Must be called from the main thread.
     * After calling this, [startListening] and [stopListening] must not be called.
     */
    fun release() {
        recognizer.destroy()
    }

    private fun sttErrorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recogniser busy"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
        else -> "Unknown error ($error)"
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/android/zubanx/tts/SttManager.kt
git commit -m "feat: add SttManager — Android SpeechRecognizer StateFlow wrapper"
```

---

### Task 5: Wire `ttsModule`

**Files:**
- Modify: `app/src/main/java/com/android/zubanx/core/di/TtsModule.kt`

- [ ] **Step 1: Update `TtsModule.kt`**

Replace the full contents of `app/src/main/java/com/android/zubanx/core/di/TtsModule.kt`:
```kotlin
package com.android.zubanx.core.di

import com.android.zubanx.tts.SttManager
import com.android.zubanx.tts.TtsManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val ttsModule = module {
    // Context-based construction — Android framework static factories require androidContext()
    single { TtsManager(androidContext()) }
    single { SttManager(androidContext()) }
}
```

- [ ] **Step 2: Build to verify**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run unit tests to confirm nothing broken**

```bash
./gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, all existing tests pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/android/zubanx/core/di/TtsModule.kt
git commit -m "feat: wire ttsModule — TtsManager and SttManager singletons"
```

---

## Chunk 2: Splash Screen

### Task 6: `SplashContract`

**Files:**
- Create: `app/src/main/java/com/android/zubanx/feature/splash/SplashContract.kt`

- [ ] **Step 1: Create `SplashContract.kt`**

Create `app/src/main/java/com/android/zubanx/feature/splash/SplashContract.kt`:
```kotlin
package com.android.zubanx.feature.splash

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState

object SplashContract {

    /**
     * The splash screen has one meaningful state — the animation is playing.
     * No Loading/Success/Error distinction is needed here.
     */
    data object State : UiState

    sealed class Event : UiEvent {
        /** Sent by the Fragment when the view is created; triggers the route-check coroutine. */
        data object Init : Event()
    }

    sealed class Effect : UiEffect {
        /** Navigate to the Onboarding screen (first launch). */
        data object NavigateToOnboarding : Effect()

        /** Navigate to the main Translate screen (returning user). */
        data object NavigateToHome : Effect()
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/android/zubanx/feature/splash/SplashContract.kt
git commit -m "feat: add SplashContract MVI contract"
```

---

### Task 7: `SplashViewModel`

**Files:**
- Create: `app/src/main/java/com/android/zubanx/feature/splash/SplashViewModel.kt`
- Test: `app/src/test/java/com/android/zubanx/feature/splash/SplashViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/android/zubanx/feature/splash/SplashViewModelTest.kt`:
```kotlin
package com.android.zubanx.feature.splash

import app.cash.turbine.test
import com.android.zubanx.data.local.datastore.AppPreferences
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val prefs: AppPreferences = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Init with onboardingComplete false emits NavigateToOnboarding after delay`() = runTest {
        every { prefs.onboardingComplete } returns flowOf(false)
        val vm = SplashViewModel(prefs)

        vm.effect.test {
            vm.onEvent(SplashContract.Event.Init)
            testDispatcher.scheduler.advanceUntilIdle()
            val effect = awaitItem()
            assertEquals(SplashContract.Effect.NavigateToOnboarding, effect)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Init with onboardingComplete true emits NavigateToHome after delay`() = runTest {
        every { prefs.onboardingComplete } returns flowOf(true)
        val vm = SplashViewModel(prefs)

        vm.effect.test {
            vm.onEvent(SplashContract.Event.Init)
            testDispatcher.scheduler.advanceUntilIdle()
            val effect = awaitItem()
            assertEquals(SplashContract.Effect.NavigateToHome, effect)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.feature.splash.SplashViewModelTest"
```
Expected: FAILED — `SplashViewModel` does not exist yet.

- [ ] **Step 3: Create `SplashViewModel.kt`**

Create `app/src/main/java/com/android/zubanx/feature/splash/SplashViewModel.kt`:
```kotlin
package com.android.zubanx.feature.splash

import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.data.local.datastore.AppPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val SPLASH_DELAY_MS = 2_000L

/**
 * ViewModel for [SplashFragment].
 *
 * On [SplashContract.Event.Init], waits [SPLASH_DELAY_MS] ms (for the Lottie animation),
 * reads [AppPreferences.onboardingComplete] once, and emits either:
 * - [SplashContract.Effect.NavigateToOnboarding] (first launch)
 * - [SplashContract.Effect.NavigateToHome] (returning user)
 *
 * The ViewModel does NOT navigate — it emits a one-shot Effect. The Fragment navigates.
 */
class SplashViewModel(
    private val prefs: AppPreferences
) : BaseViewModel<SplashContract.State, SplashContract.Event, SplashContract.Effect>(
    SplashContract.State
) {
    override fun onEvent(event: SplashContract.Event) {
        when (event) {
            SplashContract.Event.Init -> handleInit()
        }
    }

    private fun handleInit() {
        viewModelScope.launch {
            delay(SPLASH_DELAY_MS)
            val isOnboarded = prefs.onboardingComplete.first()
            sendEffect(
                if (isOnboarded) SplashContract.Effect.NavigateToHome
                else SplashContract.Effect.NavigateToOnboarding
            )
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.feature.splash.SplashViewModelTest"
```
Expected: BUILD SUCCESSFUL, 2 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/android/zubanx/feature/splash/SplashViewModel.kt \
        app/src/test/java/com/android/zubanx/feature/splash/SplashViewModelTest.kt
git commit -m "feat: add SplashViewModel — onboarding check with 2s splash delay"
```

---

### Task 8: Splash layout + Lottie placeholder

**Files:**
- Create: `app/src/main/res/raw/splash_animation.json`
- Create: `app/src/main/res/layout/fragment_splash.xml`

- [ ] **Step 1: Create `splash_animation.json`**

Create `app/src/main/res/raw/splash_animation.json`:
```json
{
  "v": "5.7.4",
  "fr": 30,
  "ip": 0,
  "op": 60,
  "w": 400,
  "h": 400,
  "nm": "ZubanX Splash",
  "ddd": 0,
  "assets": [],
  "layers": []
}
```
This is a valid but empty 2-second Lottie composition. Replace with the real branded animation before release.

- [ ] **Step 2: Create `fragment_splash.xml`**

Create `app/src/main/res/layout/fragment_splash.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="?attr/colorSurface">

    <com.airbnb.lottie.LottieAnimationView
        android:id="@+id/lottie_splash"
        android:layout_width="240dp"
        android:layout_height="240dp"
        app:layout_constraintBottom_toTopOf="@id/tv_app_name"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintVertical_chainStyle="packed"
        app:lottie_autoPlay="true"
        app:lottie_loop="false"
        app:lottie_rawRes="@raw/splash_animation" />

    <TextView
        android:id="@+id/tv_app_name"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:text="@string/app_name"
        android:textAppearance="?attr/textAppearanceHeadlineMedium"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@id/lottie_splash" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 3: Verify it compiles**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/layout/fragment_splash.xml \
        app/src/main/res/raw/splash_animation.json
git commit -m "feat: add splash layout and placeholder Lottie animation"
```

---

### Task 9: `SplashFragment`

**Files:**
- Create: `app/src/main/java/com/android/zubanx/feature/splash/SplashFragment.kt`

Note: `R.id.action_splash_to_onboarding` and `R.id.action_global_onboarding_to_translate` are defined in Task 15 (nav XML update). The build will fail until then. Commit the file now; full compilation is verified in Task 15.

- [ ] **Step 1: Create `SplashFragment.kt`**

Create `app/src/main/java/com/android/zubanx/feature/splash/SplashFragment.kt`:
```kotlin
package com.android.zubanx.feature.splash

import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.android.zubanx.R
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.databinding.FragmentSplashBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Splash screen — shown on every app launch.
 *
 * Plays a Lottie animation while [SplashViewModel] waits 2 seconds and checks
 * whether onboarding has been completed. Navigates based on the emitted Effect.
 *
 * This destination is removed from the back stack via `popUpToInclusive = true`
 * so the user cannot press Back to return to it.
 */
class SplashFragment : BaseFragment<FragmentSplashBinding>(FragmentSplashBinding::inflate) {

    private val viewModel: SplashViewModel by viewModel()

    override fun setupViews() {
        viewModel.onEvent(SplashContract.Event.Init)
    }

    override fun observeState() {
        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                SplashContract.Effect.NavigateToOnboarding ->
                    findNavController().navigate(R.id.action_splash_to_onboarding)
                SplashContract.Effect.NavigateToHome ->
                    findNavController().navigate(
                        R.id.action_global_onboarding_to_translate,
                        null,
                        NavOptions.Builder()
                            .setEnterAnim(R.anim.scale_fade_in)
                            .setExitAnim(R.anim.fade_out)
                            .setPopUpTo(R.id.nav_onboarding, inclusive = true)
                            .build()
                    )
            }
        }
    }
}
```

- [ ] **Step 2: Commit (build verified in Task 15)**

```bash
git add app/src/main/java/com/android/zubanx/feature/splash/SplashFragment.kt
git commit -m "feat: add SplashFragment — Lottie + MVI navigation routing"
```

---

## Chunk 3: Onboarding Screen

### Task 10: `OnboardingContract`

**Files:**
- Create: `app/src/main/java/com/android/zubanx/feature/onboarding/OnboardingContract.kt`

- [ ] **Step 1: Create `OnboardingContract.kt`**

Create `app/src/main/java/com/android/zubanx/feature/onboarding/OnboardingContract.kt`:
```kotlin
package com.android.zubanx.feature.onboarding

import com.android.zubanx.core.mvi.UiEffect
import com.android.zubanx.core.mvi.UiEvent
import com.android.zubanx.core.mvi.UiState

object OnboardingContract {

    data class State(
        val currentPage: Int = 0,
        val totalPages: Int = 3
    ) : UiState

    sealed class Event : UiEvent {
        /** User tapped the Skip button on any page. */
        data object SkipClicked : Event()

        /** User tapped the Done button on the last page. */
        data object DoneClicked : Event()

        /** ViewPager page changed. */
        data class PageChanged(val page: Int) : Event()
    }

    sealed class Effect : UiEffect {
        /** Navigate to the main Translate screen and clear the onboarding back stack. */
        data object NavigateToHome : Effect()
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD FAILED — this is expected. `SplashFragment.kt` (committed in Task 9) references `R.id.action_splash_to_onboarding` and `R.id.action_global_onboarding_to_translate`, which are not defined until Task 15. **Do not attempt to fix this error** — it will be resolved in Task 15.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/android/zubanx/feature/onboarding/OnboardingContract.kt
git commit -m "feat: add OnboardingContract MVI contract"
```

---

### Task 11: `OnboardingViewModel`

**Files:**
- Create: `app/src/main/java/com/android/zubanx/feature/onboarding/OnboardingViewModel.kt`
- Test: `app/src/test/java/com/android/zubanx/feature/onboarding/OnboardingViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/android/zubanx/feature/onboarding/OnboardingViewModelTest.kt`:
```kotlin
package com.android.zubanx.feature.onboarding

import app.cash.turbine.test
import com.android.zubanx.data.local.datastore.AppPreferences
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val prefs: AppPreferences = mockk(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has currentPage 0 and totalPages 3`() {
        val vm = OnboardingViewModel(prefs)
        assertEquals(0, vm.state.value.currentPage)
        assertEquals(3, vm.state.value.totalPages)
    }

    @Test
    fun `PageChanged event updates currentPage in state`() {
        val vm = OnboardingViewModel(prefs)
        vm.onEvent(OnboardingContract.Event.PageChanged(2))
        assertEquals(2, vm.state.value.currentPage)
    }

    @Test
    fun `DoneClicked sets onboardingComplete true and emits NavigateToHome`() = runTest {
        val vm = OnboardingViewModel(prefs)
        vm.effect.test {
            vm.onEvent(OnboardingContract.Event.DoneClicked)
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify { prefs.setOnboardingComplete(true) }
            val effect = awaitItem()
            assertEquals(OnboardingContract.Effect.NavigateToHome, effect)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `SkipClicked sets onboardingComplete true and emits NavigateToHome`() = runTest {
        val vm = OnboardingViewModel(prefs)
        vm.effect.test {
            vm.onEvent(OnboardingContract.Event.SkipClicked)
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify { prefs.setOnboardingComplete(true) }
            val effect = awaitItem()
            assertEquals(OnboardingContract.Effect.NavigateToHome, effect)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.feature.onboarding.OnboardingViewModelTest"
```
Expected: FAILED — `OnboardingViewModel` does not exist yet.

- [ ] **Step 3: Create `OnboardingViewModel.kt`**

Create `app/src/main/java/com/android/zubanx/feature/onboarding/OnboardingViewModel.kt`:
```kotlin
package com.android.zubanx.feature.onboarding

import com.android.zubanx.core.mvi.BaseViewModel
import com.android.zubanx.data.local.datastore.AppPreferences
import kotlinx.coroutines.launch

/**
 * ViewModel for [OnboardingFragment].
 *
 * Handles skip and done gestures by persisting `onboardingComplete = true`
 * and emitting [OnboardingContract.Effect.NavigateToHome].
 *
 * Page tracking is maintained in [OnboardingContract.State.currentPage] so the
 * Fragment can show "Next" vs "Done" on the last page.
 */
class OnboardingViewModel(
    private val prefs: AppPreferences
) : BaseViewModel<OnboardingContract.State, OnboardingContract.Event, OnboardingContract.Effect>(
    OnboardingContract.State()
) {
    override fun onEvent(event: OnboardingContract.Event) {
        when (event) {
            OnboardingContract.Event.DoneClicked -> completeOnboarding()
            OnboardingContract.Event.SkipClicked -> completeOnboarding()
            is OnboardingContract.Event.PageChanged -> setState { copy(currentPage = event.page) }
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            prefs.setOnboardingComplete(true)
            sendEffect(OnboardingContract.Effect.NavigateToHome)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "com.android.zubanx.feature.onboarding.OnboardingViewModelTest"
```
Expected: BUILD SUCCESSFUL, 4 tests passed.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/android/zubanx/feature/onboarding/OnboardingViewModel.kt \
        app/src/test/java/com/android/zubanx/feature/onboarding/OnboardingViewModelTest.kt
git commit -m "feat: add OnboardingViewModel — completes onboarding and navigates home"
```

---

### Task 12: Onboarding layouts + `OnboardingPagerAdapter`

**Files:**
- Create: `app/src/main/res/layout/fragment_onboarding_page.xml`
- Create: `app/src/main/res/layout/fragment_onboarding.xml`
- Create: `app/src/main/java/com/android/zubanx/feature/onboarding/OnboardingPagerAdapter.kt`

- [ ] **Step 1: Create `fragment_onboarding_page.xml`**

Create `app/src/main/res/layout/fragment_onboarding_page.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="vertical"
    android:padding="32dp">

    <ImageView
        android:id="@+id/iv_page_image"
        android:layout_width="200dp"
        android:layout_height="200dp"
        android:contentDescription="@null"
        android:src="@drawable/ic_launcher_foreground" />

    <TextView
        android:id="@+id/tv_page_title"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="24dp"
        android:gravity="center"
        android:text="Title"
        android:textAppearance="?attr/textAppearanceTitleLarge" />

    <TextView
        android:id="@+id/tv_page_description"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:gravity="center"
        android:text="Description"
        android:textAppearance="?attr/textAppearanceBodyMedium" />

</LinearLayout>
```

- [ ] **Step 2: Create `fragment_onboarding.xml`**

Create `app/src/main/res/layout/fragment_onboarding.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.viewpager2.widget.ViewPager2
        android:id="@+id/view_pager"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintBottom_toTopOf="@id/btn_skip"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <Button
        android:id="@+id/btn_skip"
        style="@style/Widget.Material3.Button.TextButton"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginBottom="24dp"
        android:text="Skip"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toStartOf="@id/btn_next"
        app:layout_constraintStart_toStartOf="parent" />

    <Button
        android:id="@+id/btn_next"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginBottom="24dp"
        android:text="Next"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toEndOf="@id/btn_skip" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

- [ ] **Step 3: Create `OnboardingPagerAdapter.kt`**

Create `app/src/main/java/com/android/zubanx/feature/onboarding/OnboardingPagerAdapter.kt`:
```kotlin
package com.android.zubanx.feature.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.databinding.FragmentOnboardingPageBinding

/**
 * ViewPager2 adapter for the static onboarding pages.
 *
 * [pages] is a list of (title, description) pairs provided by [OnboardingFragment].
 */
class OnboardingPagerAdapter(
    private val pages: List<Pair<String, String>>
) : RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder>() {

    inner class PageViewHolder(
        private val binding: FragmentOnboardingPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(title: String, description: String) {
            binding.tvPageTitle.text = title
            binding.tvPageDescription.text = description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = FragmentOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val (title, description) = pages[position]
        holder.bind(title, description)
    }

    override fun getItemCount(): Int = pages.size
}
```

- [ ] **Step 4: Verify it compiles**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD FAILED — this is expected (same reason as Task 10: `SplashFragment.kt` has unresolved nav IDs until Task 15). **Do not attempt to fix this error.**

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/layout/fragment_onboarding_page.xml \
        app/src/main/res/layout/fragment_onboarding.xml \
        app/src/main/java/com/android/zubanx/feature/onboarding/OnboardingPagerAdapter.kt
git commit -m "feat: add onboarding layouts and OnboardingPagerAdapter"
```

---

### Task 13: `OnboardingFragment`

**Files:**
- Create: `app/src/main/java/com/android/zubanx/feature/onboarding/OnboardingFragment.kt`

Same note as SplashFragment — `R.id.action_global_onboarding_to_translate` is defined in Task 15. Commit now; compilation verified in Task 15.

- [ ] **Step 1: Create `OnboardingFragment.kt`**

Create `app/src/main/java/com/android/zubanx/feature/onboarding/OnboardingFragment.kt`:
```kotlin
package com.android.zubanx.feature.onboarding

import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.android.zubanx.R
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.utils.collectFlow
import com.android.zubanx.databinding.FragmentOnboardingBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Onboarding flow — shown only on first launch.
 *
 * Three static pages are displayed in a [ViewPager2]. The Skip button is always
 * visible. The Next button reads "Done" on the last page. Either Skip or Done
 * triggers [OnboardingContract.Event.SkipClicked]/[OnboardingContract.Event.DoneClicked],
 * which persists `onboardingComplete = true` and navigates to the main screen.
 *
 * Back press while on a non-first page scrolls the pager back one page.
 * Back press on page 0 does nothing (default back behaviour is suppressed to prevent
 * returning to the splash screen).
 */
class OnboardingFragment : BaseFragment<FragmentOnboardingBinding>(FragmentOnboardingBinding::inflate) {

    private val viewModel: OnboardingViewModel by viewModel()

    private val pages = listOf(
        "Translate Instantly" to "Translate text, speech, and images across 100+ languages in real-time.",
        "AI Expert Mode" to "Choose your AI expert — GPT, Gemini, Claude, or our built-in model.",
        "Works Offline" to "Download language packs and translate without an internet connection."
    )

    private val adapter by lazy { OnboardingPagerAdapter(pages) }

    override fun setupViews() {
        binding.viewPager.adapter = adapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.onEvent(OnboardingContract.Event.PageChanged(position))
                binding.btnNext.text = if (position == pages.lastIndex) "Done" else "Next"
            }
        })

        binding.btnSkip.setOnClickListener {
            viewModel.onEvent(OnboardingContract.Event.SkipClicked)
        }

        binding.btnNext.setOnClickListener {
            val currentPage = viewModel.state.value.currentPage
            if (currentPage < pages.lastIndex) {
                binding.viewPager.currentItem = currentPage + 1
            } else {
                viewModel.onEvent(OnboardingContract.Event.DoneClicked)
            }
        }

        backPressHandler = {
            val currentPage = viewModel.state.value.currentPage
            if (currentPage > 0) {
                binding.viewPager.currentItem = currentPage - 1
                true
            } else {
                // Suppress back on first page — user must Skip or complete onboarding
                true
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (backPressHandler?.invoke() != true) {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun observeState() {
        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                OnboardingContract.Effect.NavigateToHome -> navigateToHome()
            }
        }
    }

    private fun navigateToHome() {
        findNavController().navigate(
            R.id.action_global_onboarding_to_translate,
            null,
            NavOptions.Builder()
                .setEnterAnim(R.anim.scale_fade_in)
                .setExitAnim(R.anim.fade_out)
                .setPopUpTo(R.id.nav_onboarding, inclusive = true)
                .build()
        )
    }
}
```

- [ ] **Step 2: Commit (build verified in Task 15)**

```bash
git add app/src/main/java/com/android/zubanx/feature/onboarding/OnboardingFragment.kt
git commit -m "feat: add OnboardingFragment — ViewPager2 3-page onboarding with skip/done"
```

---

## Chunk 4: Navigation Wiring + Translate Stub + Cleanup

### Task 14: Translate stub + `nav_translate.xml` start destination

**Files:**
- Create: `app/src/main/res/layout/fragment_translate.xml`
- Create: `app/src/main/java/com/android/zubanx/feature/translate/TranslateFragment.kt`
- Modify: `app/src/main/res/navigation/nav_translate.xml`

The current `nav_translate.xml` has no start destination, so tapping the "Translate" bottom nav item crashes. This task adds a minimal stub so it compiles and launches without crashing. Plan 5 replaces the stub with the full Translate feature.

- [ ] **Step 1: Create `fragment_translate.xml`**

Create `app/src/main/res/layout/fragment_translate.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- Stub — replaced with full Translate UI in Plan 5 -->

</FrameLayout>
```

- [ ] **Step 2: Create `TranslateFragment.kt`**

Create `app/src/main/java/com/android/zubanx/feature/translate/TranslateFragment.kt`:
```kotlin
package com.android.zubanx.feature.translate

import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.databinding.FragmentTranslateBinding

// Stub — replaced with full Translate feature in Plan 5
class TranslateFragment : BaseFragment<FragmentTranslateBinding>(FragmentTranslateBinding::inflate)
```

- [ ] **Step 3: Update `nav_translate.xml`**

Replace the full contents of `app/src/main/res/navigation/nav_translate.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_translate"
    android:label="Translate"
    app:startDestination="@id/translateFragment">

    <!-- TranslateFragment is a stub — replaced with full implementation in Plan 5 -->
    <fragment
        android:id="@+id/translateFragment"
        android:name="com.android.zubanx.feature.translate.TranslateFragment"
        android:label="Translate" />

</navigation>
```

- [ ] **Step 4: Build to verify**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD FAILED — this is expected. `SplashFragment.kt` (Task 9) and `OnboardingFragment.kt` (Task 13) both reference `R.id.action_global_onboarding_to_translate` which is not defined until Task 15. **Do not attempt to fix this error** — it is resolved when Task 15 updates `nav_graph.xml`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/android/zubanx/feature/translate/TranslateFragment.kt \
        app/src/main/res/layout/fragment_translate.xml \
        app/src/main/res/navigation/nav_translate.xml
git commit -m "feat: add TranslateFragment stub and nav_translate start destination (stub; full impl in Plan 5)"
```

---

### Task 15: Update `nav_onboarding.xml` and `nav_graph.xml`

**Files:**
- Modify: `app/src/main/res/navigation/nav_onboarding.xml`
- Modify: `app/src/main/res/navigation/nav_graph.xml`

This task defines the two nav IDs referenced by `SplashFragment` and `OnboardingFragment`:
- `R.id.action_splash_to_onboarding` — in nav_onboarding.xml
- `R.id.action_global_onboarding_to_translate` — global action in nav_graph.xml

After this task, the full build succeeds.

- [ ] **Step 1: Replace `nav_onboarding.xml`**

Replace the full contents of `app/src/main/res/navigation/nav_onboarding.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_onboarding"
    android:label="Onboarding"
    app:startDestination="@id/splashFragment">

    <fragment
        android:id="@+id/splashFragment"
        android:name="com.android.zubanx.feature.splash.SplashFragment"
        android:label="Splash">

        <action
            android:id="@+id/action_splash_to_onboarding"
            app:destination="@id/onboardingFragment"
            app:enterAnim="@anim/fade_in"
            app:exitAnim="@anim/fade_out" />

    </fragment>

    <fragment
        android:id="@+id/onboardingFragment"
        android:name="com.android.zubanx.feature.onboarding.OnboardingFragment"
        android:label="Onboarding" />

</navigation>
```

- [ ] **Step 2: Replace `nav_graph.xml`**

Replace the full contents of `app/src/main/res/navigation/nav_graph.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_graph"
    app:startDestination="@id/nav_onboarding">

    <include app:graph="@navigation/nav_onboarding" />
    <include app:graph="@navigation/nav_translate" />
    <include app:graph="@navigation/nav_conversation" />
    <include app:graph="@navigation/nav_dictionary" />
    <include app:graph="@navigation/nav_idioms" />
    <include app:graph="@navigation/nav_phrases" />
    <include app:graph="@navigation/nav_story" />
    <include app:graph="@navigation/nav_settings" />
    <include app:graph="@navigation/nav_premium" />
    <include app:graph="@navigation/nav_favourite" />
    <include app:graph="@navigation/nav_onscreen" />
    <include app:graph="@navigation/nav_imagetext" />

    <!--
        Global action used by both SplashFragment and OnboardingFragment to navigate
        to the main translate screen. Pops the entire onboarding graph off the back
        stack so the user cannot press Back to return to the splash or onboarding.
    -->
    <action
        android:id="@+id/action_global_onboarding_to_translate"
        app:destination="@id/nav_translate"
        app:enterAnim="@anim/scale_fade_in"
        app:exitAnim="@anim/fade_out"
        app:popUpTo="@id/nav_onboarding"
        app:popUpToInclusive="true" />

</navigation>
```

- [ ] **Step 3: Build to verify (first full-passing build)**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL — all nav IDs now resolved.

- [ ] **Step 4: Run all unit tests**

```bash
./gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/navigation/nav_onboarding.xml \
        app/src/main/res/navigation/nav_graph.xml
git commit -m "feat: wire nav_onboarding and nav_graph — splash/onboarding routing + global translate action"
```

---

### Task 16: Wire `ViewModelModule` + Update `MainActivity` + Cleanup

**Files:**
- Modify: `app/src/main/java/com/android/zubanx/core/di/ViewModelModule.kt`
- Modify: `app/src/main/java/com/android/zubanx/app/MainActivity.kt`
- Delete: `app/src/main/java/com/android/zubanx/feature/placeholder/PlaceholderFragment.kt`
- Delete: `app/src/main/res/layout/fragment_placeholder.xml`

- [ ] **Step 1: Update `ViewModelModule.kt`**

Replace the full contents of `app/src/main/java/com/android/zubanx/core/di/ViewModelModule.kt`:
```kotlin
package com.android.zubanx.core.di

import com.android.zubanx.feature.onboarding.OnboardingViewModel
import com.android.zubanx.feature.splash.SplashViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::SplashViewModel)
    viewModelOf(::OnboardingViewModel)
}
```

- [ ] **Step 2: Update `MainActivity.kt` hidden destinations**

In `app/src/main/java/com/android/zubanx/app/MainActivity.kt`, replace the `BOTTOM_NAV_HIDDEN_DESTINATIONS` set:
```kotlin
private val BOTTOM_NAV_HIDDEN_DESTINATIONS = setOf(
    R.id.splashFragment,
    R.id.onboardingFragment,
    // R.id.premiumFragment,
    // R.id.wordDetailFragment,
)
```

- [ ] **Step 3: Delete PlaceholderFragment**

First verify no remaining references (Task 15 replaced `nav_onboarding.xml`, so expect no output):
```bash
grep -r "PlaceholderFragment\|fragment_placeholder" app/src/main/
```
Expected: no output. If any references remain, fix them before proceeding.

```bash
git rm app/src/main/java/com/android/zubanx/feature/placeholder/PlaceholderFragment.kt
git rm app/src/main/res/layout/fragment_placeholder.xml
```

- [ ] **Step 4: Build to verify**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Run full unit test suite**

```bash
./gradlew :app:testDebugUnitTest
```
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 6: Commit**

```bash
# Note: the git rm commands in Step 3 already staged the deletions.
# This commit includes the two modified files AND the two deletions.
git add app/src/main/java/com/android/zubanx/core/di/ViewModelModule.kt \
        app/src/main/java/com/android/zubanx/app/MainActivity.kt
git commit -m "feat: wire ViewModelModule, update MainActivity hidden destinations, remove placeholder"
```

- [ ] **Step 7: Tag Plan 4 completion**

```bash
git tag plan-4-splash-onboarding-tts
```

---

## Summary

After completing all chunks, the following are in place:

**New files:**

| File | Purpose |
|---|---|
| `tts/TtsState.kt` | Sealed interface: Idle, Speaking, Error |
| `tts/SttState.kt` | Sealed interface: Idle, Listening, Result, Error |
| `tts/TtsManager.kt` | Android TextToSpeech wrapper, StateFlow<TtsState> |
| `tts/SttManager.kt` | Android SpeechRecognizer wrapper, StateFlow<SttState> |
| `feature/splash/SplashContract.kt` | MVI contract |
| `feature/splash/SplashViewModel.kt` | 2s delay → onboarding check → navigate effect |
| `feature/splash/SplashFragment.kt` | Lottie + MVI navigation |
| `feature/onboarding/OnboardingContract.kt` | MVI contract |
| `feature/onboarding/OnboardingViewModel.kt` | Sets onboardingComplete, emits NavigateToHome |
| `feature/onboarding/OnboardingPagerAdapter.kt` | ViewPager2 RecyclerView.Adapter |
| `feature/onboarding/OnboardingFragment.kt` | 3-page ViewPager2, skip/done, back-press handling |
| `feature/translate/TranslateFragment.kt` | Stub (Plan 5 replaces) |

**Modified files:**

| File | Change |
|---|---|
| `core/di/TtsModule.kt` | TtsManager + SttManager singletons |
| `core/di/ViewModelModule.kt` | SplashViewModel + OnboardingViewModel |
| `res/navigation/nav_onboarding.xml` | SplashFragment + OnboardingFragment + action |
| `res/navigation/nav_translate.xml` | TranslateFragment as startDestination |
| `res/navigation/nav_graph.xml` | Global action onboarding → translate |
| `app/MainActivity.kt` | Splash/Onboarding in hidden destinations |

**Deleted:**
- `feature/placeholder/PlaceholderFragment.kt`
- `res/layout/fragment_placeholder.xml`

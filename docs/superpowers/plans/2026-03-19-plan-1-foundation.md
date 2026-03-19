# ZubanX Foundation Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish the full architectural skeleton for ZubanX — build configuration with all dependencies, core MVI layer, base UI classes, utility extensions, navigation infrastructure, Koin DI module stubs, updated app entry points, Android manifest, animation resources, and stub navigation graphs.

**Architecture:** Contract-based MVI (State/Event/Effect sealed types per screen inside a `Contract` object), single Activity (`MainActivity`) with Navigation Component 2.9.0 NavHostFragment, ViewBinding throughout, Koin 4.1.1 BOM for DI. `BaseViewModel` exposes `StateFlow<S>` and a `Channel<Ef>` (consumed as `Flow`) observed by fragments via `collectFlow()`.

**Tech Stack:** Kotlin 2.1.20, KSP 2.1.20-1.0.31, AGP 9.1.0, Koin BOM 4.1.1, Navigation 2.9.0, Ktor 3.1.2, Room 3.0.0-alpha01, Firebase BOM 33.12.0, DataStore 1.1.4, ML Kit OCR 16.0.1 + Translate 17.0.3, Billing 8.3.0, Coil 2.7.0, Lottie 6.6.6, Timber 5.0.1

---

## File Structure

**Build:**
- Modify: `gradle/libs.versions.toml` — all version refs and library aliases
- Modify: `build.gradle.kts` — root: Kotlin, KSP, Safe Args, Serialization, Firebase plugins
- Modify: `app/build.gradle.kts` — apply plugins, ViewBinding, Room schema dir, all deps

**Core MVI (`core/mvi/`):**
- `UiState.kt` — marker interface
- `UiEvent.kt` — marker interface
- `UiEffect.kt` — marker interface
- `BaseViewModel.kt` — abstract VM with StateFlow + Channel<Effect>

**Base UI (`core/base/`):**
- `BaseActivity.kt` — edge-to-edge, ViewBinding lifecycle
- `BaseFragment.kt` — ViewBinding lifecycle + backPressHandler
- `BaseDialogFragment.kt` — ViewBinding lifecycle for dialogs

**Utils (`core/utils/`):**
- `FragmentExt.kt` — `collectFlow()` using `repeatOnLifecycle`
- `ViewExt.kt` — `show()`, `hide()`, `gone()`, `isVisible`
- `ContextExt.kt` — `toast()`, `dp()`, `colorRes()`
- `LifecycleExt.kt` — `launchWhenStarted()`, `launchWhenResumed()`
- `FlowExt.kt` — `throttleFirst()` operator
- `StringExt.kt` — `isNotNullOrBlank()`, `orDash()`, `capitalizeFirst()`
- `DateTimeUtils.kt` — timestamp formatting utilities
- `ResourceExt.kt` — `stringRes()`, `colorResCompat()`
- `ConnectivityUtils.kt` — `isOnline()` via `ConnectivityManager`

**Navigation (`core/navigation/`):**
- `NavigationExt.kt` — `safeNavigate()`, `popBackTo()`, `getNavigationResult()`, `setNavigationResult()`

**DI (`core/di/`):**
- `NetworkModule.kt` — stub (Plan 3)
- `DatabaseModule.kt` — stub (Plan 2)
- `DataStoreModule.kt` — stub (Plan 2)
- `SecurityModule.kt` — stub (Plan 3)
- `UtilsModule.kt` — registers `ConnectivityUtils`
- `MlKitModule.kt` — stub (Plan 7)
- `RepositoryModule.kt` — stub (Plan 2)
- `UseCaseModule.kt` — stub (per feature plan)
- `ViewModelModule.kt` — stub (per feature plan)
- `TtsModule.kt` — stub (Plan 4)

**App (`app/`):**
- Modify: `ZubanApp.kt` — Koin `startKoin`, Timber `plant`
- Modify: `MainActivity.kt` — NavHostFragment, bottom nav, destination listener

**Services (stubs for manifest compilation):**
- `service/FloatingOverlayService.kt` — stub (Plan 7)
- `service/AccessibilityTranslateService.kt` — stub (Plan 7)

**Placeholder (temporary, replaced in Plan 4):**
- `feature/placeholder/PlaceholderFragment.kt` — empty fragment that gives nav_onboarding a valid startDestination so the app launches without crashing
- `res/layout/fragment_placeholder.xml` — minimal empty layout for PlaceholderFragment

**Manifest & Resources:**
- Modify: `AndroidManifest.xml` — permissions + service declarations
- Modify: `res/layout/activity_main.xml` — CoordinatorLayout + NavHostFragment + BottomNavigationView
- Create: `res/menu/bottom_nav_menu.xml` — 5 bottom nav items
- Create: `res/anim/slide_in_right.xml`, `slide_out_left.xml`, `slide_in_left.xml`, `slide_out_right.xml`
- Create: `res/anim/slide_up.xml`, `slide_down.xml`, `fade_in.xml`, `fade_out.xml`, `scale_fade_in.xml`
- Create: `res/navigation/nav_graph.xml` (root) + 12 stub feature nav graphs
- Create: `res/xml/accessibility_service_config.xml`

---

## Chunk 1: Build Configuration

### Task 1: Update `gradle/libs.versions.toml`

**Files:**
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Replace libs.versions.toml with full dependency catalog**

```toml
[versions]
agp = "9.1.0"
kotlin = "2.1.20"
ksp = "2.1.20-1.0.31"
coreKtx = "1.17.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
appcompat = "1.7.1"
material = "1.13.0"
activity = "1.12.4"
constraintlayout = "2.2.1"
navigation = "2.9.0"
lifecycle = "2.9.0"
coroutines = "1.10.1"
room3 = "3.0.0-alpha01"
koinBom = "4.1.1"
ktor = "3.1.2"
datastore = "1.1.4"
firebaseBom = "33.12.0"
googleServices = "4.4.2"
mlkitOcr = "16.0.1"
mlkitTranslate = "17.0.3"
billing = "8.3.0"
coil = "2.7.0"
lottie = "6.6.6"
timber = "5.0.1"
serializationJson = "1.8.0"
turbine = "1.2.0"
mockk = "1.14.0"

[libraries]
# AndroidX Core
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
androidx-activity = { group = "androidx.activity", name = "activity", version.ref = "activity" }
androidx-constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }
# Navigation
androidx-navigation-fragment = { group = "androidx.navigation", name = "navigation-fragment-ktx", version.ref = "navigation" }
androidx-navigation-ui = { group = "androidx.navigation", name = "navigation-ui-ktx", version.ref = "navigation" }
# Lifecycle
androidx-lifecycle-viewmodel = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-ktx", version.ref = "lifecycle" }
androidx-lifecycle-runtime = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
# Coroutines
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
# Room 3
room3-runtime = { group = "androidx.room3", name = "room-runtime", version.ref = "room3" }
room3-ktx = { group = "androidx.room3", name = "room-ktx", version.ref = "room3" }
room3-compiler = { group = "androidx.room3", name = "room-compiler", version.ref = "room3" }
# Koin
koin-bom = { group = "io.insert-koin", name = "koin-bom", version.ref = "koinBom" }
koin-android = { group = "io.insert-koin", name = "koin-android" }
koin-androidx-navigation = { group = "io.insert-koin", name = "koin-androidx-navigation" }
# Ktor
ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { group = "io.ktor", name = "ktor-client-okhttp", version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-logging = { group = "io.ktor", name = "ktor-client-logging", version.ref = "ktor" }
# DataStore
androidx-datastore = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
# Firebase
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-config = { group = "com.google.firebase", name = "firebase-config-ktx" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics-ktx" }
# ML Kit
mlkit-ocr = { group = "com.google.mlkit", name = "text-recognition", version.ref = "mlkitOcr" }
mlkit-translate = { group = "com.google.mlkit", name = "translate", version.ref = "mlkitTranslate" }
# Billing
billing = { group = "com.android.billingclient", name = "billing-ktx", version.ref = "billing" }
# UI
coil = { group = "io.coil-kt", name = "coil", version.ref = "coil" }
lottie = { group = "com.airbnb.android", name = "lottie", version.ref = "lottie" }
timber = { group = "com.jakewharton.timber", name = "timber", version.ref = "timber" }
# Serialization
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "serializationJson" }
# Test
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
navigation-safeargs = { id = "androidx.navigation.safeargs.kotlin", version.ref = "navigation" }
room3 = { id = "androidx.room3", version.ref = "room3" }
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

---

### Task 2: Update root `build.gradle.kts`

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: Replace root build.gradle.kts with full plugin declarations**

```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.navigation.safeargs) apply false
    alias(libs.plugins.room3) apply false
    alias(libs.plugins.google.services) apply false
}
```

---

### Task 3: Update `app/build.gradle.kts`

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Replace app/build.gradle.kts with full configuration**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.room3)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.android.zubanx"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.android.zubanx"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Navigation
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Room 3
    implementation(libs.room3.runtime)
    implementation(libs.room3.ktx)
    ksp(libs.room3.compiler)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.navigation)

    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.logging)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // DataStore
    implementation(libs.androidx.datastore)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    implementation(libs.firebase.analytics)

    // ML Kit
    implementation(libs.mlkit.ocr)
    implementation(libs.mlkit.translate)

    // Billing
    implementation(libs.billing)

    // UI
    implementation(libs.coil)
    implementation(libs.lottie)
    implementation(libs.timber)

    // Test
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
```

- [ ] **Step 2: Sync Gradle and verify BUILD SUCCESSFUL**

In Android Studio: File → Sync Project with Gradle Files
Expected: BUILD SUCCESSFUL, no unresolved dependency errors

- [ ] **Step 3: Commit build configuration**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts
git commit -m "build: configure all dependencies for ZubanX architecture"
```

---

## Chunk 2: Core MVI Infrastructure

### Task 4: Create MVI marker interfaces

**Files:**
- Create: `app/src/main/java/com/android/zubanx/core/mvi/UiState.kt`
- Create: `app/src/main/java/com/android/zubanx/core/mvi/UiEvent.kt`
- Create: `app/src/main/java/com/android/zubanx/core/mvi/UiEffect.kt`
- Test: `app/src/test/java/com/android/zubanx/core/mvi/MviContractTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
// app/src/test/java/com/android/zubanx/core/mvi/MviContractTest.kt
package com.android.zubanx.core.mvi

import org.junit.Test

class MviContractTest {

    private sealed interface State : UiState {
        object Idle : State
        object Loading : State
    }

    private sealed class Event : UiEvent {
        object Click : Event()
    }

    private sealed class Effect : UiEffect {
        object Navigate : Effect()
    }

    @Test
    fun `UiState can be implemented by sealed interface`() {
        val state: UiState = State.Idle
        assert(state is State.Idle)
    }

    @Test
    fun `UiEvent can be implemented by sealed class`() {
        val event: UiEvent = Event.Click
        assert(event is Event.Click)
    }

    @Test
    fun `UiEffect can be implemented by sealed class`() {
        val effect: UiEffect = Effect.Navigate
        assert(effect is Effect.Navigate)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "*.MviContractTest" 2>&1 | tail -20
```

Expected: FAILED — `UiState`, `UiEvent`, `UiEffect` not found

- [ ] **Step 3: Create UiState.kt**

```kotlin
// app/src/main/java/com/android/zubanx/core/mvi/UiState.kt
package com.android.zubanx.core.mvi

interface UiState
```

- [ ] **Step 4: Create UiEvent.kt**

```kotlin
// app/src/main/java/com/android/zubanx/core/mvi/UiEvent.kt
package com.android.zubanx.core.mvi

interface UiEvent
```

- [ ] **Step 5: Create UiEffect.kt**

```kotlin
// app/src/main/java/com/android/zubanx/core/mvi/UiEffect.kt
package com.android.zubanx.core.mvi

interface UiEffect
```

- [ ] **Step 6: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "*.MviContractTest" 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL, 3 tests passed

---

### Task 5: Create BaseViewModel

**Files:**
- Create: `app/src/main/java/com/android/zubanx/core/mvi/BaseViewModel.kt`
- Test: `app/src/test/java/com/android/zubanx/core/mvi/BaseViewModelTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
// app/src/test/java/com/android/zubanx/core/mvi/BaseViewModelTest.kt
package com.android.zubanx.core.mvi

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    sealed interface TestState : UiState {
        object Idle : TestState
        data class Loaded(val value: String) : TestState
    }

    sealed class TestEvent : UiEvent {
        data class Load(val value: String) : TestEvent()
    }

    sealed class TestEffect : UiEffect {
        data class ShowToast(val msg: String) : TestEffect()
    }

    class TestViewModel : BaseViewModel<TestState, TestEvent, TestEffect>(TestState.Idle) {
        override fun onEvent(event: TestEvent) {
            when (event) {
                is TestEvent.Load -> {
                    setState { TestState.Loaded(event.value) }
                    sendEffect(TestEffect.ShowToast("Loaded: ${event.value}"))
                }
            }
        }
    }

    @Before
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state is Idle`() = runTest {
        val vm = TestViewModel()
        assert(vm.state.value is TestState.Idle)
    }

    @Test
    fun `onEvent Load transitions state to Loaded`() = runTest {
        val vm = TestViewModel()
        vm.state.test {
            assert(awaitItem() is TestState.Idle)
            vm.onEvent(TestEvent.Load("hello"))
            testDispatcher.scheduler.advanceUntilIdle()
            val loaded = awaitItem()
            assert(loaded is TestState.Loaded && (loaded as TestState.Loaded).value == "hello")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onEvent Load sends ShowToast effect`() = runTest {
        val vm = TestViewModel()
        vm.effect.test {
            vm.onEvent(TestEvent.Load("hello"))
            testDispatcher.scheduler.advanceUntilIdle()
            val effect = awaitItem()
            assert(effect is TestEffect.ShowToast && (effect as TestEffect.ShowToast).msg == "Loaded: hello")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "*.BaseViewModelTest" 2>&1 | tail -20
```

Expected: FAILED — `BaseViewModel` not found

- [ ] **Step 3: Create BaseViewModel.kt**

```kotlin
// app/src/main/java/com/android/zubanx/core/mvi/BaseViewModel.kt
package com.android.zubanx.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<S : UiState, E : UiEvent, Ef : UiEffect>(
    initialState: S
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = Channel<Ef>(Channel.BUFFERED)
    val effect: Flow<Ef> = _effect.receiveAsFlow()

    protected fun setState(reducer: S.() -> S) {
        _state.update { it.reducer() }
    }

    protected fun sendEffect(effect: Ef) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    abstract fun onEvent(event: E)
}
```

- [ ] **Step 4: Run all MVI tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "*.MviContractTest" --tests "*.BaseViewModelTest" 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL, 6 tests passed

- [ ] **Step 5: Commit MVI core**

```bash
git add app/src/main/java/com/android/zubanx/core/mvi/ app/src/test/java/com/android/zubanx/core/mvi/
git commit -m "feat(core): add MVI marker interfaces and BaseViewModel"
```

---

## Chunk 3: Base UI Classes

### Task 6: Create BaseActivity

**Files:**
- Create: `app/src/main/java/com/android/zubanx/core/base/BaseActivity.kt`

- [ ] **Step 1: Create BaseActivity.kt**

```kotlin
// app/src/main/java/com/android/zubanx/core/base/BaseActivity.kt
package com.android.zubanx.core.base

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<T : ViewBinding>(
    private val bindingFactory: (LayoutInflater) -> T
) : AppCompatActivity() {

    private var _binding: T? = null
    protected val binding: T
        get() = checkNotNull(_binding) { "ViewBinding accessed after onDestroy" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        _binding = bindingFactory(layoutInflater)
        setContentView(binding.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
```

---

### Task 7: Create BaseFragment

**Files:**
- Create: `app/src/main/java/com/android/zubanx/core/base/BaseFragment.kt`

- [ ] **Step 1: Create BaseFragment.kt**

```kotlin
// app/src/main/java/com/android/zubanx/core/base/BaseFragment.kt
package com.android.zubanx.core.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<T : ViewBinding>(
    private val bindingFactory: (LayoutInflater) -> T
) : Fragment() {

    private var _binding: T? = null
    protected val binding: T
        get() = checkNotNull(_binding) { "ViewBinding accessed outside view lifecycle" }

    /**
     * Set in [setupViews] to intercept back press.
     * Return `true` = consumed (do nothing), `false` = default (pop back stack).
     */
    var backPressHandler: (() -> Boolean)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = bindingFactory(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** Override to initialise views, click listeners, and set [backPressHandler]. */
    open fun setupViews() {}

    /** Override to collect state/effect flows via [com.android.zubanx.core.utils.collectFlow]. */
    open fun observeState() {}
}
```

> **Back press wiring note:** `backPressHandler` is a plain lambda. To consume back presses, fragments must register with `OnBackPressedDispatcher` inside `setupViews()`:
> ```kotlin
> requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
>     if (backPressHandler?.invoke() != true) {
>         isEnabled = false
>         requireActivity().onBackPressedDispatcher.onBackPressed()
>     }
> }
> ```
> The base class intentionally does not register this automatically — it would always intercept back press even when no handler is set.

```kotlin
```

---

### Task 8: Create BaseDialogFragment

**Files:**
- Create: `app/src/main/java/com/android/zubanx/core/base/BaseDialogFragment.kt`

- [ ] **Step 1: Create BaseDialogFragment.kt**

```kotlin
// app/src/main/java/com/android/zubanx/core/base/BaseDialogFragment.kt
package com.android.zubanx.core.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding

abstract class BaseDialogFragment<T : ViewBinding>(
    private val bindingFactory: (LayoutInflater) -> T
) : DialogFragment() {

    private var _binding: T? = null
    protected val binding: T
        get() = checkNotNull(_binding) { "ViewBinding accessed outside view lifecycle" }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = bindingFactory(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    open fun setupViews() {}
}
```

- [ ] **Step 2: Commit base UI classes**

```bash
git add app/src/main/java/com/android/zubanx/core/base/
git commit -m "feat(core): add BaseActivity, BaseFragment, BaseDialogFragment"
```

---

## Chunk 4: Utility Extensions

### Task 9: Create FragmentExt

**Files:**
- Create: `app/src/main/java/com/android/zubanx/core/utils/FragmentExt.kt`

- [ ] **Step 1: Create FragmentExt.kt**

```kotlin
// app/src/main/java/com/android/zubanx/core/utils/FragmentExt.kt
package com.android.zubanx.core.utils

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Collects [flow] safely within the Fragment's view lifecycle (STARTED state).
 * Cancels automatically when the view goes below STARTED; resumes when it returns.
 */
fun <T> Fragment.collectFlow(
    flow: Flow<T>,
    collector: suspend (T) -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { collector(it) }
        }
    }
}
```

---

### Task 10: Create ViewExt

**Files:**
- Create: `app/src/main/java/com/android/zubanx/core/utils/ViewExt.kt`

- [ ] **Step 1: Create ViewExt.kt**

```kotlin
// app/src/main/java/com/android/zubanx/core/utils/ViewExt.kt
package com.android.zubanx.core.utils

import android.view.View

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.INVISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

var View.isVisible: Boolean
    get() = visibility == View.VISIBLE
    set(value) { visibility = if (value) View.VISIBLE else View.GONE }
```

---

### Task 11: Create ContextExt, LifecycleExt, FlowExt, StringExt, DateTimeUtils, ResourceExt

**Files:**
- Create: `app/src/main/java/com/android/zubanx/core/utils/ContextExt.kt`
- Create: `app/src/main/java/com/android/zubanx/core/utils/LifecycleExt.kt`
- Create: `app/src/main/java/com/android/zubanx/core/utils/FlowExt.kt`
- Create: `app/src/main/java/com/android/zubanx/core/utils/StringExt.kt`
- Create: `app/src/main/java/com/android/zubanx/core/utils/DateTimeUtils.kt`
- Create: `app/src/main/java/com/android/zubanx/core/utils/ResourceExt.kt`

- [ ] **Step 1: Create ContextExt.kt**

```kotlin
// app/src/main/java/com/android/zubanx/core/utils/ContextExt.kt
package com.android.zubanx.core.utils

import android.content.Context
import android.util.TypedValue
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.dp(value: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics)

fun Context.colorRes(@ColorRes res: Int): Int =
    ContextCompat.getColor(this, res)
```

- [ ] **Step 2: Create LifecycleExt.kt**

```kotlin
// app/src/main/java/com/android/zubanx/core/utils/LifecycleExt.kt
package com.android.zubanx.core.utils

import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// NOTE: These are simplified shims. Unlike the deprecated lifecycle-aware variants,
// they do NOT suspend until a specific lifecycle state — they launch immediately.
// For lifecycle-aware flow collection use collectFlow() (FragmentExt) or
// repeatOnLifecycle() directly. These exist only as call-site convenience wrappers.
fun LifecycleCoroutineScope.launchWhenStarted(block: suspend () -> Unit): Job =
    launch { block() }

fun LifecycleCoroutineScope.launchWhenResumed(block: suspend () -> Unit): Job =
    launch { block() }
```

- [ ] **Step 3: Create FlowExt.kt**

```kotlin
// app/src/main/java/com/android/zubanx/core/utils/FlowExt.kt
package com.android.zubanx.core.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

/**
 * Emits the first item in each [windowDuration] window, dropping subsequent items.
 * Useful for preventing double-tap actions.
 */
fun <T> Flow<T>.throttleFirst(windowDuration: Duration): Flow<T> = flow {
    var lastEmitTime = 0L
    collect { value ->
        val now = System.currentTimeMillis()
        if (now - lastEmitTime >= windowDuration.inWholeMilliseconds) {
            lastEmitTime = now
            emit(value)
        }
    }
}
```

- [ ] **Step 4: Create StringExt.kt**

```kotlin
// app/src/main/java/com/android/zubanx/core/utils/StringExt.kt
package com.android.zubanx.core.utils

fun String?.isNotNullOrBlank(): Boolean = !this.isNullOrBlank()

/** Returns "—" if null or blank, otherwise the string itself. */
fun String?.orDash(): String = if (isNullOrBlank()) "—" else this!!

fun String.capitalizeFirst(): String =
    if (isEmpty()) this else this[0].uppercaseChar() + substring(1)
```

- [ ] **Step 5: Create DateTimeUtils.kt**

```kotlin
// app/src/main/java/com/android/zubanx/core/utils/DateTimeUtils.kt
package com.android.zubanx.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {
    private val displayFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    fun formatTimestamp(timestampMs: Long): String =
        displayFormat.format(Date(timestampMs))

    fun formatDateOnly(timestampMs: Long): String =
        dateOnlyFormat.format(Date(timestampMs))

    fun now(): Long = System.currentTimeMillis()
}
```

- [ ] **Step 6: Create ResourceExt.kt**

```kotlin
// app/src/main/java/com/android/zubanx/core/utils/ResourceExt.kt
package com.android.zubanx.core.utils

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

fun Context.stringRes(@StringRes res: Int, vararg args: Any): String =
    if (args.isEmpty()) getString(res) else getString(res, *args)

fun Context.colorResCompat(@ColorRes res: Int): Int =
    ContextCompat.getColor(this, res)
```

---

### Task 12: Create ConnectivityUtils

**Files:**
- Create: `app/src/main/java/com/android/zubanx/core/utils/ConnectivityUtils.kt`
- Test: `app/src/test/java/com/android/zubanx/core/utils/ConnectivityUtilsTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
// app/src/test/java/com/android/zubanx/core/utils/ConnectivityUtilsTest.kt
package com.android.zubanx.core.utils

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

class ConnectivityUtilsTest {

    @Test
    fun `isOnline returns true when network has INTERNET capability`() {
        val connectivityManager = mockk<ConnectivityManager>()
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        val utils = ConnectivityUtils(connectivityManager)

        assert(utils.isOnline())
    }

    @Test
    fun `isOnline returns false when no active network`() {
        val connectivityManager = mockk<ConnectivityManager>()
        every { connectivityManager.activeNetwork } returns null

        val utils = ConnectivityUtils(connectivityManager)

        assert(!utils.isOnline())
    }

    @Test
    fun `isOnline returns false when capabilities are null`() {
        val connectivityManager = mockk<ConnectivityManager>()
        val network = mockk<Network>()
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns null

        val utils = ConnectivityUtils(connectivityManager)

        assert(!utils.isOnline())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "*.ConnectivityUtilsTest" 2>&1 | tail -15
```

Expected: FAILED — `ConnectivityUtils` not found

- [ ] **Step 3: Create ConnectivityUtils.kt**

```kotlin
// app/src/main/java/com/android/zubanx/core/utils/ConnectivityUtils.kt
package com.android.zubanx.core.utils

import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class ConnectivityUtils(
    private val connectivityManager: ConnectivityManager
) {
    fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
```

- [ ] **Step 4: Run all utils tests to verify they pass**

```bash
./gradlew :app:testDebugUnitTest --tests "*.ConnectivityUtilsTest" 2>&1 | tail -10
```

Expected: BUILD SUCCESSFUL, 3 tests passed

- [ ] **Step 5: Commit utils**

```bash
git add app/src/main/java/com/android/zubanx/core/utils/ app/src/test/java/com/android/zubanx/core/utils/
git commit -m "feat(core): add utility extensions (Fragment, View, Context, String, DateTime, Connectivity)"
```

---

## Chunk 5: Navigation Infrastructure

### Task 13: Create NavigationExt

**Files:**
- Create: `app/src/main/java/com/android/zubanx/core/navigation/NavigationExt.kt`

- [ ] **Step 1: Create NavigationExt.kt**

```kotlin
// app/src/main/java/com/android/zubanx/core/navigation/NavigationExt.kt
package com.android.zubanx.core.navigation

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.findNavController

/**
 * Navigate safely — checks the action exists on the current destination before
 * navigating, preventing crashes from rapid double-taps.
 */
fun Fragment.safeNavigate(directions: NavDirections) {
    val navController = findNavController()
    val action = navController.currentDestination?.getAction(directions.actionId)
    if (action != null) {
        navController.navigate(directions)
    }
}

/**
 * Pop back stack to [destinationId].
 * @param inclusive If true, also pops [destinationId] itself.
 */
fun Fragment.popBackTo(@IdRes destinationId: Int, inclusive: Boolean = false) {
    findNavController().popBackStack(destinationId, inclusive)
}

/**
 * Read a result that was left by the screen this fragment navigated to via [setNavigationResult].
 */
fun <T> Fragment.getNavigationResult(key: String): T? =
    findNavController().currentBackStackEntry
        ?.savedStateHandle
        ?.get<T>(key)

/**
 * Deliver a result to the fragment that opened this screen.
 */
fun <T> Fragment.setNavigationResult(key: String, value: T) {
    findNavController().previousBackStackEntry
        ?.savedStateHandle
        ?.set(key, value)
}
```

- [ ] **Step 2: Commit navigation extensions**

```bash
git add app/src/main/java/com/android/zubanx/core/navigation/
git commit -m "feat(core): add NavigationExt (safeNavigate, popBackTo, navigation result helpers)"
```

---

## Chunk 6: Koin DI Module Stubs

### Task 14: Create all DI module stubs

**Files:**
- Create: `app/src/main/java/com/android/zubanx/core/di/NetworkModule.kt`
- Create: `app/src/main/java/com/android/zubanx/core/di/DatabaseModule.kt`
- Create: `app/src/main/java/com/android/zubanx/core/di/DataStoreModule.kt`
- Create: `app/src/main/java/com/android/zubanx/core/di/SecurityModule.kt`
- Create: `app/src/main/java/com/android/zubanx/core/di/UtilsModule.kt`
- Create: `app/src/main/java/com/android/zubanx/core/di/MlKitModule.kt`
- Create: `app/src/main/java/com/android/zubanx/core/di/RepositoryModule.kt`
- Create: `app/src/main/java/com/android/zubanx/core/di/UseCaseModule.kt`
- Create: `app/src/main/java/com/android/zubanx/core/di/ViewModelModule.kt`
- Create: `app/src/main/java/com/android/zubanx/core/di/TtsModule.kt`

- [ ] **Step 1: Create NetworkModule.kt (stub — filled in Plan 3)**

```kotlin
// app/src/main/java/com/android/zubanx/core/di/NetworkModule.kt
package com.android.zubanx.core.di

import org.koin.dsl.module

// Populated in Plan 3: Network Layer
val networkModule = module {
}
```

- [ ] **Step 2: Create DatabaseModule.kt (stub — filled in Plan 2)**

```kotlin
// app/src/main/java/com/android/zubanx/core/di/DatabaseModule.kt
package com.android.zubanx.core.di

import org.koin.dsl.module

// Populated in Plan 2: Data Layer
val databaseModule = module {
}
```

- [ ] **Step 3: Create DataStoreModule.kt (stub — filled in Plan 2)**

```kotlin
// app/src/main/java/com/android/zubanx/core/di/DataStoreModule.kt
package com.android.zubanx.core.di

import org.koin.dsl.module

// Populated in Plan 2: Data Layer
val dataStoreModule = module {
}
```

- [ ] **Step 4: Create SecurityModule.kt (stub — filled in Plan 3)**

```kotlin
// app/src/main/java/com/android/zubanx/core/di/SecurityModule.kt
package com.android.zubanx.core.di

import org.koin.dsl.module

// Populated in Plan 3: Network Layer
val securityModule = module {
}
```

- [ ] **Step 5: Create UtilsModule.kt**

```kotlin
// app/src/main/java/com/android/zubanx/core/di/UtilsModule.kt
package com.android.zubanx.core.di

import android.content.Context
import android.net.ConnectivityManager
import com.android.zubanx.core.utils.ConnectivityUtils
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val utilsModule = module {
    single<ConnectivityManager> {
        androidContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    singleOf(::ConnectivityUtils)
}
```

- [ ] **Step 6: Create MlKitModule.kt (stub — filled in Plan 7)**

```kotlin
// app/src/main/java/com/android/zubanx/core/di/MlKitModule.kt
package com.android.zubanx.core.di

import org.koin.dsl.module

// Populated in Plan 7: Visual Features
val mlKitModule = module {
}
```

- [ ] **Step 7: Create RepositoryModule.kt (stub — filled in Plan 2)**

```kotlin
// app/src/main/java/com/android/zubanx/core/di/RepositoryModule.kt
package com.android.zubanx.core.di

import org.koin.dsl.module

// Populated in Plan 2: Data Layer
val repositoryModule = module {
}
```

- [ ] **Step 8: Create UseCaseModule.kt (stub — filled per feature plan)**

```kotlin
// app/src/main/java/com/android/zubanx/core/di/UseCaseModule.kt
package com.android.zubanx.core.di

import org.koin.dsl.module

// Populated per feature plan
val useCaseModule = module {
}
```

- [ ] **Step 9: Create ViewModelModule.kt (stub — filled per feature plan)**

```kotlin
// app/src/main/java/com/android/zubanx/core/di/ViewModelModule.kt
package com.android.zubanx.core.di

import org.koin.dsl.module

// Populated per feature plan
val viewModelModule = module {
}
```

- [ ] **Step 10: Create TtsModule.kt (stub — filled in Plan 4)**

```kotlin
// app/src/main/java/com/android/zubanx/core/di/TtsModule.kt
package com.android.zubanx.core.di

import org.koin.dsl.module

// Populated in Plan 4: TTS/STT
val ttsModule = module {
}
```

- [ ] **Step 11: Commit DI module stubs**

```bash
git add app/src/main/java/com/android/zubanx/core/di/
git commit -m "feat(di): add Koin module stubs (network, db, datastore, security, utils, mlkit, repo, usecase, viewmodel, tts)"
```

---

## Chunk 7: App Entry Point

### Task 15: Update ZubanApp

**Files:**
- Modify: `app/src/main/java/com/android/zubanx/app/ZubanApp.kt`

- [ ] **Step 1: Replace ZubanApp.kt**

```kotlin
// app/src/main/java/com/android/zubanx/app/ZubanApp.kt
package com.android.zubanx.app

import android.app.Application
import com.android.zubanx.BuildConfig
import com.android.zubanx.core.di.databaseModule
import com.android.zubanx.core.di.dataStoreModule
import com.android.zubanx.core.di.mlKitModule
import com.android.zubanx.core.di.networkModule
import com.android.zubanx.core.di.repositoryModule
import com.android.zubanx.core.di.securityModule
import com.android.zubanx.core.di.ttsModule
import com.android.zubanx.core.di.useCaseModule
import com.android.zubanx.core.di.utilsModule
import com.android.zubanx.core.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class ZubanApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initTimber()
        initKoin()
    }

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun initKoin() {
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@ZubanApp)
            modules(
                networkModule,
                databaseModule,
                dataStoreModule,
                securityModule,
                utilsModule,
                mlKitModule,
                repositoryModule,
                useCaseModule,
                viewModelModule,
                ttsModule
            )
        }
    }
}
```

---

### Task 16: Update MainActivity and activity layout

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Create: `app/src/main/res/menu/bottom_nav_menu.xml`
- Modify: `app/src/main/java/com/android/zubanx/app/MainActivity.kt`

- [ ] **Step 1: Replace activity_main.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.fragment.app.FragmentContainerView
        android:id="@+id/nav_host_fragment"
        android:name="androidx.navigation.fragment.NavHostFragment"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:defaultNavHost="true"
        app:navGraph="@navigation/nav_graph" />

    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottom_nav"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_gravity="bottom"
        app:menu="@menu/bottom_nav_menu" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

- [ ] **Step 2: Create bottom_nav_menu.xml**

> Note: `@drawable/ic_launcher_foreground` is a temporary placeholder. Replace with dedicated vector icons in each feature plan.

```xml
<?xml version="1.0" encoding="utf-8"?>
<menu xmlns:android="http://schemas.android.com/apk/res/android">

    <item
        android:id="@+id/nav_translate"
        android:icon="@drawable/ic_launcher_foreground"
        android:title="Translate" />

    <item
        android:id="@+id/nav_conversation"
        android:icon="@drawable/ic_launcher_foreground"
        android:title="Conversation" />

    <item
        android:id="@+id/nav_dictionary"
        android:icon="@drawable/ic_launcher_foreground"
        android:title="Dictionary" />

    <item
        android:id="@+id/nav_favourite"
        android:icon="@drawable/ic_launcher_foreground"
        android:title="Favourites" />

    <item
        android:id="@+id/nav_settings"
        android:icon="@drawable/ic_launcher_foreground"
        android:title="Settings" />

</menu>
```

- [ ] **Step 3: Replace MainActivity.kt**

```kotlin
// app/src/main/java/com/android/zubanx/app/MainActivity.kt
package com.android.zubanx.app

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.android.zubanx.R
import com.android.zubanx.core.base.BaseActivity
import com.android.zubanx.core.utils.isVisible
import com.android.zubanx.databinding.ActivityMainBinding

class MainActivity : BaseActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.isVisible = destination.id !in BOTTOM_NAV_HIDDEN_DESTINATIONS
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp() || super.onSupportNavigateUp()

    companion object {
        /**
         * Fragment destination IDs where the bottom navigation bar is hidden.
         * Uncomment each ID as the corresponding fragment is added in feature plans.
         */
        private val BOTTOM_NAV_HIDDEN_DESTINATIONS = setOf<Int>(
            // R.id.splashFragment,
            // R.id.onboardingFragment,
            // R.id.premiumFragment,
            // R.id.wordDetailFragment,
        )
    }
}
```

- [ ] **Step 4: Commit app entry point**

```bash
git add app/src/main/java/com/android/zubanx/app/ app/src/main/res/layout/ app/src/main/res/menu/
git commit -m "feat(app): ZubanApp with Koin+Timber init, MainActivity with NavHostFragment + BottomNav"
```

---

## Chunk 8: Manifest, Animations, Navigation Graphs & Service Stubs

### Task 17: Update AndroidManifest.xml

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/xml/accessibility_service_config.xml`

- [ ] **Step 1: Replace AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Network -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- STT / Conversation -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />

    <!-- Image Text Translate (camera input) -->
    <uses-permission android:name="android.permission.CAMERA" />

    <!-- On-Screen Translate — floating overlay -->
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

    <!-- Foreground service for FloatingOverlayService -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

    <application
        android:name=".app.ZubanApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.ZubanX">

        <activity
            android:name=".app.MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Floating Overlay Foreground Service -->
        <service
            android:name=".service.FloatingOverlayService"
            android:foregroundServiceType="specialUse"
            android:exported="false" />

        <!-- Required for specialUse foreground service — Android 14+ / Play Store compliance -->
        <property
            android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
            android:value="overlay_translation" />

        <!-- Accessibility Service -->
        <service
            android:name=".service.AccessibilityTranslateService"
            android:exported="true"
            android:label="@string/accessibility_service_label"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

    </application>

</manifest>
```

- [ ] **Step 2: Add accessibility_service_label to strings.xml**

```xml
<resources>
    <string name="app_name">ZubanX</string>
    <string name="accessibility_service_label">ZubanX On-Screen Translate</string>
</resources>
```

- [ ] **Step 3: Create accessibility_service_config.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<accessibility-service
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeAllMask"
    android:accessibilityFeedbackType="feedbackAllMask"
    android:accessibilityFlags="flagDefault"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_label"
    android:notificationTimeout="100"
    android:settingsActivity=".app.MainActivity" />
```

---

### Task 18: Create service stub classes

**Files:**
- Create: `app/src/main/java/com/android/zubanx/service/FloatingOverlayService.kt`
- Create: `app/src/main/java/com/android/zubanx/service/AccessibilityTranslateService.kt`

- [ ] **Step 1: Create FloatingOverlayService.kt (stub)**

```kotlin
// app/src/main/java/com/android/zubanx/service/FloatingOverlayService.kt
package com.android.zubanx.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

// Full implementation in Plan 7: Visual Features
class FloatingOverlayService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
```

- [ ] **Step 2: Create AccessibilityTranslateService.kt (stub)**

```kotlin
// app/src/main/java/com/android/zubanx/service/AccessibilityTranslateService.kt
package com.android.zubanx.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

// Full implementation in Plan 7: Visual Features
class AccessibilityTranslateService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
```

---

### Task 19: Create animation resources

**Files:**
- Create: `app/src/main/res/anim/slide_in_right.xml`
- Create: `app/src/main/res/anim/slide_out_left.xml`
- Create: `app/src/main/res/anim/slide_in_left.xml`
- Create: `app/src/main/res/anim/slide_out_right.xml`
- Create: `app/src/main/res/anim/slide_up.xml`
- Create: `app/src/main/res/anim/slide_down.xml`
- Create: `app/src/main/res/anim/fade_in.xml`
- Create: `app/src/main/res/anim/fade_out.xml`
- Create: `app/src/main/res/anim/scale_fade_in.xml`

- [ ] **Step 1: Create slide_in_right.xml (forward enter)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<translate xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="300"
    android:fromXDelta="100%"
    android:interpolator="@android:anim/decelerate_interpolator"
    android:toXDelta="0%" />
```

- [ ] **Step 2: Create slide_out_left.xml (forward exit)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<translate xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="300"
    android:fromXDelta="0%"
    android:interpolator="@android:anim/accelerate_decelerate_interpolator"
    android:toXDelta="-100%" />
```

- [ ] **Step 3: Create slide_in_left.xml (back enter)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<translate xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="300"
    android:fromXDelta="-100%"
    android:interpolator="@android:anim/decelerate_interpolator"
    android:toXDelta="0%" />
```

- [ ] **Step 4: Create slide_out_right.xml (back exit)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<translate xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="300"
    android:fromXDelta="0%"
    android:interpolator="@android:anim/accelerate_decelerate_interpolator"
    android:toXDelta="100%" />
```

- [ ] **Step 5: Create slide_up.xml (modal/bottom-sheet enter)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<translate xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="300"
    android:fromYDelta="100%"
    android:interpolator="@android:anim/decelerate_interpolator"
    android:toYDelta="0%" />
```

- [ ] **Step 6: Create slide_down.xml (modal/bottom-sheet exit)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<translate xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="300"
    android:fromYDelta="0%"
    android:interpolator="@android:anim/accelerate_decelerate_interpolator"
    android:toYDelta="100%" />
```

- [ ] **Step 7: Create fade_in.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<alpha xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="300"
    android:fromAlpha="0.0"
    android:interpolator="@android:anim/accelerate_decelerate_interpolator"
    android:toAlpha="1.0" />
```

- [ ] **Step 8: Create fade_out.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<alpha xmlns:android="http://schemas.android.com/apk/res/android"
    android:duration="300"
    android:fromAlpha="1.0"
    android:interpolator="@android:anim/accelerate_decelerate_interpolator"
    android:toAlpha="0.0" />
```

- [ ] **Step 9: Create scale_fade_in.xml (splash → home transition)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<set xmlns:android="http://schemas.android.com/apk/res/android">
    <scale
        android:duration="400"
        android:fromXScale="0.85"
        android:fromYScale="0.85"
        android:interpolator="@android:anim/decelerate_interpolator"
        android:pivotX="50%"
        android:pivotY="50%"
        android:toXScale="1.0"
        android:toYScale="1.0" />
    <alpha
        android:duration="400"
        android:fromAlpha="0.0"
        android:interpolator="@android:anim/decelerate_interpolator"
        android:toAlpha="1.0" />
</set>
```

---

### Task 20: Create navigation graphs (stubs)

**Files:**
- Create: `app/src/main/res/navigation/nav_graph.xml`
- Create: `app/src/main/res/navigation/nav_onboarding.xml`
- Create: `app/src/main/res/navigation/nav_translate.xml`
- Create: `app/src/main/res/navigation/nav_conversation.xml`
- Create: `app/src/main/res/navigation/nav_dictionary.xml`
- Create: `app/src/main/res/navigation/nav_idioms.xml`
- Create: `app/src/main/res/navigation/nav_phrases.xml`
- Create: `app/src/main/res/navigation/nav_story.xml`
- Create: `app/src/main/res/navigation/nav_settings.xml`
- Create: `app/src/main/res/navigation/nav_premium.xml`
- Create: `app/src/main/res/navigation/nav_favourite.xml`
- Create: `app/src/main/res/navigation/nav_onscreen.xml`
- Create: `app/src/main/res/navigation/nav_imagetext.xml`

- [ ] **Step 1: Create root nav_graph.xml**

> Note: `startDestination` points to `nav_onboarding`. Update to `nav_translate` after onboarding is gated by DataStore flag in Plan 4.

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

</navigation>
```

- [ ] **Step 2: Create PlaceholderFragment.kt and its layout (temporary — replaced in Plan 4)**

> **Why:** The root nav graph's `startDestination` is `nav_onboarding`. Navigation Component requires that a graph referenced as `startDestination` contains at least one destination with its own `startDestination`. Without this, the app crashes immediately at runtime with `NavigationException`. This placeholder is removed when Plan 4 adds `SplashFragment`.

```xml
<!-- app/src/main/res/layout/fragment_placeholder.xml -->
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

```kotlin
// app/src/main/java/com/android/zubanx/feature/placeholder/PlaceholderFragment.kt
package com.android.zubanx.feature.placeholder

import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.databinding.FragmentPlaceholderBinding

// Temporary — replaced by SplashFragment in Plan 4: Splash & Onboarding
class PlaceholderFragment : BaseFragment<FragmentPlaceholderBinding>(
    FragmentPlaceholderBinding::inflate
)
```

- [ ] **Step 3: Create nav_onboarding.xml with PlaceholderFragment as startDestination**

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_onboarding"
    android:label="Onboarding"
    app:startDestination="@id/placeholderFragment">

    <fragment
        android:id="@+id/placeholderFragment"
        android:name="com.android.zubanx.feature.placeholder.PlaceholderFragment"
        android:label="Placeholder" />

    <!-- SplashFragment and OnboardingFragment added in Plan 4 — remove placeholderFragment then -->
</navigation>
```

- [ ] **Step 4: Create nav_translate.xml (stub)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/nav_translate"
    android:label="Translate">
    <!-- Fragments added in Plan 5: Translate & Conversation -->
</navigation>
```

- [ ] **Step 5: Create nav_conversation.xml (stub)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/nav_conversation"
    android:label="Conversation">
    <!-- Fragments added in Plan 5: Translate & Conversation -->
</navigation>
```

- [ ] **Step 6: Create nav_dictionary.xml (stub)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/nav_dictionary"
    android:label="Dictionary">
    <!-- Fragments added in Plan 6: Dictionary & Content Features -->
</navigation>
```

- [ ] **Step 7: Create nav_idioms.xml (stub)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/nav_idioms"
    android:label="Idioms">
    <!-- Fragments added in Plan 6: Dictionary & Content Features -->
</navigation>
```

- [ ] **Step 8: Create nav_phrases.xml (stub)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/nav_phrases"
    android:label="Phrases">
    <!-- Fragments added in Plan 6: Dictionary & Content Features -->
</navigation>
```

- [ ] **Step 9: Create nav_story.xml (stub)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/nav_story"
    android:label="Story">
    <!-- Fragments added in Plan 6: Dictionary & Content Features -->
</navigation>
```

- [ ] **Step 10: Create nav_settings.xml (stub)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/nav_settings"
    android:label="Settings">
    <!-- Fragments added in Plan 8: Favourites, Settings & Premium -->
</navigation>
```

- [ ] **Step 11: Create nav_premium.xml (stub)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/nav_premium"
    android:label="Premium">
    <!-- Fragments added in Plan 8: Favourites, Settings & Premium -->
</navigation>
```

- [ ] **Step 12: Create nav_favourite.xml (stub)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/nav_favourite"
    android:label="Favourite">
    <!-- Fragments added in Plan 8: Favourites, Settings & Premium -->
</navigation>
```

- [ ] **Step 13: Create nav_onscreen.xml (stub)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/nav_onscreen"
    android:label="On-Screen Translate">
    <!-- Fragments added in Plan 7: Visual Features -->
</navigation>
```

- [ ] **Step 14: Create nav_imagetext.xml (stub)**

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/nav_imagetext"
    android:label="Image Text Translate">
    <!-- Fragments added in Plan 7: Visual Features -->
</navigation>
```

- [ ] **Step 15: Build and verify project compiles cleanly**

```bash
./gradlew :app:assembleDebug 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL — APK at `app/build/outputs/apk/debug/app-debug.apk`. The app launches to a blank white screen (PlaceholderFragment) until Plan 4 replaces it with Splash/Onboarding.

- [ ] **Step 16: Run all unit tests**

```bash
./gradlew :app:testDebugUnitTest 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL, all tests pass (MviContractTest x3, BaseViewModelTest x3, ConnectivityUtilsTest x3 = 9 tests)

- [ ] **Step 17: Commit manifest, service stubs, animations, nav graphs, placeholder**

```bash
git add app/src/main/AndroidManifest.xml \
        app/src/main/res/values/strings.xml \
        app/src/main/res/xml/accessibility_service_config.xml \
        app/src/main/java/com/android/zubanx/service/ \
        app/src/main/java/com/android/zubanx/feature/placeholder/ \
        app/src/main/res/layout/fragment_placeholder.xml \
        app/src/main/res/anim/ \
        app/src/main/res/navigation/
git commit -m "feat(foundation): manifest, service stubs, animation resources, navigation graph stubs, placeholder fragment"
```

---

## Subsequent Plans

This plan produces a compilable skeleton with all architectural infrastructure in place. The following plans implement each layer in dependency order:

| Plan | File | Scope |
|---|---|---|
| **Plan 2: Data Layer** | `2026-03-19-plan-2-data-layer.md` | Room 3 entities/DAOs/DB, DataStore, `AppPreferences`, repository interfaces + impls, `databaseModule`, `dataStoreModule`, `repositoryModule` |
| **Plan 3: Network Layer** | `2026-03-19-plan-3-network-layer.md` | `KtorClientFactory`, `safeApiCall`, `NetworkResult`, `TranslateApiService`, `DictionaryApiService`, `AiExpertService`, `KeyDecryptionModule`, `networkModule`, `securityModule` |
| **Plan 4: TTS/STT + Splash/Onboarding** | `2026-03-19-plan-4-tts-splash-onboarding.md` | `TtsManager`, `SttManager`, `ttsModule`, `SplashFragment`, `OnboardingFragment`, onboarding gating via DataStore |
| **Plan 5: Translate + Conversation** | `2026-03-19-plan-5-translate-conversation.md` | `TranslateFragment` + Contract + ViewModel + UseCase, `ConversationFragment` dual-panel mic flow |
| **Plan 6: Dictionary + Content Features** | `2026-03-19-plan-6-dictionary-content.md` | `DictionaryFragment` + `WordDetailFragment`, `IdiomsFragment`, `PhrasesFragment`, `StoryFragment` |
| **Plan 7: Visual Features** | `2026-03-19-plan-7-visual-features.md` | `OnScreenFragment`, `FloatingOverlayService` (full), `AccessibilityTranslateService` (full), `ImageTextFragment`, `MlKitOcrService`, `mlKitModule` |
| **Plan 8: Favourites + Settings + Premium** | `2026-03-19-plan-8-favourites-settings-premium.md` | `FavouriteFragment`, `SettingsFragment`, `ThemeSelectFragment`, `AboutFragment`, `PremiumFragment`, Google Billing 8.3.0 IAP flow |
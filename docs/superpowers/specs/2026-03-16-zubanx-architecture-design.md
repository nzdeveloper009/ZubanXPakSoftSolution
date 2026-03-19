# ZubanX Architecture Design Spec

**Date:** 2026-03-16
**Project:** ZubanX — AI-powered Translator App
**Package:** `com.android.zubanx`
**Status:** Approved

---

## 1. Overview

ZubanX is a single-module Android translator app with features including Text Translate, Conversation Translator, Dictionary, Idioms, Phrases, Story, On-Screen Translate, Image Text Translate, Offline Mode, TTS/STT, Translate Expert (GPT/Gemini/Claude/Default), Favourites, Premium/IAP, and Settings.

The architecture follows **MVI (Model-View-Intent)** with **Koin DI**, **Room 3**, and **DataStore**, using a single Activity with Navigation Component.

---

## 2. Architecture

### Pattern: Contract-based MVI

Each screen defines a `Contract` object with:
- `sealed interface State : UiState` — distinct screen states (Idle, Loading, Success, Error)
- `sealed class Event : UiEvent` — user actions sent to ViewModel
- `sealed class Effect : UiEffect` — one-time side effects (navigation, toast, TTS)

**Data flow:** `Fragment → Event → ViewModel → setState() / sendEffect() → Fragment`

### Base Classes

```kotlin
abstract class BaseFragment<T : ViewBinding>(private val bindingFactory: (LayoutInflater) -> T) : Fragment()
abstract class BaseActivity<T : ViewBinding>(private val bindingFactory: (LayoutInflater) -> T) : AppCompatActivity()
abstract class BaseViewModel<S : UiState, E : UiEvent, Ef : UiEffect>(initialState: S) : ViewModel()
```

- `BaseFragment` handles: ViewBinding lifecycle, back press via `backPressHandler: (() -> Boolean)?`
- `BaseActivity` handles: edge-to-edge, ViewBinding lifecycle
- `BaseViewModel` exposes: `StateFlow<S>` for state, `Channel<Ef>` (as Flow) for effects
- MVI observation done in each Fragment via `collectFlow()` utility from `FragmentExt.kt`

### Back Press

Each Fragment sets `backPressHandler` in `setupViews()`. Returns `true` = consumed, `false` = default behavior. Supports: navigation, panel close, dialog show, or blocking.

---

## 3. Module Structure

**Single app module.** Hybrid package structure: shared `data/` and `domain/` layers, feature-based `feature/` UI packages.

```
com.android.zubanx/
├── app/                    # ZubanApp (Koin init), MainActivity
├── core/
│   ├── mvi/                # UiState, UiEvent, UiEffect, BaseViewModel
│   ├── base/               # BaseActivity, BaseFragment, BaseDialogFragment
│   ├── di/                 # All Koin modules
│   ├── navigation/         # NavigationExt (safeNavigate, popBackTo, getNavigationResult)
│   ├── network/            # KtorClientFactory, NetworkResult, safeApiCall
│   └── utils/              # LifecycleExt, FlowExt, FragmentExt, ViewExt, ContextExt,
│                           # DateTimeUtils, StringExt, ResourceExt, ConnectivityUtils
├── data/
│   ├── local/
│   │   ├── db/             # ZubanDatabase, entities, DAOs
│   │   └── datastore/      # AppPreferences interface + impl
│   ├── remote/
│   │   ├── api/            # TranslateApiService, DictionaryApiService, AiExpertService
│   │   └── dto/            # Network response data classes
│   ├── mlkit/              # MlKitOcrService (on-device, not remote)
│   └── repository/         # Repository implementations
├── domain/
│   ├── model/              # Pure Kotlin domain models
│   ├── repository/         # Repository interfaces
│   └── usecase/            # UseCases grouped by feature
├── feature/
│   ├── splash/
│   ├── onboarding/
│   ├── translate/
│   ├── conversation/
│   ├── dictionary/
│   ├── idioms/
│   ├── phrases/
│   ├── story/
│   ├── onscreen/
│   ├── imagetext/
│   ├── favourite/
│   ├── premium/
│   └── settings/
├── service/                # AccessibilityTranslateService, FloatingOverlayService
├── security/               # KeyDecryptionModule
└── tts/                    # TtsManager, SttManager
```

---

## 4. Data Layer

### Room 3 (`3.0.0-alpha01`)

- **Artifact group:** `androidx.room3` (new in Room 3)
- **Gradle plugin:** `androidx.room3` with `schemaDirectory`
- **KSP only** — no KAPT
- All DAO functions must be `suspend` or return `Flow`

**Entities:** `TranslationEntity`, `FavouriteEntity`, `DictionaryEntity`, `OfflineLanguagePackEntity`
**DAOs:** `TranslationDao`, `FavouriteDao`, `DictionaryDao`, `OfflineLanguagePackDao`

### DataStore

`AppPreferences` interface backed by `DataStore<Preferences>`. Stores: theme, selectedExpert, sourceLang, targetLang, isPremium, offlineMode, onboardingComplete.

### Repository Pattern

- Interface in `domain/repository/`
- Implementation in `data/repository/`
- Bound via Koin: `singleOf(::RepoImpl) bind Repository::class`
- Cache-first strategy: Room cache → connectivity check → API call → cache result

### Offline Mode

Two-layer strategy:
1. **ML Kit Translate** — downloadable language packs for fresh offline translations
2. **Room cache** — previously translated content served without network

---

## 5. Network Layer

**Ktor 3.1.2** with OkHttp engine.

```kotlin
safeApiCall { ... } // returns NetworkResult<T>
sealed interface NetworkResult<out T> { Success, Error }
// Loading is a UI/MVI concern — managed via State.Loading, not NetworkResult
```

**Remote Services (Ktor-based):**
- `TranslateApiService` — Google Translate scraping + AI expert calls
- `DictionaryApiService` — open-source dictionary API
- `AiExpertService` — unified interface for GPT / Gemini / Claude

**On-device Services (no network):**
- `MlKitOcrService` — ML Kit Text Recognition wrapper (lives in `data/mlkit/`, registered in `mlKitModule`)

### API Key Management

Keys stored encrypted in Firebase Remote Config and locally. Decrypted at runtime via `KeyDecryptionModule`. No user input required.

---

## 6. Dependency Injection (Koin BOM 4.1.1)

**Idiomatic DSL:**
- `singleOf(::Impl)` — singleton, no interface
- `singleOf(::Impl) bind Interface::class` — singleton with interface binding
- `factoryOf(::UseCase)` — new instance per injection
- `viewModelOf(::VM)` — lifecycle-scoped ViewModel

**Modules:** `networkModule`, `databaseModule`, `dataStoreModule`, `securityModule`, `utilsModule`, `mlKitModule`, `repositoryModule`, `useCaseModule`, `viewModelModule`, `ttsModule`

- `mlKitModule` — registers `MlKitOcrService` and `MlKitTranslateService` as `single`

---

## 7. Navigation

**Single Activity** (`MainActivity`) with `NavHostFragment`.

- Navigation Component 2.9.0 with Safe Args (typed `NavDirections`)
- Root `nav_graph.xml` includes nested graphs per feature
- Bottom navigation hidden on splash, onboarding, premium, word detail screens
- `safeNavigate()` prevents crash on rapid taps
- `getNavigationResult()` / `setNavigationResult()` via `SavedStateHandle` for fragment-to-fragment data

### Navigation Graphs

```
nav_graph.xml (root)
├── nav_onboarding.xml
├── nav_translate.xml
├── nav_conversation.xml
├── nav_dictionary.xml      (Dictionary → WordDetail)
├── nav_idioms.xml
├── nav_phrases.xml
├── nav_story.xml
├── nav_settings.xml        (Settings → ThemeSelect → About)
├── nav_premium.xml
├── nav_favourite.xml
├── nav_onscreen.xml
└── nav_imagetext.xml
```

---

## 8. Animations

Three animation styles applied per nav action:

| Style | Enter | Exit | Used for |
|---|---|---|---|
| Slide | `slide_in_right` / `slide_out_left` | `slide_in_left` / `slide_out_right` | Standard forward/back navigation |
| Fade + Slide Up | `slide_up` / `fade_out` | `fade_in` / `slide_down` | Modal / premium / bottom sheet |
| Scale Fade | `scale_fade_in` / `fade_out` | n/a (Splash is one-way; `popUpToInclusive=true` removes it from back stack) | Splash → Home |

Interpolators: `decelerate` for enter, `accelerate_decelerate` for fade.

---

## 9. Features

| Feature | Key Details |
|---|---|
| Text Translate | Google scrape (default) or AI expert; cache in Room |
| Conversation | Dual-panel mic TTS/STT per speaker |
| Dictionary | Open-source API + AI detail; cached in Room |
| Idioms / Phrases / Story | Predefined content + AI explanation via selected expert |
| On-Screen Translate | AccessibilityService (auto) + FloatingOverlayService (manual bubble) |
| Image Text Translate | ML Kit Text Recognition (on-device, offline capable) |
| Offline Mode | ML Kit language packs + Room cache |
| Translate Expert | DEFAULT / GPT / GEMINI / CLAUDE — selected in Settings |
| TTS / STT | `TtsManager` + `SttManager` with `StateFlow<TtsState/SttState>` |
| Favourites | Save/remove translated text; `Flow<List<Favourite>>` from Room |
| Premium / IAP | Google Billing 8.3.0 — freemium + one-time purchase + subscription |
| Settings | Theme, Expert, Languages, Offline toggle, Cache clear, Premium, About |

---

## 10. TTS / STT

- `TtsManager` wraps Android `TextToSpeech` — exposes `StateFlow<TtsState>`
- `SttManager` wraps `SpeechRecognizer` — exposes `StateFlow<SttState>`
- Both registered as `single` in Koin, injected via UseCases

---

## 11. Security

`KeyDecryptionModule` — decrypts encrypted keys from Firebase Remote Config or local fallback. Called by `AiExpertService` at request time. Keys never stored in plaintext.

---

## 12. Dependencies Summary

| Library | Version |
|---|---|
| AGP | 9.1.0 |
| Kotlin | 2.1.20 |
| KSP | 2.1.20-1.0.31 |
| Room 3 | 3.0.0-alpha01 |
| Koin BOM | 4.1.1 |
| Ktor | 3.1.2 |
| Navigation | 2.9.0 |
| Lifecycle | 2.9.0 |
| Coroutines | 1.10.1 |
| DataStore | 1.1.4 |
| Firebase BOM | 33.12.0 |
| ML Kit OCR | 16.0.1 |
| ML Kit Translate | 17.0.3 |
| Billing | 8.3.0 |
| Coil | 2.7.0 |
| Lottie | 6.6.6 |
| Timber | 5.0.1 |

---

## 13. AndroidManifest Permissions & Declarations

### Permissions

```xml
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
```

### Service Declarations

```xml
<!-- Floating Overlay Foreground Service -->
<service
    android:name=".service.FloatingOverlayService"
    android:foregroundServiceType="specialUse"
    android:exported="false" />

<!-- Required for specialUse foreground service — Android 14+ / Play Store compliance -->
<!-- Place inside <application> block -->
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
```

### Runtime Permission Flow

- `RECORD_AUDIO` — requested from `ConversationFragment` and `TranslateFragment` (mic button tap)
- `CAMERA` — requested from `ImageTextFragment` on camera capture action
- `SYSTEM_ALERT_WINDOW` — not a runtime permission; user directed to `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` from `OnScreenFragment`
- `BIND_ACCESSIBILITY_SERVICE` — user directed to Accessibility settings via `Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)` from `OnScreenFragment`
- Permission denial handled via `Effect` (e.g., `Effect.ShowPermissionRationale`, `Effect.OpenSettings`)

---

## 14. Conventions

- `sealed interface State` per screen (not data class)
- `backPressHandler: (() -> Boolean)?` set per fragment
- All Room DAO functions are `suspend` or return `Flow`
- `singleOf` / `viewModelOf` / `factoryOf` Koin DSL throughout
- `safeNavigate()` for all fragment navigation
- `collectFlow()` from `FragmentExt` for all state/effect observation

# Plan 9: Settings Completion — Favourites Wiring, AI Tone Dialog, Language Selection & Localization

## Goal

Complete the three remaining gaps left after Plan 8:
1. Wire `FavouriteFragment → WordDetailFragment` navigation (currently a toast stub)
2. Add AI Tone selection as a dialog inside `SettingsFragment` (no new screen)
3. Build a Language Selection screen that applies full Android UI localization

---

## Architecture Principles

Clean MVI throughout. No new architectural patterns introduced — all new code follows existing conventions: `Contract` / `ViewModel` / `Fragment`, Koin DI, Safe Args navigation, DataStore preferences.

`LocaleHelper` is a pure utility object — no ViewModel dependency, no Flow. It reads the stored locale synchronously at Activity attach time (one `runBlocking` call, acceptable at `attachBaseContext` because no coroutine dispatcher exists yet at that point).

---

## Section 1: Gap Fix — FavouriteFragment → WordDetailFragment

### Problem

`FavouriteContract.Effect.NavigateToWordDetail` in `FavouriteFragment` currently shows a toast placeholder. Dictionary-category favourites cannot open their word detail.

### Fix

**`nav_favourite.xml`** — add `wordDetailFragment` as a destination and an action from `favouriteFragment`:

```xml
<fragment
    android:id="@+id/favouriteFragment"
    android:name="com.android.zubanx.feature.favourite.FavouriteFragment">
    <action
        android:id="@+id/action_favourite_to_wordDetail"
        app:destination="@id/wordDetailFragment"
        app:enterAnim="@anim/slide_in_right"
        app:exitAnim="@anim/slide_out_left"
        app:popEnterAnim="@anim/slide_in_left"
        app:popExitAnim="@anim/slide_out_right" />
</fragment>

<fragment
    android:id="@+id/wordDetailFragment"
    android:name="com.android.zubanx.feature.dictionary.WordDetailFragment"
    android:label="Word Detail">
    <argument android:name="word" app:argType="string" />
    <argument android:name="language" app:argType="string" android:defaultValue="en" />
</fragment>
```

**`FavouriteFragment.kt`** — replace toast in `NavigateToWordDetail` effect handler:
```kotlin
is FavouriteContract.Effect.NavigateToWordDetail -> {
    val action = FavouriteFragmentDirections
        .actionFavouriteToWordDetail(word = effect.word, language = effect.language)
    findNavController().navigate(action)
}
```

**`MainActivity.BOTTOM_NAV_HIDDEN_DESTINATIONS`** — uncomment `R.id.wordDetailFragment` so the bottom nav hides when navigating to word detail from Favourites.

Also uncomment `R.id.premiumFragment` in the same set (was left commented out).

**Files modified:** `nav_favourite.xml`, `FavouriteFragment.kt`, `MainActivity.kt`

---

## Section 2: AI Tone — Settings Dialog

### Overview

No new screen, no new navigation. The **AI Tone** row in `SettingsFragment` shows a `MaterialAlertDialog` with 6 single-choice tone options. Selection is saved immediately. The row's trailing text updates reactively.

### 2.1 AiTone Domain Model

File: `domain/model/AiTone.kt`

```kotlin
enum class AiTone(val key: String, val label: String, val description: String) {
    ORIGINAL("original", "Original", "Natural and authentic language"),
    CASUAL("casual", "Casual", "Friendly and approachable tone"),
    PROFESSIONAL("professional", "Professional", "Neutral, direct, unaltered"),
    EDUCATIONAL("education", "Educational", "Clear and instructional content"),
    FRIENDLY("friendly", "Friendly", "Warm and welcoming tone"),
    FUNNY("funny", "Funny", "Add humor or playfulness to the text");

    companion object {
        fun fromKey(key: String) = entries.firstOrNull { it.key == key } ?: ORIGINAL
    }
}
```

### 2.2 AppPreferences — New Key

Add to `AppPreferences` interface:
```kotlin
val aiTone: Flow<String>
suspend fun setAiTone(value: String)
```

Add to `AppPreferencesImpl`:
```kotlin
val AI_TONE = stringPreferencesKey("ai_tone")
override val aiTone: Flow<String> = dataStore.data.map { it[Keys.AI_TONE] ?: AiTone.ORIGINAL.key }
override suspend fun setAiTone(value: String) { dataStore.edit { it[Keys.AI_TONE] = value } }
```

### 2.3 SettingsContract Changes

Replace `NavigateToAiTone : Event()` → `ShowAiTonePicker : Event()`.

Add a new effect alongside the existing `Navigate` effect (do NOT replace `Navigate` — it is still used for other rows):
```kotlin
data class ShowAiToneDialog(val currentTone: AiTone) : Effect()
```

Change `aiTone: String = "Original"` → `aiTone: AiTone = AiTone.ORIGINAL` in `SettingsContract.State`.

Add events:
```kotlin
object ShowAiTonePicker : Event()
data class SetAiTone(val tone: AiTone) : Event()
```

### 2.4 SettingsViewModel Changes

The existing `combine` in `init` uses 5 flows — the maximum for the typed `combine` overload. Do **not** add a 6th flow to it. Instead:

1. Remove `appPreferences.selectedExpert` from the existing `combine`. The combine becomes 4 flows: `isPremium`, `offlineMode`, `autoSpeak`, `floatingOverlay`. Also remove `aiTone = expert` from the combine lambda — the full updated lambda body:

```kotlin
combine(
    appPreferences.isPremium,
    appPreferences.offlineMode,
    appPreferences.autoSpeak,
    appPreferences.floatingOverlay
) { isPremium, offlineMode, autoSpeak, floatingOverlay ->
    SettingsContract.State(
        isPremium = isPremium,
        offlineMode = offlineMode,
        floatingOverlay = floatingOverlay,
        autoSpeak = autoSpeak,
        appVersion = BuildConfig.VERSION_NAME
        // aiTone is set separately below — omit it here so the separate collector wins
    )
}.collect { newState -> setState { newState } }
```

2. Add `aiTone` as a **separate** `viewModelScope.launch` collection in `init` (after the combine launch):
```kotlin
viewModelScope.launch {
    appPreferences.aiTone.collect { key ->
        setState { copy(aiTone = AiTone.fromKey(key)) }
    }
}
```

This keeps the `combine` within the 4-argument overload and avoids any compile error. Note: `SettingsContract.State` has `aiTone: AiTone = AiTone.ORIGINAL` as default, so the state is valid even for the brief window before the separate collector emits.

Handle `ShowAiTonePicker` event:
```kotlin
SettingsContract.Event.ShowAiTonePicker -> {
    sendEffect(SettingsContract.Effect.ShowAiToneDialog(state.value.aiTone))
}
```

Add new event handler for when a tone is selected from the dialog:
```kotlin
is SettingsContract.Event.SetAiTone -> viewModelScope.launch {
    appPreferences.setAiTone(event.tone.key)
}
```

Add `data class SetAiTone(val tone: AiTone) : Event()` to `SettingsContract.Event`.

### 2.5 SettingsFragment Changes

**`setupViews()`** — the existing binding `binding.rowAiTone.setOnClickListener { viewModel.onEvent(SettingsContract.Event.NavigateToAiTone) }` must be **replaced** (not added alongside) with:
```kotlin
binding.rowAiTone.setOnClickListener { viewModel.onEvent(SettingsContract.Event.ShowAiTonePicker) }
```
`NavigateToAiTone` is deleted from the contract, so the old line will not compile if left in place.

**State observation:** `binding.tvAiTone.text = state.aiTone.label`

**Effect handler — `ShowAiToneDialog`:**
```kotlin
is SettingsContract.Effect.ShowAiToneDialog -> {
    val tones = AiTone.entries.toTypedArray()
    val labels = tones.map { "${it.label} — ${it.description}" }.toTypedArray()
    val currentIndex = tones.indexOf(effect.currentTone)
    MaterialAlertDialogBuilder(requireContext())
        .setTitle("AI Tone")
        .setSingleChoiceItems(labels, currentIndex) { dialog, which ->
            viewModel.onEvent(SettingsContract.Event.SetAiTone(tones[which]))
            dialog.dismiss()
        }
        .setNegativeButton("Cancel", null)
        .show()
}
```

### 2.6 TranslateUseCase — Apply Tone to AI Prompt

`TranslateUseCase` gains a new `aiTone: String` parameter (default `"original"`). When `expert != "DEFAULT"`, the prompt is built with tone instruction:

`AiExpertServiceImpl.buildTranslationPrompt()` becomes:
```kotlin
fun buildTranslationPrompt(text: String, sourceLang: String, targetLang: String, tone: String = "original"): String {
    val toneInstruction = when (tone) {
        "casual"       -> "Use a friendly and approachable tone."
        "professional" -> "Use a neutral, direct, professional tone."
        "education"    -> "Provide clear and instructional content."
        "friendly"     -> "Adopt a warm and welcoming tone."
        "funny"        -> "Add humor or playfulness to the translated text."
        else           -> "Use natural and authentic language." // original
    }
    return "Translate the following text from $sourceLang to $targetLang. $toneInstruction " +
           "Reply with only the translated text, no explanations.\n\n$text"
}
```

`TranslateViewModel` changes — add a `private var currentAiTone = AiTone.ORIGINAL.key` field alongside the existing `currentExpert`, then read both in `init`:

```kotlin
private var currentAiTone = AiTone.ORIGINAL.key

// Inside the existing init { viewModelScope.launch { ... } } block that reads prefs:
currentExpert = appPreferences.selectedExpert.first()
currentAiTone = appPreferences.aiTone.first()
```

`aiTone` is read once with `.first()` (same pattern as `selectedExpert`) — not continuously collected. This is intentional: the tone applies to the current session. If the user changes tone in Settings, the change takes effect when they return to the translate screen and the ViewModel is re-created (or on the next translate call if the ViewModel is still alive, since `init` has already run). For MVP this is acceptable.

Update the `translate()` call site to pass `currentAiTone` as the 5th argument:
```kotlin
translateUseCase(text, currentSourceLang.code, currentTargetLang.code, currentExpert, currentAiTone)
```

**Files modified/created:** `AiTone.kt` (create), `AppPreferences.kt`, `AppPreferencesImpl.kt`, `SettingsContract.kt`, `SettingsViewModel.kt`, `SettingsFragment.kt`, `TranslateUseCase.kt`, `AiExpertServiceImpl.kt`, `TranslateViewModel.kt`

---

## Section 3: Language Selection Screen

### 3.1 AppLanguage Presentation Model

`AppLanguage` is a **presentation-layer** model — it carries a drawable reference (`@DrawableRes Int`) and therefore lives in `feature/language/`, NOT in `domain/model/`. Domain models in this project carry no Android dependencies (confirmed by `Favourite.kt`, `DictionaryEntry.kt`, etc.).

File: `feature/language/AppLanguage.kt`

```kotlin
data class AppLanguage(
    val code: String,
    val displayName: String,
    @DrawableRes val flagDrawable: Int
) {
    companion object {
        val ALL = listOf(
            AppLanguage("en",  "English",    R.drawable.flag_us),
            AppLanguage("es",  "Spanish",    R.drawable.flag_es),
            AppLanguage("fil", "Filipino",   R.drawable.flag_ph),
            AppLanguage("hi",  "Hindi",      R.drawable.flag_in),
            AppLanguage("de",  "German",     R.drawable.flag_de),
            AppLanguage("fr",  "French",     R.drawable.flag_fr),
            AppLanguage("it",  "Italian",    R.drawable.flag_it),
            AppLanguage("ko",  "Korean",     R.drawable.flag_kr),
            AppLanguage("pt",  "Portuguese", R.drawable.flag_pt),
            AppLanguage("vi",  "Vietnamese", R.drawable.flag_vn),
            AppLanguage("ar",  "Arabic",     R.drawable.flag_sa),
            AppLanguage("ru",  "Russian",    R.drawable.flag_ru),
            AppLanguage("bn",  "Bengali",    R.drawable.flag_bd),
            AppLanguage("zh",  "Chinese",    R.drawable.flag_cn),
            AppLanguage("ur",  "Urdu",       R.drawable.flag_pk),
            AppLanguage("id",  "Indonesian", R.drawable.flag_id),
            AppLanguage("tr",  "Turkish",    R.drawable.flag_tr),
        )

        fun fromCode(code: String) = ALL.firstOrNull { it.code == code } ?: ALL.first()
    }
}
```

Flag drawables are PNG files placed in `res/drawable/`. Each flag is a square PNG of the country flag — displayed in a circular `ShapeableImageView` with `shapeAppearanceModel = @style/ShapeAppearance.App.CircleFlag`. No external library required.

### 3.2 AppPreferences — New Key

```kotlin
val appLanguage: Flow<String>
suspend fun setAppLanguage(value: String)
```

`AppPreferencesImpl`:
```kotlin
val APP_LANGUAGE = stringPreferencesKey("app_language")
override val appLanguage: Flow<String> = dataStore.data.map { it[Keys.APP_LANGUAGE] ?: "en" }
override suspend fun setAppLanguage(value: String) { dataStore.edit { it[Keys.APP_LANGUAGE] = value } }
```

### 3.3 LocaleHelper

File: `core/utils/LocaleHelper.kt`

```kotlin
object LocaleHelper {
    fun applyLocale(context: Context, langCode: String): Context {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
```

### 3.4 Shared DataStore Extension — Fix Duplicate Instance Risk

The existing `private val Context.dataStore by preferencesDataStore(name = "zuban_prefs")` is declared at the top of `DataStoreModule.kt` as `private`. Because it is `private`, it cannot be accessed from `attachBaseContext` in `BaseActivity` or `ZubanApp`. Creating a second `preferencesDataStore` delegate pointing to the same file name `"zuban_prefs"` in another file would throw `IllegalStateException("There are multiple DataStores active for the same file")` at runtime.

**Fix:** Move the delegate to a new file and change visibility to `internal`:

**New file: `core/utils/DataStoreExt.kt`**
```kotlin
package com.android.zubanx.core.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

internal val Context.zubanDataStore: DataStore<Preferences> by preferencesDataStore(name = "zuban_prefs")
```

**`DataStoreModule.kt`** — remove the old private delegate; use the shared extension:
```kotlin
val dataStoreModule = module {
    single<DataStore<Preferences>> { androidContext().zubanDataStore }
    singleOf(::AppPreferencesImpl) bind AppPreferences::class
}
```

### 3.5 BaseActivity — attachBaseContext

`BaseActivity.attachBaseContext()` reads the stored language code directly from the shared DataStore extension — no `AppPreferencesImpl` construction required:

```kotlin
override fun attachBaseContext(newBase: Context) {
    val langCode = runBlocking {
        newBase.zubanDataStore.data.first()[stringPreferencesKey("app_language")] ?: "en"
    }
    super.attachBaseContext(LocaleHelper.applyLocale(newBase, langCode))
}
```

`runBlocking` is acceptable here — `attachBaseContext` is called before any coroutine dispatcher exists; this is the Android-standard pattern for synchronous DataStore reads at context-wrap time.

### 3.6 ZubanApp — attachBaseContext

Same override in `ZubanApp`:
```kotlin
override fun attachBaseContext(newBase: Context) {
    val langCode = runBlocking {
        newBase.zubanDataStore.data.first()[stringPreferencesKey("app_language")] ?: "en"
    }
    super.attachBaseContext(LocaleHelper.applyLocale(newBase, langCode))
}
```

### 3.6 LanguageContract

File: `feature/language/LanguageContract.kt`

```kotlin
object LanguageContract {
    data class State(
        val languages: List<AppLanguage> = AppLanguage.ALL,
        val selectedCode: String = "en"
    ) : UiState

    sealed class Event : UiEvent {
        data class SelectLanguage(val code: String) : Event()
        object Confirm : Event()
    }

    sealed class Effect : UiEffect {
        data class ApplyLocaleAndRestart(val code: String) : Effect()
    }
}
```

### 3.7 LanguageViewModel

File: `feature/language/LanguageViewModel.kt`

```kotlin
class LanguageViewModel(private val appPreferences: AppPreferences) :
    BaseViewModel<LanguageContract.State, LanguageContract.Event, LanguageContract.Effect>(
        LanguageContract.State()
    ) {

    init {
        viewModelScope.launch {
            val current = appPreferences.appLanguage.first()
            setState { copy(selectedCode = current) }
        }
    }

    override fun onEvent(event: LanguageContract.Event) {
        when (event) {
            is LanguageContract.Event.SelectLanguage -> setState { copy(selectedCode = event.code) }
            LanguageContract.Event.Confirm -> viewModelScope.launch {
                val code = state.value.selectedCode
                appPreferences.setAppLanguage(code)
                sendEffect(LanguageContract.Effect.ApplyLocaleAndRestart(code))
            }
        }
    }
}
```

### 3.8 LanguageFragment

File: `feature/language/LanguageFragment.kt`

- `RecyclerView` driven by `LanguageAdapter`
- Toolbar with **"Done"** `MenuItem` (top-right) → dispatches `LanguageContract.Event.Confirm`
- Bottom nav is hidden on this screen (add `languageFragment` to `MainActivity.BOTTOM_NAV_HIDDEN_DESTINATIONS`)

Effect handler:
```kotlin
is LanguageContract.Effect.ApplyLocaleAndRestart -> {
    // recreate() posts an Activity restart via ActivityThread. It discards the
    // current back stack and restores from savedInstanceState, which naturally
    // lands on the start destination (translate tab). No popBackStack() needed —
    // it would operate on a graph ID (not a fragment ID) and be a no-op anyway.
    // The actual locale is applied in attachBaseContext during the new Activity's
    // startup, so no LocaleHelper call is needed here either.
    requireActivity().recreate()
}
```

### 3.9 LanguageAdapter

File: `feature/language/LanguageAdapter.kt`

`ListAdapter<AppLanguage, VH>` with `DiffUtil`. Each item row (`item_language.xml`):
- `ShapeableImageView` (48dp circle, `shapeAppearance = @style/ShapeAppearance.App.CircleFlag`) — flag PNG loaded via `setImageResource(item.flagDrawable)`
- `TextView` — `item.displayName`
- `ImageView` — `ic_check_circle` (filled, `colorPrimary`) when selected; `ic_radio_button_unchecked` (grey) when not selected
- Row background highlighted (`colorSurfaceVariant`) when selected

The adapter receives the `selectedCode` from state and rebinds only changed items via `DiffUtil`.

### 3.10 Layouts

**`item_language.xml`** — `ConstraintLayout`:
- `ShapeableImageView` id `imgFlag` — start, centered vertical — 48dp × 48dp
- `TextView` id `tvLanguageName` — start of flag + 12dp margin, centered vertical — bold `textAppearanceBodyLarge`
- `ImageView` id `imgSelected` — end, centered vertical — 24dp × 24dp

**`fragment_language.xml`** — `CoordinatorLayout`:
- `MaterialToolbar` id `toolbar` — title `"Language"`, menu item `"Done"` (`app:showAsAction="always"`)
- `RecyclerView` id `rvLanguages` below toolbar — `LinearLayoutManager`

**Style for circular flag** (in `themes.xml`):
```xml
<style name="ShapeAppearance.App.CircleFlag" parent="">
    <item name="cornerFamily">rounded</item>
    <item name="cornerSize">50%</item>
</style>
```

### 3.11 Navigation

**`nav_language.xml`** (new file):
```xml
<navigation android:id="@+id/nav_language" app:startDestination="@id/languageFragment">
    <fragment android:id="@+id/languageFragment"
        android:name="com.android.zubanx.feature.language.LanguageFragment"
        android:label="Language" />
</navigation>
```

**`nav_graph.xml`** — add `<include app:graph="@navigation/nav_language" />`

**`nav_settings.xml`** — add action from `settingsFragment`:
```xml
<action
    android:id="@+id/action_settings_to_language"
    app:destination="@id/nav_language" />
```

**`SettingsContract`** — `NavigateToLanguage : Event()` already exists in the live codebase. No change to the event itself.

**`SettingsViewModel`** — the existing `NavigateToLanguage` handler currently shows a toast stub `"Language settings coming soon"`. **Replace** it with:
```kotlin
SettingsContract.Event.NavigateToLanguage ->
    sendEffect(SettingsContract.Effect.Navigate(R.id.action_settings_to_language))
```
This is a one-line change that wires the existing stub to the real action.

### 3.12 String Resources — Infrastructure Only

This plan creates the locale infrastructure. Actual translated strings are a **content task** (separate from this implementation plan). The app falls back to English strings until translated `values-XX/strings.xml` files are added.

The plan does add `values-ur/strings.xml`, `values-hi/strings.xml` as **placeholder files** with a comment:
```xml
<!-- TODO: Add Urdu translations for all strings in values/strings.xml -->
```
This ensures the locale switching works end-to-end even before translations exist.

---

## Section 4: DI Updates

**`ViewModelModule`** — add:
```kotlin
viewModelOf(::LanguageViewModel)
```

**`nav_graph.xml`** — include `nav_language`

**`MainActivity.BOTTOM_NAV_HIDDEN_DESTINATIONS`** — add:
- `R.id.languageFragment`
- Uncomment `R.id.premiumFragment`
- Uncomment `R.id.wordDetailFragment`

---

## File Map

| Action | File |
|--------|------|
| Create | `domain/model/AiTone.kt` |
| Create | `feature/language/AppLanguage.kt` — presentation model (NOT in domain/) |
| Create | `core/utils/LocaleHelper.kt` |
| Create | `feature/language/LanguageContract.kt` |
| Create | `feature/language/LanguageViewModel.kt` |
| Create | `feature/language/LanguageFragment.kt` |
| Create | `feature/language/LanguageAdapter.kt` |
| Create | `res/layout/fragment_language.xml` |
| Create | `res/layout/item_language.xml` |
| Create | `res/navigation/nav_language.xml` |
| Create | `res/drawable/flag_us.png` … `flag_tr.png` (17 flag PNGs) |
| Create | `res/values-ur/strings.xml` (placeholder) |
| Create | `res/values-hi/strings.xml` (placeholder) |
| Create | `core/utils/DataStoreExt.kt` — move `preferencesDataStore` delegate here as `internal val Context.zubanDataStore` |
| Modify | `AppPreferences.kt` — add `aiTone`, `appLanguage` |
| Modify | `AppPreferencesImpl.kt` — implement `aiTone` and `appLanguage` keys |
| Modify | `DataStoreModule.kt` — remove private delegate; use `androidContext().zubanDataStore` from `DataStoreExt.kt` |
| Modify | `SettingsContract.kt` — add `aiTone` to state, `ShowAiTonePicker`/`SetAiTone` events, `ShowAiToneDialog` effect |
| Modify | `SettingsViewModel.kt` — handle tone events, collect `aiTone` pref |
| Modify | `SettingsFragment.kt` — dialog for tone, `NavigateToLanguage` click |
| Modify | `TranslateUseCase.kt` — accept `aiTone: String` param |
| Modify | `AiExpertServiceImpl.kt` — `buildTranslationPrompt` accepts tone |
| Modify | `TranslateViewModel.kt` — read `aiTone` pref and pass to use case |
| Modify | `BaseActivity.kt` — `attachBaseContext` with `LocaleHelper` |
| Modify | `ZubanApp.kt` — `attachBaseContext` with `LocaleHelper` |
| Modify | `nav_favourite.xml` — add `wordDetailFragment` + action |
| Modify | `nav_settings.xml` — add `action_settings_to_language` |
| Modify | `nav_graph.xml` — include `nav_language` |
| Modify | `FavouriteFragment.kt` — wire `NavigateToWordDetail` effect |
| Modify | `MainActivity.kt` — update `BOTTOM_NAV_HIDDEN_DESTINATIONS` |
| Modify | `res/values/themes.xml` — add `ShapeAppearance.App.CircleFlag` |

---

## ANR / Correctness Notes

| Risk | Mitigation |
|------|-----------|
| `runBlocking` in `attachBaseContext` | Acceptable — no coroutine dispatcher exists at this point; this is the Android-standard pattern for synchronous DataStore reads at context wrap time |
| Locale not applying after `recreate()` | `attachBaseContext` override on both `BaseActivity` and `ZubanApp` ensures every Activity and Application context gets the locale applied before any layout inflation |
| Back stack after `recreate()` | `recreate()` discards the current back stack and restores from `savedInstanceState`, naturally landing on the start destination (translate tab). No `popBackStack()` needed — `LanguageFragment` effect handler calls `requireActivity().recreate()` only. |
| `wordDetailFragment` duplicate in nav graphs | Safe — each `<navigation>` graph is independent; Navigation Component resolves destinations by graph scope, not globally |
| Flag PNGs bundled in APK | 17 small PNGs (~5–10 KB each) add < 200 KB to APK; acceptable |

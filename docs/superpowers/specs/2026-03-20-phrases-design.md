# Phrases Feature — Design Spec
**Date:** 2026-03-20
**Status:** Approved

---

## Overview

A static-base, dynamically-translated Phrases feature organised into 10 travel/life categories. Users pick a category, select source/target languages, and browse expandable phrase cards. Each card shows the phrase in the source language; expanding reveals the translation with speak, copy, and zoom actions. A full-screen zoom view lets users show the translated text to a local speaker.

---

## Navigation & App Bar Changes

### Bottom Nav Restructure
Settings is removed from the bottom nav. The 5 tabs become:

| Position | Label | Nav Graph |
|---|---|---|
| 1 | Translate | nav_translate |
| 2 | Conversation | nav_conversation |
| 3 | Dictionary | nav_dictionary |
| 4 | Phrases | nav_phrases |
| 5 | Favourites | nav_favourite |

### Translate Screen App Bar
A `MaterialToolbar` is added at the top of `fragment_translate.xml` with `app:title="ZubanX"`.
- **Right icons (end menu):** diamond (`ic_premium`) → navigates to Premium screen via global nav action; gear (`ic_settings`) → navigates to Settings via `findNavController().navigate(R.id.nav_settings)`
- The toolbar is set as the support action bar via `(activity as AppCompatActivity).setSupportActionBar(binding.toolbar)` in `TranslateFragment.onViewCreated`

### MainActivity Changes
- Remove `nav_settings` from `bottom_nav_menu.xml`
- Add `nav_phrases` to `bottom_nav_menu.xml` with icon `ic_nav_phrases`
- Add `nav_settings` start destination (`settingsFragment`) to the hidden destinations list in `MainActivity` so the bottom nav is hidden when Settings is shown
- The `nav_settings` graph remains in `nav_graph.xml` unchanged — it is just no longer a bottom nav tab

---

## Screen 1: Phrases Categories (`PhrasesFragment`)

**Layout:** `RecyclerView` with `GridLayoutManager(spanCount = 3)`.

**10 categories:**

| Stable ID | Label | Drawable |
|---|---|---|
| `dining` | Dining | `ic_category_dining` |
| `emergency` | Emergency | `ic_category_emergency` |
| `travel` | Travel | `ic_category_travel` |
| `greeting` | Greeting | `ic_category_greeting` |
| `shopping` | Shopping | `ic_category_shopping` |
| `hotel` | Hotel | `ic_category_hotel` |
| `office` | Office | `ic_category_office` |
| `trouble` | Trouble | `ic_category_trouble` |
| `entertainment` | Entertainment | `ic_category_entertainment` |
| `medicine` | Medicine | `ic_category_medicine` |

Each drawable is a new Material-style vector asset (`24dp` viewport, white fill). Each card: `MaterialCardView` with a centered 32dp icon and label below. Tapping navigates to `PhrasesCategoryFragment` passing `categoryId` (the stable string ID above, e.g. `"dining"`).

No language selector on this screen. No empty state required — `PhrasesData` guarantees a non-empty category list.

---

## Screen 2: Category Detail (`PhrasesCategoryFragment`)

### Safe Args Input
- `categoryId: String` — the stable ID (e.g. `"dining"`)
- Fragment resolves the `PhraseCategory` instance via `PhrasesData.categoryById(categoryId)` which returns the matching sealed subclass

### Layout (top → bottom)
1. `MaterialToolbar` — back arrow + `category.displayName` as title
2. **Language bar** (inline in `fragment_phrases_category.xml`, no separate file): `btnSourceLang` | `btnSwapLang` (swap icon) | `btnTargetLang` — identical visual pattern to the Translate screen language bar
3. `RecyclerView` — managed by `PhrasesCategoryAdapter` (expandable items, `item_phrase.xml`)

### Phrase Item Behaviour (`item_phrase.xml`)
- **Collapsed:** phrase text in source language + chevron-down icon
- **Expanded:** translated text row + 3 action icons:
  - `ic_volume_up` (speak) — TTS reads the translation in target language
  - `ic_copy` (copy) — copies translation to clipboard, shows toast "Copied"
  - `ic_zoom_in` (zoom) — navigates to `PhrasesZoomFragment` passing `translatedText` + `langCode`
- **Loading state (expanded):** progress indicator replaces translated text while API call is in progress
- **Error state (expanded):** if translation fails, show inline error text ("Translation failed") + a retry icon (`ic_refresh`) inside the expanded card; card remains expanded; tapping retry re-triggers the API call

### Expand Model
One item expanded at a time (accordion). State holds `expandedIndex: Int?` (null = none expanded). When a user taps an already-expanded item it collapses. When language pair changes, `expandedIndex` resets to null.

### Language Defaults
- Source: English (`en`)
- Target: Urdu (`ur`)

### Translation Behaviour
- Phrases are hardcoded in English as the canonical base
- **Source language:** if source ≠ English, list phrases are translated English → source via API before display
- **Target language:** expanded row shows source-language text → target-language translation
- **Cache:** `HashMap<String, String>` in ViewModel keyed by `"$sourceLang:$targetLang:$phraseIndex"` — ViewModel-scoped, cleared on ViewModel destruction, no size limit imposed (bounded by number of language pairs × 10 phrases)
- Language pair change resets `expandedIndex` to null; only uncached entries trigger new API calls

### Language Picker
Same `AlertDialog` list as Translate and Conversation screens. Both pickers exclude the `DETECT` entry.

---

## Screen 3: Phrase Zoom (`PhrasesZoomFragment`)

**Full-screen, no ViewModel. `TtsManager` injected directly via `by inject()` in the fragment.**

### Safe Args Input
- `translatedText: String`
- `langCode: String`

### Layout (top → bottom)
1. `MaterialToolbar` — back arrow only, no title
2. `TextView` (centered, `textAppearanceDisplaySmall` ~28sp, vertically centered in remaining space) — translated text
3. `MaterialButton` with `ic_volume_up` — calls `ttsManager.speak(translatedText, langCode)` on tap

---

## Data Layer

### `PhraseCategory.kt`
```kotlin
sealed class PhraseCategory(val id: String, val displayName: String, val iconRes: Int) {
    object Dining       : PhraseCategory("dining",        "Dining",        R.drawable.ic_category_dining)
    object Emergency    : PhraseCategory("emergency",     "Emergency",     R.drawable.ic_category_emergency)
    object Travel       : PhraseCategory("travel",        "Travel",        R.drawable.ic_category_travel)
    object Greeting     : PhraseCategory("greeting",      "Greeting",      R.drawable.ic_category_greeting)
    object Shopping     : PhraseCategory("shopping",      "Shopping",      R.drawable.ic_category_shopping)
    object Hotel        : PhraseCategory("hotel",         "Hotel",         R.drawable.ic_category_hotel)
    object Office       : PhraseCategory("office",        "Office",        R.drawable.ic_category_office)
    object Trouble      : PhraseCategory("trouble",       "Trouble",       R.drawable.ic_category_trouble)
    object Entertainment: PhraseCategory("entertainment", "Entertainment", R.drawable.ic_category_entertainment)
    object Medicine     : PhraseCategory("medicine",      "Medicine",      R.drawable.ic_category_medicine)
}
```

### `PhrasesData.kt`
```kotlin
object PhrasesData {
    val categories: List<PhraseCategory> = listOf(...)  // all 10 in display order
    val phrases: Map<PhraseCategory, List<String>>       // 10+ English phrases per category
    fun categoryById(id: String): PhraseCategory = categories.first { it.id == id }
}
```

### Sample phrases per category (English base, 10 each minimum)

**Dining:** A table for two, please. / Can I see the menu? / I am allergic to nuts. / What do you recommend? / Could we have the bill, please? / Is there a vegetarian option? / No spice, please. / Water, please. / This is delicious! / Can I have a takeaway?

**Emergency:** Call an ambulance! / I need a doctor. / I have lost my passport. / Call the police, please. / There is a fire! / I have been robbed. / I need help. / Where is the nearest hospital? / I am injured. / Please hurry!

**Travel:** Where is the bus station? / How much is the ticket? / Does this bus go to the city centre? / Can you call me a taxi? / Where is the airport? / I missed my flight. / I need to go to this address. / Is this the right platform? / How far is it? / Can I have a map?

**Greeting:** Good morning! / Good evening! / How are you? / Nice to meet you. / My name is … / I do not speak this language. / Do you speak English? / Thank you very much. / You are welcome. / Goodbye!

**Shopping:** How much does this cost? / Do you have a smaller size? / Can I try this on? / I would like to buy this. / Do you accept cards? / Can I get a discount? / Where is the fitting room? / I am just looking. / Can I get a receipt? / Do you have this in another colour?

**Hotel:** I have a reservation. / I would like to check in. / What time is checkout? / Can I have an extra pillow? / The air conditioning is not working. / Can I have a wake-up call at 7? / Where is the elevator? / Can I have room service? / I would like to extend my stay. / Can you store my luggage?

**Office:** I have a meeting at 10. / Can I use the Wi-Fi? / Where is the conference room? / I need to print a document. / Can I speak to the manager? / I am here for an interview. / Please send me the report. / The projector is not working. / I need a pen and paper. / Can you reschedule the meeting?

**Trouble:** I am lost. / Can you help me? / I do not understand. / Please speak slowly. / Can you write that down? / I need a translator. / This is not what I ordered. / There is a problem with my room. / I want to make a complaint. / Can I speak to a supervisor?

**Entertainment:** Two tickets, please. / What time does the show start? / Where is the entrance? / Is there a student discount? / Can I take photos here? / Where is the nearest cinema? / What is showing tonight? / I would like to book in advance. / Is this show suitable for children? / Can I get a programme?

**Medicine:** I need a pharmacy. / I have a headache. / I feel nauseous. / I am diabetic. / I am allergic to penicillin. / I need my prescription filled. / How many times a day should I take this? / Do you have pain relief? / I have a fever. / I need to see a doctor urgently.

---

## Translation Architecture

### `TranslatePhraseUseCase`
- Constructor: `TranslateApiService`
- Calls `apiService.translate(text, sourceLang, targetLang)`
- Returns `NetworkResult<TranslateResponseDto>`
- Does **not** save to translation history

### `PhrasesCategoryViewModel`
```
State.Active(
    category: PhraseCategory,
    phrases: List<String>,          // English base phrases
    langSource: LanguageItem,       // default: English
    langTarget: LanguageItem,       // default: Urdu
    displayPhrases: List<String>,   // source-language phrases (translated if source≠en)
    expandedIndex: Int?,            // null = none expanded
    translationCache: Map<String, String>,  // key: "$src:$tgt:$index"
    loadingIndices: Set<Int>,       // indices currently being translated
    errorIndices: Set<Int>          // indices with failed translation
)
Events: LangSourceSelected, LangTargetSelected, SwapLanguages, ExpandPhrase(index), CollapsePhrase, RetryTranslation(index)
Effects: SpeakText(text, langCode), CopyToClipboard(text), ShowToast(msg), NavigateToZoom(translatedText, langCode)
```

### `PhrasesViewModel`
- State holds `categories: List<PhraseCategory>` from `PhrasesData`
- No API calls, no effects

---

## Dependency Injection

| Module | Addition |
|---|---|
| `UseCaseModule` | `factoryOf(::TranslatePhraseUseCase)` |
| `ViewModelModule` | `viewModelOf(::PhrasesViewModel)`, `viewModelOf(::PhrasesCategoryViewModel)` |

---

## Navigation (`nav_phrases.xml`)

```
phrasesFragment (start destination)
  └─→ phrasesCategoryFragment  [arg: categoryId: String]
        └─→ phrasesZoomFragment  [args: translatedText: String, langCode: String]
```

---

## File Map

| Action | Path |
|---|---|
| **Create** | `domain/usecase/phrases/TranslatePhraseUseCase.kt` |
| **Create** | `feature/phrases/data/PhraseCategory.kt` |
| **Create** | `feature/phrases/data/PhrasesData.kt` |
| **Create** | `feature/phrases/PhrasesContract.kt` |
| **Create** | `feature/phrases/PhrasesViewModel.kt` |
| **Create** | `feature/phrases/PhrasesFragment.kt` |
| **Create** | `feature/phrases/PhrasesCategoryContract.kt` |
| **Create** | `feature/phrases/PhrasesCategoryViewModel.kt` |
| **Create** | `feature/phrases/PhrasesCategoryFragment.kt` |
| **Create** | `feature/phrases/PhrasesCategoryAdapter.kt` |
| **Create** | `feature/phrases/PhrasesZoomFragment.kt` |
| **Create** | `res/layout/fragment_phrases.xml` |
| **Create** | `res/layout/fragment_phrases_category.xml` |
| **Create** | `res/layout/fragment_phrases_zoom.xml` |
| **Create** | `res/layout/item_phrase_category.xml` |
| **Create** | `res/layout/item_phrase.xml` |
| **Create** | `res/drawable/ic_nav_phrases.xml` |
| **Create** | `res/drawable/ic_category_dining.xml` |
| **Create** | `res/drawable/ic_category_emergency.xml` |
| **Create** | `res/drawable/ic_category_travel.xml` |
| **Create** | `res/drawable/ic_category_greeting.xml` |
| **Create** | `res/drawable/ic_category_shopping.xml` |
| **Create** | `res/drawable/ic_category_hotel.xml` |
| **Create** | `res/drawable/ic_category_office.xml` |
| **Create** | `res/drawable/ic_category_trouble.xml` |
| **Create** | `res/drawable/ic_category_entertainment.xml` |
| **Create** | `res/drawable/ic_category_medicine.xml` |
| **Modify** | `res/navigation/nav_phrases.xml` |
| **Modify** | `res/menu/bottom_nav_menu.xml` |
| **Modify** | `res/layout/fragment_translate.xml` |
| **Modify** | `feature/translate/TranslateFragment.kt` |
| **Modify** | `core/di/UseCaseModule.kt` |
| **Modify** | `core/di/ViewModelModule.kt` |
| **Modify** | `app/MainActivity.kt` |

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
A `MaterialToolbar` is added to `fragment_translate.xml`:
- **Left:** "ZubanX" title
- **Right icons:** diamond (`ic_premium`) → navigates to Premium screen; gear (`ic_settings`) → navigates to Settings screen

`MainActivity` hides the bottom nav for Settings (it is now reached via the toolbar, not the nav bar).

---

## Screen 1: Phrases Categories (`PhrasesFragment`)

**Layout:** `RecyclerView` with `GridLayoutManager(spanCount = 3)`.

**Categories:**

| ID | Label | Icon |
|---|---|---|
| DINING | Dining | ic_restaurant |
| EMERGENCY | Emergency | ic_emergency |
| TRAVEL | Travel | ic_flight |
| GREETING | Greeting | ic_waving_hand |
| SHOPPING | Shopping | ic_shopping_bag |
| HOTEL | Hotel | ic_hotel |
| OFFICE | Office | ic_work |
| TROUBLE | Trouble | ic_warning |
| ENTERTAINMENT | Entertainment | ic_movie |
| MEDICINE | Medicine | ic_medical_services |

Each card: `MaterialCardView` with a centered 32dp icon and label below. Tapping navigates to `PhrasesCategoryFragment` passing `categoryId`.

No language selector on this screen.

---

## Screen 2: Category Detail (`PhrasesCategoryFragment`)

### Layout (top → bottom)
1. `MaterialToolbar` — back arrow + category display name as title
2. Language bar — `btnSourceLang` | `btnSwapLang` (swap icon) | `btnTargetLang` — identical pattern to Translate screen
3. `RecyclerView` — expandable phrase items (`item_phrase.xml`)

### Phrase Item Behaviour
- **Collapsed:** phrase text in source language + chevron-down icon
- **Expanded:** translated text row + 3 action icons:
  - `ic_volume_up` (speak) — TTS reads the translation
  - `ic_copy` (copy) — copies translation to clipboard
  - `ic_zoom_in` (zoom) — navigates to `PhrasesZoomFragment`

### Language Defaults
- Source: English (`en`)
- Target: Urdu (`ur`)

### Translation Behaviour
- Phrases are hardcoded in English as the canonical base
- **Source language:** if source ≠ English, the list phrases are translated English → source via API before display
- **Target language:** expanded row always shows English → target translation (or source → target if source ≠ English, using cached source translation as input)
- **Cache:** `Map<String, String>` in ViewModel keyed by `"$sourceLang:$targetLang:$phraseIndex"` — survives language switches within session
- **Loading state:** shimmer/progress indicator per item while translating; other items remain interactive
- Changing language re-triggers translation only for uncached entries

### Language Picker
Same `AlertDialog` list as Translate and Conversation screens. Both pickers exclude the `DETECT` entry.

---

## Screen 3: Phrase Zoom (`PhrasesZoomFragment`)

**Full-screen, stateless — receives data via Safe Args.**

### Layout (top → bottom)
1. `MaterialToolbar` — back arrow only, no title
2. `TextView` (centered, `textAppearanceDisplaySmall` ~28sp) — translated text, vertically centered in remaining space
3. `MaterialButton` with `ic_volume_up` — speaks the translated text via TTS

**Safe Args:** `translatedText: String`, `langCode: String`

No ViewModel required.

---

## Data Layer

### `PhrasesData.kt`
A top-level `object` containing:
```kotlin
val categories: List<PhraseCategory>   // ordered list of all 10 categories
val phrases: Map<PhraseCategory, List<String>>  // 10+ English phrases per category
```

`PhraseCategory` is a sealed class with `id`, `displayName: String`, `iconRes: Int`.

### Sample phrases (Dining)
1. A table for two, please.
2. Can I see the menu?
3. I am allergic to nuts.
4. What do you recommend?
5. Could we have the bill, please?
6. Is there a vegetarian option?
7. No spice, please.
8. Water, please.
9. This is delicious!
10. Can I have a takeaway?

*(10+ phrases defined per category.)*

---

## Translation Architecture

### `TranslatePhraseUseCase`
- Constructor: `TranslateApiService`
- Calls `apiService.translate(text, sourceLang, targetLang)`
- Returns `NetworkResult<TranslateResponseDto>`
- Does **not** save to translation history

### `PhrasesCategoryViewModel`
- State: `Active(category, phrases, langA, langB, expandedIndex, translationCache, loadingIndices)`
- Events: `LangASelected`, `LangBSelected`, `SwapLanguages`, `ExpandPhrase`, `CollapsePhrase`
- Effects: `SpeakText`, `CopyToClipboard`, `ShowToast`
- Translates on-demand; checks cache before calling API

### `PhrasesViewModel`
- Holds the list of categories from `PhrasesData`
- No API calls

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
| Create | `domain/usecase/phrases/TranslatePhraseUseCase.kt` |
| Create | `feature/phrases/data/PhrasesData.kt` |
| Create | `feature/phrases/data/PhraseCategory.kt` |
| Create | `feature/phrases/PhrasesContract.kt` |
| Create | `feature/phrases/PhrasesViewModel.kt` |
| Create | `feature/phrases/PhrasesFragment.kt` |
| Create | `feature/phrases/PhrasesCategoryContract.kt` |
| Create | `feature/phrases/PhrasesCategoryViewModel.kt` |
| Create | `feature/phrases/PhrasesCategoryFragment.kt` |
| Create | `feature/phrases/PhrasesZoomFragment.kt` |
| Create | `res/layout/fragment_phrases.xml` |
| Create | `res/layout/fragment_phrases_category.xml` |
| Create | `res/layout/fragment_phrases_zoom.xml` |
| Create | `res/layout/item_phrase_category.xml` |
| Create | `res/layout/item_phrase.xml` |
| Modify | `res/navigation/nav_phrases.xml` |
| Modify | `res/menu/bottom_nav_menu.xml` |
| Modify | `res/layout/fragment_translate.xml` |
| Modify | `feature/translate/TranslateFragment.kt` |
| Modify | `core/di/UseCaseModule.kt` |
| Modify | `core/di/ViewModelModule.kt` |
| Modify | `app/MainActivity.kt` |

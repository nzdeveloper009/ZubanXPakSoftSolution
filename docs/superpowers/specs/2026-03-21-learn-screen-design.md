# Learn Screen Design

**Date:** 2026-03-21
**Status:** Approved

## Goal

Replace the "Phrases" bottom navigation tab with a "Learn" hub screen that presents Phrases, Idioms, and Story as first-class content cards. Tapping a card navigates to the existing category grid for that content type.

## Background

Currently:
- Bottom nav has a "Phrases" tab that goes directly to the Phrases category grid
- Idioms and Story are accessible only via an overflow menu on the Phrases screen (hidden, not discoverable)

After this change:
- Bottom nav tab is renamed "Learn"
- Learn screen shows a 2-column grid of content type cards: Phrases, Idioms, Story
- Tapping each card navigates to its existing category grid
- Vocabulary and other content types can be added to the Learn grid in future plans

## Architecture

`LearnFragment` is a stateless fragment — no ViewModel, no MVI contract, no Koin registration. It holds a hardcoded list of 3 `LearnSection` items and navigates on click. This is appropriate because there is no state to manage (static list, no network calls, no persistence).

`LearnFragment` becomes the start destination of `nav_phrases.xml`. `PhrasesFragment` becomes a child destination reached via `action_learn_to_phrases`. Idioms and Story are reached via cross-graph navigation (`R.id.nav_idioms`, `R.id.nav_story`) — the same mechanism that was previously in the Phrases overflow menu.

## Navigation Graph

**Before (`nav_phrases.xml`):**
```
phrasesFragment (start) → phrasesCategoryFragment → phrasesZoomFragment
```

**After (`nav_phrases.xml`):**
```
learnFragment (start)
  └── action_learn_to_phrases → phrasesFragment → phrasesCategoryFragment → phrasesZoomFragment
```

Cross-graph navigation (to `nav_idioms` and `nav_story`) is called directly via `findNavController().navigate(R.id.nav_idioms)` and `findNavController().navigate(R.id.nav_story)` — same as before, now in `LearnFragment` instead of `PhrasesFragment`.

Back navigation from `PhrasesFragment` returns to `LearnFragment` automatically via `navigateUp()`.

## Files

### New
| File | Purpose |
|---|---|
| `feature/learn/LearnFragment.kt` | 2-column grid of LearnSection cards; inline ListAdapter; direct navigation on click |
| `res/layout/fragment_learn.xml` | FrameLayout with RecyclerView `@+id/rvSections` |
| `res/layout/item_learn_section.xml` | MaterialCardView with `ivSectionIcon` + `tvSectionName` (reuses existing card pattern) |

### Modified
| File | Change |
|---|---|
| `res/navigation/nav_phrases.xml` | Add `learnFragment` as start destination; add `action_learn_to_phrases` action to `phrasesFragment` |
| `res/menu/bottom_nav_menu.xml` | Change title of `nav_phrases` item from "Phrases" to `@string/nav_learn` |
| `feature/phrases/PhrasesFragment.kt` | Remove `MenuProvider` / overflow menu (Idioms/Stories navigation moves to LearnFragment) |
| `res/menu/menu_phrases.xml` | Delete file (unused after PhrasesFragment cleanup) |
| `res/values/strings.xml` | Add: `nav_learn`, `learn_section_phrases`, `learn_section_idioms`, `learn_section_story` |

### Unchanged
`MainActivity`, `nav_graph.xml`, `nav_idioms.xml`, `nav_story.xml`, all category/detail fragments, ViewModelModule.

## LearnSection Data

```kotlin
data class LearnSection(
    val titleRes: Int,
    val iconRes: Int,
    val navigate: () -> Unit
)
```

Defined inline in `LearnFragment`. The list is constructed in `onViewCreated` / `setupViews`:

```kotlin
listOf(
    LearnSection(R.string.learn_section_phrases, R.drawable.ic_nav_phrases) {
        findNavController().navigate(R.id.action_learn_to_phrases)
    },
    LearnSection(R.string.learn_section_idioms, R.drawable.ic_category_greeting) {
        findNavController().navigate(R.id.nav_idioms)
    },
    LearnSection(R.string.learn_section_story, R.drawable.ic_category_hotel) {
        findNavController().navigate(R.id.nav_story)
    }
)
```

Icons reuse existing drawables already in the project.

## String Resources

| Key | Value |
|---|---|
| `nav_learn` | "Learn" |
| `learn_section_phrases` | "Phrases" |
| `learn_section_idioms` | "Idioms" |
| `learn_section_story` | "Stories" |

## What Does NOT Change

- `nav_phrases` item ID in the bottom nav menu — stays as-is so `MainActivity` needs no changes
- `BOTTOM_NAV_HIDDEN_DESTINATIONS` in `MainActivity` — no changes needed
- All existing Phrases, Idioms, Story category and detail screens — untouched
- Koin `ViewModelModule` — no new ViewModels

## Out of Scope

- Vocabulary card (placeholder or implementation) — future plan
- Any UI changes to Phrases, Idioms, or Story category/detail screens

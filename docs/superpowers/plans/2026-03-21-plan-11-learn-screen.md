# Learn Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the "Phrases" bottom nav tab with a "Learn" hub screen that shows Phrases, Idioms, and Story as tappable cards, making these features discoverable from one place.

**Architecture:** A new stateless `LearnFragment` (no ViewModel, no MVI contract, no Koin registration) becomes the start destination of `nav_phrases.xml`. It holds a hardcoded list of 3 `LearnSection` items and navigates on click. `PhrasesFragment` becomes a child destination reached via Safe Args action. The bottom nav item ID `nav_phrases` is unchanged so `MainActivity` needs zero modifications.

**Tech Stack:** Kotlin, MVI (BaseFragment only — no ViewModel needed), ViewBinding, Navigation Component Safe Args, Koin (no new registrations), RecyclerView ListAdapter.

---

## File Structure

### New
| File | Responsibility |
|---|---|
| `app/src/main/java/com/android/zubanx/feature/learn/LearnFragment.kt` | Stateless fragment: 2-column grid of 3 LearnSection cards, navigates on click |
| `app/src/main/res/layout/fragment_learn.xml` | FrameLayout with RecyclerView `@+id/rvSections` |
| `app/src/main/res/layout/item_learn_section.xml` | MaterialCardView with `ivSectionIcon` + `tvSectionName` |

### Modified
| File | Change |
|---|---|
| `app/src/main/res/navigation/nav_phrases.xml` | Change `app:startDestination` to `learnFragment`; add `learnFragment` entry with `action_learn_to_phrases`; add slide animations to `action_phrases_to_category` |
| `app/src/main/res/menu/bottom_nav_menu.xml` | Change `nav_phrases` item title to `@string/nav_learn` |
| `app/src/main/java/com/android/zubanx/feature/phrases/PhrasesFragment.kt` | Remove `MenuProvider` / overflow menu block |
| `app/src/main/res/values/strings.xml` | Add `nav_learn`, `learn_section_phrases`, `learn_section_idioms`, `learn_section_story` |

### Deleted
| File | Reason |
|---|---|
| `app/src/main/res/menu/menu_phrases.xml` | Overflow menu removed from PhrasesFragment; file is unused |

---

## Task 1: Add string resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add Learn strings to strings.xml**

Open `app/src/main/res/values/strings.xml`. After the `<!-- Phrases -->` section (after line containing `phrases_cd_swap_languages`), add:

```xml
    <!-- Learn -->
    <string name="nav_learn">Learn</string>
    <string name="learn_section_phrases">Phrases</string>
    <string name="learn_section_idioms">Idioms</string>
    <string name="learn_section_story">Stories</string>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat(learn): add Learn screen string resources"
```

---

## Task 2: Learn layouts

**Files:**
- Create: `app/src/main/res/layout/fragment_learn.xml`
- Create: `app/src/main/res/layout/item_learn_section.xml`

- [ ] **Step 1: Create fragment_learn.xml**

Same structure as `fragment_phrases.xml` — single RecyclerView, but with ID `rvSections`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rvSections"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:padding="12dp"
        android:clipToPadding="false" />

</FrameLayout>
```

- [ ] **Step 2: Create item_learn_section.xml**

Larger cards than category items — 2-column grid, so cards can be taller. Copy of `item_phrase_category.xml` with height increased to 120dp and icon to 40dp:

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.card.MaterialCardView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/cardSection"
    android:layout_width="match_parent"
    android:layout_height="120dp"
    android:layout_margin="8dp"
    app:cardElevation="3dp"
    app:cardCornerRadius="16dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:gravity="center"
        android:padding="12dp">

        <ImageView
            android:id="@+id/ivSectionIcon"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:contentDescription="@null" />

        <TextView
            android:id="@+id/tvSectionName"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="8dp"
            android:textAppearance="?attr/textAppearanceTitleSmall"
            android:gravity="center"
            android:maxLines="1" />

    </LinearLayout>

</com.google.android.material.card.MaterialCardView>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/fragment_learn.xml \
        app/src/main/res/layout/item_learn_section.xml
git commit -m "feat(learn): add Learn screen layouts"
```

---

## Task 3: LearnFragment

**Files:**
- Create: `app/src/main/java/com/android/zubanx/feature/learn/LearnFragment.kt`

- [ ] **Step 1: Create LearnFragment.kt**

```kotlin
package com.android.zubanx.feature.learn

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.zubanx.R
import com.android.zubanx.core.base.BaseFragment
import com.android.zubanx.core.navigation.safeNavigate
import com.android.zubanx.databinding.FragmentLearnBinding
import com.android.zubanx.databinding.ItemLearnSectionBinding

data class LearnSection(
    val titleRes: Int,
    val iconRes: Int,
    val navigate: () -> Unit
)

class LearnFragment : BaseFragment<FragmentLearnBinding>(FragmentLearnBinding::inflate) {

    private lateinit var sections: List<LearnSection>
    private lateinit var sectionAdapter: SectionAdapter

    override fun setupViews() {
        sections = listOf(
            LearnSection(R.string.learn_section_phrases, R.drawable.ic_nav_phrases) {
                safeNavigate(LearnFragmentDirections.actionLearnToPhrases())
            },
            LearnSection(R.string.learn_section_idioms, R.drawable.ic_category_greeting) {
                // cross-graph: no Safe Args directions available for included graphs
                findNavController().navigate(R.id.nav_idioms)
            },
            LearnSection(R.string.learn_section_story, R.drawable.ic_category_hotel) {
                // cross-graph: no Safe Args directions available for included graphs
                findNavController().navigate(R.id.nav_story)
            }
        )

        sectionAdapter = SectionAdapter()
        binding.rvSections.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvSections.adapter = sectionAdapter
        sectionAdapter.submitList(sections)
    }

    inner class SectionAdapter : ListAdapter<LearnSection, SectionAdapter.VH>(DIFF) {

        inner class VH(val b: ItemLearnSectionBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemLearnSectionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val section = getItem(position)
            holder.b.ivSectionIcon.setImageResource(section.iconRes)
            holder.b.tvSectionName.setText(section.titleRes)
            holder.b.cardSection.setOnClickListener { section.navigate() }
        }

        companion object {
            val DIFF = object : DiffUtil.ItemCallback<LearnSection>() {
                override fun areItemsTheSame(a: LearnSection, b: LearnSection) =
                    a.titleRes == b.titleRes
                override fun areContentsTheSame(a: LearnSection, b: LearnSection) =
                    a.titleRes == b.titleRes && a.iconRes == b.iconRes
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/android/zubanx/feature/learn/LearnFragment.kt
git commit -m "feat(learn): add LearnFragment with 3-section grid"
```

---

## Task 4: Update nav_phrases.xml

**Files:**
- Modify: `app/src/main/res/navigation/nav_phrases.xml`

- [ ] **Step 1: Replace nav_phrases.xml**

The full updated file — `learnFragment` is the new start destination, `phrasesFragment` is now a child reached via `action_learn_to_phrases`. Slide animations added for consistency with the rest of the app:

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_phrases"
    android:label="Learn"
    app:startDestination="@id/learnFragment">

    <fragment
        android:id="@+id/learnFragment"
        android:name="com.android.zubanx.feature.learn.LearnFragment"
        android:label="Learn">
        <action
            android:id="@+id/action_learn_to_phrases"
            app:destination="@id/phrasesFragment"
            app:enterAnim="@anim/slide_in_right"
            app:exitAnim="@anim/slide_out_left"
            app:popEnterAnim="@anim/slide_in_left"
            app:popExitAnim="@anim/slide_out_right" />
    </fragment>

    <fragment
        android:id="@+id/phrasesFragment"
        android:name="com.android.zubanx.feature.phrases.PhrasesFragment"
        android:label="Phrases">
        <action
            android:id="@+id/action_phrases_to_category"
            app:destination="@id/phrasesCategoryFragment"
            app:enterAnim="@anim/slide_in_right"
            app:exitAnim="@anim/slide_out_left"
            app:popEnterAnim="@anim/slide_in_left"
            app:popExitAnim="@anim/slide_out_right" />
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

**Critical:** Verify that `@anim/slide_in_right`, `@anim/slide_out_left`, `@anim/slide_in_left`, `@anim/slide_out_right` exist before committing. Run:
```bash
ls app/src/main/res/anim/
```
If any are missing, omit the animation attributes on that action.

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/navigation/nav_phrases.xml
git commit -m "feat(learn): make learnFragment start destination of nav_phrases"
```

---

## Task 5: Update bottom nav + clean up PhrasesFragment + delete menu file

**Files:**
- Modify: `app/src/main/res/menu/bottom_nav_menu.xml`
- Modify: `app/src/main/java/com/android/zubanx/feature/phrases/PhrasesFragment.kt`
- Delete: `app/src/main/res/menu/menu_phrases.xml`

- [ ] **Step 1: Update bottom_nav_menu.xml — rename Phrases → Learn**

Change only the `android:title` on the `nav_phrases` item. Do not change the ID:

```xml
    <item
        android:id="@+id/nav_phrases"
        android:icon="@drawable/ic_nav_phrases"
        android:title="@string/nav_learn" />
```

- [ ] **Step 2: Remove overflow menu from PhrasesFragment.kt**

Read the current `PhrasesFragment.kt`. Remove the entire `requireActivity().addMenuProvider(...)` block (lines 34–45 in current file). Also remove the now-unused imports:

Remove these imports:
```kotlin
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
```

The `setupViews()` method should now look like:

```kotlin
override fun setupViews() {
    binding.rvCategories.layoutManager = GridLayoutManager(requireContext(), 3)
    binding.rvCategories.adapter = adapter
}
```

- [ ] **Step 3: Delete menu_phrases.xml**

```bash
git rm app/src/main/res/menu/menu_phrases.xml
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/menu/bottom_nav_menu.xml \
        app/src/main/java/com/android/zubanx/feature/phrases/PhrasesFragment.kt
git commit -m "feat(learn): rename bottom nav tab to Learn, remove Phrases overflow menu"
```

---

## Task 6: Build verification

- [ ] **Step 1: Build**

```bash
JAVA_HOME=/Users/nokhaiz/.gradle/jdks/jetbrains_s_r_o_-17-aarch64-os_x.2/jbrsdk_jcef-17.0.14-osx-aarch64-b1367.22/Contents/Home ./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`

Common failure causes:
- `LearnFragmentDirections` not generated → Safe Args only generates after a successful build; ensure `nav_phrases.xml` is saved with `learnFragment` correctly defined before building
- Missing drawable reference → check `ic_category_greeting` and `ic_category_hotel` exist: `ls app/src/main/res/drawable/ic_category_greeting.xml app/src/main/res/drawable/ic_category_hotel.xml`
- `menu_phrases.xml` still referenced → ensure the `MenuProvider` import and block were fully removed from `PhrasesFragment.kt`

- [ ] **Step 2: Run unit tests**

```bash
JAVA_HOME=/Users/nokhaiz/.gradle/jdks/jetbrains_s_r_o_-17-aarch64-os_x.2/jbrsdk_jcef-17.0.14-osx-aarch64-b1367.22/Contents/Home ./gradlew :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Final commit if any fixes were needed**

```bash
git add -A
git commit -m "fix(learn): build fixes"
```

---

## Notes for Implementer

1. **`LearnFragmentDirections` is generated by Safe Args** — it will not exist until you add `learnFragment` to `nav_phrases.xml` (Task 4) and run a build. If your IDE shows an unresolved reference before building, that is expected.

2. **`safeNavigate` signature** — it only accepts `NavDirections`. The cross-graph calls (`R.id.nav_idioms`, `R.id.nav_story`) use raw `findNavController().navigate(Int)` because no `NavDirections` exist for included graphs. This is intentional and consistent with how `PhrasesFragment` previously did the same navigation.

3. **`ic_category_greeting` and `ic_category_hotel`** are placeholder icons reusing existing drawables. The spec notes these should be replaced with proper Learn-section icons before release.

4. **No ViewModel, no Koin** — `LearnFragment` does not have a ViewModel. Do not add `viewModelOf(::LearnViewModel)` to `ViewModelModule.kt`. The fragment is intentionally stateless.

5. **`menu_idioms` and `menu_stories` strings** remain in `strings.xml` after this change but are no longer referenced by any layout or menu XML. They are harmless (unused strings do not cause build errors) but can be removed in a future cleanup pass.

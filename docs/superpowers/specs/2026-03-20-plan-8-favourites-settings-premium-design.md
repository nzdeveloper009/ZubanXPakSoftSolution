# Plan 8: Favourites, Settings & Premium — Design Spec

## Goal

Implement three interconnected screens: a Favourites list (translate + dictionary tabs), a Settings screen (preferences + navigation hub), and a Premium screen with real Google Play in-app subscription billing.

## Architecture

Clean MVI with strict separation: domain use cases own all business logic, ViewModels hold state and dispatch effects, Fragments are purely reactive. All Room operations are Flow-based (no blocking reads on main thread). BillingManager is a singleton that owns the full Play Billing lifecycle and exposes a `StateFlow<BillingState>` — the ViewModel observes this, never holds an Activity reference. Swipe-to-delete follows a restore-first pattern to prevent ANR from blocking the main thread during a confirmation dialog.

## Tech Stack

- Google Play Billing Library KTX 7.x (`com.android.billingclient:billing-ktx`)
- Room 3 (migration v1 → v2)
- DataStore Preferences
- Koin 4.1.1 (`singleOf`, `factoryOf`, `viewModelOf`)
- Navigation Component 2.9.0 with Safe Args
- Material3 components (TabLayout, MaterialCardView, MaterialAlertDialog, BottomSheetDialogFragment)

---

## Section 1: Data Layer Changes

### 1.1 Room Migration v1 → v2

Add a `category` column to the `favourites` table. Default value `'translate'` preserves all existing rows.

**Migration SQL:**
```sql
ALTER TABLE favourites ADD COLUMN category TEXT NOT NULL DEFAULT 'translate'
```

File: `data/local/db/migration/Migrations.kt`
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE favourites ADD COLUMN category TEXT NOT NULL DEFAULT 'translate'")
    }
}
```

`ZubanDatabase` bumps to `version = 2` and receives `.addMigrations(MIGRATION_1_2)` in `DatabaseModule`.

### 1.2 FavouriteEntity

Add field: `val category: String = "translate"`

**Category constants** (companion object in `FavouriteEntity` or a top-level object):
```kotlin
object FavouriteCategory {
    const val TRANSLATE = "translate"
    const val DICTIONARY = "dictionary"
}
```

### 1.3 FavouriteDao

Add one new query — existing queries unchanged:
```kotlin
@Query("SELECT * FROM favourites WHERE category = :category ORDER BY timestamp DESC")
fun getAllByCategory(category: String): Flow<List<FavouriteEntity>>
```

### 1.4 Favourite Domain Model

Add field: `val category: String = FavouriteCategory.TRANSLATE`

### 1.5 FavouriteRepository + Impl

Add to interface:
```kotlin
fun getByCategory(category: String): Flow<List<Favourite>>
```

`FavouriteRepositoryImpl` maps `dao.getAllByCategory(category)` via `FavouriteMapper.toDomain()`.

### 1.6 New Domain Use Cases (`domain/usecase/favourite/`)

| File | Responsibility |
|------|---------------|
| `GetFavouritesByCategoryUseCase` | `operator fun invoke(category: String): Flow<List<Favourite>>` — delegates to repository |
| `DeleteFavouriteUseCase` | `suspend operator fun invoke(id: Long)` — calls `repository.remove(id)` |
| `AddDictionaryFavouriteUseCase` | `suspend operator fun invoke(word, definition, language)` — builds a `Favourite` with `category = DICTIONARY` and calls `repository.add()` |

### 1.7 AppPreferences — New Keys

Add to interface and `AppPreferencesImpl`:
```kotlin
val autoSpeak: Flow<Boolean>
val floatingOverlay: Flow<Boolean>
suspend fun setAutoSpeak(value: Boolean)
suspend fun setFloatingOverlay(value: Boolean)
```

DataStore keys: `booleanPreferencesKey("auto_speak")`, `booleanPreferencesKey("floating_overlay")`. Both default to `false`.

### 1.8 ANR Fix — WordDetailFragment

Replace existing `runBlocking { repository.getCached(...) }` with a lifecycle-safe coroutine:
```kotlin
override fun setupViews() {
    lifecycleScope.launch {
        val entry = withContext(Dispatchers.IO) {
            repository.getCached(args.word, args.language)
        } ?: DictionaryEntry(word = args.word, language = args.language, definition = "", timestamp = 0L)
        viewModel.onEvent(WordDetailContract.Event.Load(entry))
    }
    // button click listeners set up here, before data loads
}
```

---

## Section 2: BillingManager

File: `billing/BillingManager.kt`
DI: `singleOf(::BillingManager)` in new `core/di/BillingModule.kt`.
Connected once from `ZubanApp.onCreate()`.

### 2.1 BillingState

File: `billing/BillingState.kt`
```kotlin
sealed interface BillingState {
    object Loading : BillingState
    data class Ready(val plans: List<PremiumPlan>) : BillingState
    object Purchased : BillingState
    data class Error(val message: String) : BillingState
}
```

### 2.2 PremiumProductIds

File: `billing/PremiumProductIds.kt`
```kotlin
object PremiumProductIds {
    const val WEEKLY  = "zubanx_premium_weekly"
    const val MONTHLY = "zubanx_premium_monthly"
    const val YEARLY  = "zubanx_premium_yearly"
}
```

### 2.3 PremiumPlan Domain Model

File: `domain/model/PremiumPlan.kt`
```kotlin
data class PremiumPlan(
    val productId: String,
    val planType: PlanType,
    val title: String,
    val price: String,
    val isDefault: Boolean = false
)
enum class PlanType { WEEKLY, MONTHLY, YEARLY }
```

### 2.4 BillingManager Responsibilities

1. **connect()** — calls `billingClient.startConnection(this)`. Retries on `BillingClient.BillingResponseCode.SERVICE_DISCONNECTED`.
2. **queryProducts()** — called from `onBillingSetupFinished`. Queries all three subscription product IDs. Maps `ProductDetails` → `PremiumPlan` (price from `subscriptionOfferDetails`).
3. **launchPurchaseFlow(activity, plan)** — called from Fragment. Builds `BillingFlowParams` with the matching `ProductDetails` and calls `billingClient.launchBillingFlow(activity, params)`.
4. **onPurchasesUpdated(result, purchases)** — runs on main thread; immediately dispatches acknowledgment to IO:
   ```kotlin
   viewModelScope.launch(Dispatchers.IO) {
       purchases?.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
           ?.forEach { acknowledgePurchase(it) }
   }
   ```
5. **acknowledgePurchase(purchase)** — calls `billingClient.acknowledgePurchase(...)`. On success, calls `appPreferences.setIsPremium(true)` and emits `BillingState.Purchased`.
6. **restorePurchases()** — queries existing purchases via `billingClient.queryPurchasesAsync(QueryPurchasesParams)`.

`BillingManager` exposes `val billingState: StateFlow<BillingState>` backed by a `MutableStateFlow`.

---

## Section 3: Favourites Feature

### 3.1 FavouriteContract

File: `feature/favourite/FavouriteContract.kt`

```kotlin
object FavouriteContract {
    enum class Tab { TRANSLATE, DICTIONARY }

    data class Active(
        val translateItems: List<Favourite> = emptyList(),
        val dictionaryItems: List<Favourite> = emptyList(),
        val selectedTab: Tab = Tab.TRANSLATE
    ) : UiState

    sealed class Event : UiEvent {
        data class TabSelected(val tab: Tab) : Event()
        data class DeleteConfirmed(val id: Long) : Event()
        data class ItemClicked(val item: Favourite) : Event()
        data class SpeakText(val text: String, val lang: String) : Event()
        data class CopyText(val text: String) : Event()
        data class ShareText(val text: String) : Event()
    }

    sealed class Effect : UiEffect {
        data class ConfirmDelete(val id: Long) : Effect()
        data class OpenTranslateDetail(val item: Favourite) : Effect()
        data class NavigateToWordDetail(val word: String, val language: String) : Effect()
        data class CopyToClipboard(val text: String) : Effect()
        data class Share(val text: String) : Effect()
        data class Speak(val text: String, val lang: String) : Effect()
        data class ShowToast(val message: String) : Effect()
    }
}
```

### 3.2 FavouriteViewModel

- Collects both categories via `GetFavouritesByCategoryUseCase` in `init` using `viewModelScope.launch` + `collect`.
- `DeleteConfirmed` → launches coroutine calling `DeleteFavouriteUseCase(id)`.
- `ItemClicked` → sends `OpenTranslateDetail` or `NavigateToWordDetail` based on `item.category`.
- No blocking calls; all state updates via `setState {}`.

### 3.3 FavouriteFragment

Layout: `fragment_favourite.xml`
- `TabLayout` with two tabs ("Translate", "Dictionary")
- `RecyclerView` id `rvTranslate` (visible by default)
- `RecyclerView` id `rvDictionary` (gone by default)
- Empty state `TextView` per list

**ANR-safe swipe-to-delete** (enforced pattern):
```kotlin
val callback = object : ItemTouchHelper.SimpleCallback(0, LEFT or RIGHT) {
    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val item = adapter.currentList[viewHolder.bindingAdapterPosition]
        adapter.notifyItemChanged(viewHolder.bindingAdapterPosition) // restore immediately
        viewModel.onEvent(FavouriteContract.Event.ItemClicked(item)) // triggers ConfirmDelete effect
    }
}
```
Fragment observes `Effect.ConfirmDelete` → shows `MaterialAlertDialog` → on positive button calls `onEvent(DeleteConfirmed(id))`.

**Detail bottom sheet** (`FavouriteDetailBottomSheet`): BottomSheetDialogFragment receiving a `Favourite` as an argument (parcelize or pass via ViewModel). Shows source text, translated text, language pair label, and three buttons: Copy, Share, Speak — each delegates back to the parent Fragment via a listener interface or shared ViewModel event.

### 3.4 Item Layout

`item_favourite.xml` — `MaterialCardView` root:
- `tvSourceText`, `tvTranslatedText`, `tvLangPair` (e.g., "EN → UR")
- `tvTimestamp` (formatted via `DateTimeUtils`)
- Swipe surface handled by `ItemTouchHelper` (no swipe buttons in XML)

---

## Section 4: Settings Feature

### 4.1 SettingsContract

File: `feature/settings/SettingsContract.kt`

```kotlin
object SettingsContract {
    data class State(
        val isPremium: Boolean = false,
        val aiTone: String = "Original",
        val offlineMode: Boolean = false,
        val floatingOverlay: Boolean = false,
        val autoSpeak: Boolean = false,
        val appVersion: String = ""
    ) : UiState

    sealed class Event : UiEvent {
        object NavigateToPremium : Event()
        object NavigateToHistory : Event()
        object NavigateToFavourites : Event()
        object NavigateToAiTone : Event()
        object NavigateToLanguage : Event()
        object OpenPrivacyPolicy : Event()
        object OpenTerms : Event()
        object RateUs : Event()
        object ShareApp : Event()
        object ContactSupport : Event()
        data class SetOfflineMode(val enabled: Boolean) : Event()
        data class SetFloatingOverlay(val enabled: Boolean) : Event()
        data class SetAutoSpeak(val enabled: Boolean) : Event()
    }

    sealed class Effect : UiEffect {
        data class Navigate(val actionId: Int) : Effect()
        data class OpenUrl(val url: String) : Effect()
        object LaunchShare : Effect()
        object LaunchRateUs : Effect()
        object LaunchContactSupport : Effect()
        data class StartFloatingService(val enable: Boolean) : Effect()
        data class ShowToast(val message: String) : Effect()
    }
}
```

### 4.2 SettingsViewModel

- Collects `isPremium`, `offlineMode`, `autoSpeak`, `floatingOverlay`, `selectedExpert` from `AppPreferences` in `init` using `combine` → updates state.
- `appVersion` set from `BuildConfig.VERSION_NAME` in `init`.
- Toggle events call `appPreferences.setX(value)` in coroutines.
- `SetFloatingOverlay` additionally sends `Effect.StartFloatingService(enabled)`.
- Navigation events send `Effect.Navigate(actionId)`.

### 4.3 SettingsFragment Layout

`fragment_settings.xml` — `NestedScrollView` → `LinearLayout`:

1. **Premium card** (`MaterialCardView`): "Upgrade to Premium" or "Premium Active ✓" — `setOnClickListener` dispatches `NavigateToPremium`
2. **Features card**: Three `ConstraintLayout` rows:
   - History (icon + label) → `NavigateToHistory`
   - Favourite (icon + label) → `NavigateToFavourites`
   - AI Tone (icon + label + trailing `tvAiTone` in blue `colorPrimary`) → `NavigateToAiTone`
3. **Toggles card**: Three rows each with icon, label, `MaterialSwitch`:
   - Translate Offline → `switchOffline` → `SetOfflineMode`
   - Floating Icon → `switchFloating` → `SetFloatingOverlay`
   - Auto Speak → `switchAutoSpeak` → `SetAutoSpeak`
4. **App card**: Five rows (icon + label, no trailing):
   - Language, Privacy Policy, Rate Us, Share App, Customer Support
5. **Version `TextView`** centered: `"v${state.appVersion}"`

Fragment observes state and sets switch values programmatically (with listener temporarily removed to avoid feedback loop).

---

## Section 5: Premium Feature

### 5.1 PremiumContract

File: `feature/premium/PremiumContract.kt`

```kotlin
object PremiumContract {
    data class State(
        val plans: List<PremiumPlan> = emptyList(),
        val selectedPlan: PremiumPlan? = null,
        val isPurchasing: Boolean = false,
        val isPremium: Boolean = false,
        val isLoading: Boolean = true,
        val errorMessage: String? = null
    ) : UiState

    sealed class Event : UiEvent {
        data class SelectPlan(val plan: PremiumPlan) : Event()
        object Purchase : Event()
        object RestorePurchase : Event()
        object OpenPrivacyPolicy : Event()
        object OpenTerms : Event()
    }

    sealed class Effect : UiEffect {
        data class LaunchBillingFlow(val plan: PremiumPlan) : Effect()
        data class OpenUrl(val url: String) : Effect()
        data class ShowToast(val message: String) : Effect()
        object NavigateBack : Effect()
    }
}
```

### 5.2 PremiumViewModel

- Injects `BillingManager` and `AppPreferences`.
- In `init`: collects `billingState` flow and maps to `State`:
  - `BillingState.Loading` → `isLoading = true`
  - `BillingState.Ready(plans)` → `isLoading = false, plans = plans, selectedPlan = plans.firstOrNull { it.planType == WEEKLY }`
  - `BillingState.Purchased` → `isPremium = true` + sends `Effect.NavigateBack`
  - `BillingState.Error(msg)` → `errorMessage = msg`
- Collects `appPreferences.isPremium` to reflect existing premium state.
- `Purchase` event → sends `Effect.LaunchBillingFlow(selectedPlan!!)`.
- `RestorePurchase` event → calls `billingManager.restorePurchases()`.

### 5.3 PremiumFragment Layout

`fragment_premium.xml` — `NestedScrollView` → `LinearLayout`:

1. **Header**: App icon + `"ZubanX Premium"` title
2. **Feature table** (static `LinearLayout` — 6 rows, no RecyclerView):
   Each row: `ImageView` icon | `TextView` feature name | `TextView` "✓" premium col | `TextView` "✓" or "—" basic col
   - Translate Any Text → ✓ / ✓
   - All Languages Support → ✓ / ✓
   - Camera + Voice Translate → ✓ / ✓
   - AI Agent → ✓ / —
   - VIP Customer Support → ✓ / —
   - Remove Ads → ✓ / —
3. **Plan cards** (3 × `MaterialCardView`, horizontal or stacked):
   Weekly (default selected border/elevation), Monthly, Yearly.
   Each shows plan title + price from `PremiumPlan.price`.
   `isSelected` state drives a `colorPrimary` stroke via `strokeColor`.
4. **Subscribe button** (`MaterialButton`): disabled when `isLoading` or `isPurchasing`; shows `ProgressBar` when `isPurchasing`
5. **Restore Purchase** text button
6. **Privacy Policy | Terms of Service** — `SpannableString` clickable links dispatching events

Fragment handles `Effect.LaunchBillingFlow`:
```kotlin
is PremiumContract.Effect.LaunchBillingFlow -> {
    billingManager.launchPurchaseFlow(requireActivity(), effect.plan)
}
```
`BillingManager` injected into Fragment via `by inject()`.

---

## Section 6: Navigation Wiring

### 6.1 nav_favourite.xml
```xml
<navigation android:id="@+id/nav_favourite" app:startDestination="@id/favouriteFragment">
    <fragment android:id="@+id/favouriteFragment" android:name="...FavouriteFragment" />
</navigation>
```

### 6.2 nav_settings.xml
```xml
<navigation android:id="@+id/nav_settings" app:startDestination="@id/settingsFragment">
    <fragment android:id="@+id/settingsFragment" android:name="...SettingsFragment">
        <action android:id="@+id/action_settings_to_premium" app:destination="@id/nav_premium" />
        <action android:id="@+id/action_settings_to_favourite" app:destination="@id/nav_favourite" />
    </fragment>
</navigation>
```

### 6.3 nav_premium.xml
```xml
<navigation android:id="@+id/nav_premium" app:startDestination="@id/premiumFragment">
    <fragment android:id="@+id/premiumFragment" android:name="...PremiumFragment" />
</navigation>
```

Settings and Favourites are wired as bottom nav top-level destinations in `MainActivity`. Premium is launched from Settings (not a bottom nav item).

---

## Section 7: DI Registration

### New BillingModule (`core/di/BillingModule.kt`)
```kotlin
val billingModule = module {
    singleOf(::BillingManager)
}
```

### UseCaseModule additions
```kotlin
factoryOf(::GetFavouritesByCategoryUseCase)
factoryOf(::DeleteFavouriteUseCase)
factoryOf(::AddDictionaryFavouriteUseCase)
```

### ViewModelModule additions
```kotlin
viewModelOf(::FavouriteViewModel)
viewModelOf(::SettingsViewModel)
viewModelOf(::PremiumViewModel)
```

### ZubanApp.onCreate addition
```kotlin
get<BillingManager>().connect()
```

---

## File Map

| Action | File |
|--------|------|
| Modify | `FavouriteEntity.kt` — add `category` |
| Modify | `FavouriteDao.kt` — add `getAllByCategory` |
| Modify | `Favourite.kt` — add `category` |
| Modify | `FavouriteRepository.kt` + impl — add `getByCategory` |
| Modify | `AppPreferences.kt` + impl — add `autoSpeak`, `floatingOverlay` |
| Modify | `ZubanDatabase.kt` — v2 + migration |
| Modify | `DatabaseModule.kt` — add migration |
| Modify | `UseCaseModule.kt` — 3 new use cases |
| Modify | `ViewModelModule.kt` — 3 new ViewModels |
| Modify | `WordDetailFragment.kt` — fix runBlocking |
| Modify | `ZubanApp.kt` — connect BillingManager |
| Create | `data/local/db/migration/Migrations.kt` |
| Create | `billing/BillingManager.kt` |
| Create | `billing/BillingState.kt` |
| Create | `billing/PremiumProductIds.kt` |
| Create | `domain/model/PremiumPlan.kt` |
| Create | `domain/usecase/favourite/GetFavouritesByCategoryUseCase.kt` |
| Create | `domain/usecase/favourite/DeleteFavouriteUseCase.kt` |
| Create | `domain/usecase/favourite/AddDictionaryFavouriteUseCase.kt` |
| Create | `core/di/BillingModule.kt` |
| Create | `feature/favourite/FavouriteContract.kt` |
| Create | `feature/favourite/FavouriteViewModel.kt` |
| Create | `feature/favourite/FavouriteFragment.kt` |
| Create | `feature/favourite/FavouriteDetailBottomSheet.kt` |
| Create | `feature/settings/SettingsContract.kt` |
| Create | `feature/settings/SettingsViewModel.kt` |
| Create | `feature/settings/SettingsFragment.kt` |
| Create | `feature/premium/PremiumContract.kt` |
| Create | `feature/premium/PremiumViewModel.kt` |
| Create | `feature/premium/PremiumFragment.kt` |
| Create | Layouts: `fragment_favourite.xml`, `item_favourite.xml`, `fragment_settings.xml`, `fragment_premium.xml`, `bottom_sheet_favourite_detail.xml` |
| Modify | `nav_favourite.xml`, `nav_settings.xml`, `nav_premium.xml` |

---

## ANR Prevention Summary

| Risk | Mitigation |
|------|-----------|
| Room reads on main thread | All queries via `Flow` — Room emits on IO, collected on Main |
| Swipe-to-delete blocking | Item restored immediately; delete only after dialog confirmation |
| BillingClient callbacks | `onPurchasesUpdated` dispatches acknowledgment to `Dispatchers.IO` |
| `WordDetailFragment.runBlocking` | Replaced with `lifecycleScope.launch + withContext(IO)` |
| Switch listeners causing feedback loops | Listener removed before setting value programmatically, re-attached after |
| `ListAdapter.DiffUtil` | Already async — no action needed |

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Galva is a local-only Android gallery app: Kotlin, Jetpack Compose, a single `:app` module (package
`com.abrarshakhi.galva`, minSdk 29, target/compile SDK 37). It mirrors the device's MediaStore photos
and videos into Room and serves every screen from that index. Nothing is uploaded or moved on disk.
The manifest has no INTERNET permission, and the Ktor/koin-ktor dependencies are declared but unused.

## Commands

```bash
./gradlew :app:assembleDebug              # debug APK
./gradlew :app:installDebug               # build + install on a connected device
./gradlew :app:testDebugUnitTest          # JVM unit tests
./gradlew :app:testDebugUnitTest --tests "com.abrarshakhi.galva.ExampleUnitTest.addition_isCorrect"
./gradlew :app:connectedDebugAndroidTest  # instrumented tests (device/emulator required)
./gradlew :app:lintDebug                  # Android Lint
```

- `gradlew` needs `JAVA_HOME` or `java` on `PATH` (JDK 17+) to start. Android Studio's bundled JBR
  works. The Gradle daemon itself runs on JDK 25 (`gradle/gradle-daemon-jvm.properties`) and is
  auto-provisioned if missing. Sources compile to JVM 17.
- The Android SDK comes from `local.properties` (`sdk.dir`) or `ANDROID_HOME`.
- Dependencies live in `gradle/libs.versions.toml`. R8 keep rules go in `app/src/main/keepRules/`
  (AGP 9 layout; there is no `proguard-rules.pro`).
- Unit tests so far cover the Secrets vault (`core/vault`); everything else has only template tests.
  `kotlinx-coroutines-test` and `koin-test` are on the unit-test classpath, and the code has test
  seams: the single injected `CoroutineDispatcher`, the `MediaStoreReader` interface (fake it to
  exercise the sync deletion guard), `ObserveTimelineUseCase(zoneId = …)`, the vault's
  `HardwareKeyRing`, `PassphraseKdf` and `SecretEncryptor`/`SecretExporter` interfaces, and pure
  layout functions (`adaptiveColumnCount`, `albumGridColumns`, `chromeLayoutFor`).
- `lintDebug` currently fails on two errors that predate the vault work, in `DateLabels.kt` and
  `DraggableScrollbar.kt`.

## Architecture

```
MediaStore ──► MediaSyncManager ──► Room ──► Repository ──► UseCase ──► MviViewModel ──► Compose
   ▲                  ▲                                                        │
   └── ContentObserver┘                                               Intent ◄─┘
```

Source root `app/src/main/java/com/abrarshakhi/galva/`: `common/` (app shell, MVI base, navigation,
Koin modules, shared UI, theme), `core/` (domain and data for `media`, `settings`, `permission`,
`share`), and `features/<name>/presentation/` (presentation only).

### Data layer invariants

- **Repositories read only from Room.** MediaStore is touched only by `MediaSyncManager` (through
  `MediaStoreReader`), `MediaStoreObserver`, the Coil thumbnail fetcher, the delete flow, and the
  vault's `VaultImporter`/`VaultRestorer`.
- **Sync is a fingerprint diff** on `_ID` + `DATE_MODIFIED`. Only new or changed rows are re-read, the
  delta is applied in one transaction (`MediaDao.applySyncDelta`), and a `Mutex` serialises passes, so
  `sync()` is safe to call repeatedly.
- **User-authored data cascades off `media`.** `favorites` and `user_album_members` have
  `ON DELETE CASCADE` foreign keys to `media`, so deleting media rows silently destroys favourites and
  album membership. Three rules follow from that:
  - Keep the sync deletion guard. An empty MediaStore read is never applied, and removing ≥50% of an
    index of ≥20 items needs a second read that agrees. Don't loosen it.
  - App-owned state goes in its own tables, never in columns on `MediaEntity`, because sync upserts
    those rows freely.
  - There is no destructive migration fallback. For a schema change, bump `version` in
    `GalvaDatabase`, write a `Migration` in `GalvaMigrations.kt`, register it in `databaseModule`
    (`AppModules.kt`), and commit the regenerated JSON under `app/schemas/`.
- **Identity:** `MediaEntity.id` *is* the MediaStore `_ID`. Albums are identified by the sealed
  `AlbumRef` (`Device(bucketId)` / `Favorites` / `User(id)`). Bucket ids can be negative, so don't
  reduce album identity to a raw `Long`. Use `AlbumRef.key` for list keys and ViewModel keys. Screens
  hand the data layer a `MediaSource` (`AllMedia` / `Album` / `Query`), not a list.
- Domain models hold URIs as `String` (no Android types in `domain/`). All timestamps are epoch millis.
  MediaStore's `DATE_MODIFIED` is in seconds and is converted in `MediaStoreDataSource`.
- SQLite caps bind variables (999 on older API levels), so bulk `IN (…)` statements get chunked (see
  `MediaDao.applySyncDelta`, `MediaStoreDataSource.queryByIds`).
- **Deletes are two-phase**, because scoped storage requires a system consent dialog:
  1. `MediaRepository.delete()` deletes nothing and returns `DeleteOutcome.NeedsConsent(uris)`.
  2. The ViewModel emits a `ConfirmDelete` effect.
  3. The screen calls `rememberMediaDeleteLauncher` (`createDeleteRequest` on API 30+,
     `RecoverableSecurityException` on API 29).
  4. The answer comes back as a `DeleteResolved` intent. Only a confirmed delete prunes the index,
     via `MediaSelectionActions.confirmDeleted`.

### Presentation (MVI)

- Each screen has a `*Contract.kt` (`UiState` data class, sealed `UiIntent`, sealed `UiEffect`), a
  ViewModel extending `MviViewModel<S, I, E>`, and a `*Screen.kt`. Intents are reduced one at a time
  from a channel (`onIntent` → `reduce`). State changes go through `setState { copy(…) }`, and
  one-shot events through `sendEffect`, which the UI collects with the lifecycle-gated `CollectEffects`.
- ViewModels subscribe to long-lived flows in `init` (`launchIn(viewModelScope)`) instead of querying
  on demand. Multi-select is an id-based `SelectionState`, pruned on every new list emission.
  Share/favourite/delete on a selection go through the shared `MediaSelectionActions` collaborator,
  not a base class.
- For snackbars, the ViewModel emits a `ShowMessage` effect and the screen forwards it to the app-wide
  `SnackbarDispatcher` (`koinInject()`).
- User-facing strings are inline Kotlin literals. `strings.xml` only holds `app_name`.

### Shell, chrome, and ViewModel scoping

- `AppRoot` owns the **only** `Scaffold`, plus the settings `ModalNavigationDrawer` and the
  `SnackbarHost`, all inside `MediaPermissionGate`. Screens never create their own Scaffold. Tabs
  contribute bars through `ScreenChrome` (`topBar`, `navigation`, `fab`, `immersive`), chosen by
  `AppRouteKey.chrome()` in `common/main/ScreenChrome.kt`. While a selection is active, a tab's chrome
  swaps both bars for selection bars.
- The `navigation` slot renders as a bottom bar or a leading rail, depending on `rememberChromeLayout()`
  (rail when width ≥ 600dp, or in landscape with height < 480dp).
- **ViewModel scope depends on the kind of screen:**
  - Tabs (Gallery, Albums, Search, Secrets) and the settings drawer use `appViewModel()`, which is
    Activity-scoped. Tab switches replace the whole back stack, so entry-scoped ViewModels would lose
    scroll position and selection. The `*Chrome.kt` bars are also rendered by the shell, outside
    `NavDisplay`, and must resolve the same instance as the screen.
  - Pushed screens (`AlbumDetail`, `Viewer`, `SecretViewer`) use `koinViewModel { parametersOf(…) }`,
    scoped to their nav entry by `rememberViewModelStoreNavEntryDecorator`. The shell can't reach
    those ViewModels, so these screens get a `ScreenChrome` with no bars and draw their own top bar.
    Both viewers also set `immersive = true`.
- **Navigation 3:** the back stack is a `SnapshotStateList<AppRouteKey>` saved as JSON across process
  death. Keys must be `@Serializable` and carry identity (ids, `AlbumRef`, `MediaSource`), never
  materialised lists. `switchTabTo` replaces the stack, `navigateTo` pushes, and back from a
  non-Gallery tab returns to Gallery.
- **Adding a screen:** add an `AppRouteKey` subtype, an `entry<…>` in `AppNavigation.kt`, and a branch
  in `AppRouteKey.chrome()`, then register the ViewModel in `presentationModule`. A new tab also has
  to be added to both `AppRouteKey.topLevel` and the `TabSpec` list in `common/ui/component/AppTabs.kt`,
  which are separate lists. (`BottomKey` is unused.)

### Secrets vault (`core/vault`, `features/secrets`)

An encrypted, passphrase-only vault. Key chain: passphrase (Argon2id) or 24-word recovery phrase →
master key → per-item Tink streaming keysets, all inside `vault.bin`, whose outer layer is a
container key wrapped by an Android Keystore key. Security depends on these invariants:

- **Plaintext never touches disk.** Import streams from the content resolver into Tink's encrypting
  stream. `VaultImageFetcher` decodes in memory and returns an `ImageFetchResult`, never a
  `SourceFetchResult`, because Coil may copy sources to a temp file. Video plays through
  `VaultDataSource`; don't add a Media3 cache.
- **Removing anything rotates.** Delete, restore, undoing a move, and changing the passphrase all go
  through `VaultStore.write(rotate = true)`: new Keystore key and container key, atomic rewrite, then
  the old key is destroyed. That rotation is what makes deletion permanent, because flash storage
  doesn't reliably erase overwritten files. Adding items doesn't rotate.
- **Commit before touching originals.** `VaultRepository.add` commits the entries, marked pending,
  before the system delete dialog runs. `MoveToSecretsActions.resolve` then confirms the move or
  undoes it. Pending entries whose originals are gone are settled on the next unlock.
- **Vault files live only in `noBackupFilesDir/vault/`**, never in shared storage or backed-up
  directories. Nothing that can decrypt the vault is persisted: the master key exists only in
  `VaultSession`, which is wiped on lock. The vault locks on `ON_STOP` (`AppRoot`), deferred while an
  operation runs.
- Vault items reuse `MediaItem` with `galva-vault://` URIs (`VaultUri`). They must never reach
  share or MediaStore delete. Vault screens call `SecureWindow()` (`FLAG_SECURE`).
- `vault.bin` records are protobuf with explicit `@ProtoNumber`s. Only add fields; an existing vault
  that no longer parses is lost.
- JVM tests in `app/src/test/.../core/vault/` use `FakeKeyRing` and `FakeKdf`, because the Keystore
  and Argon2's native library aren't available on the JVM.

### Dependency injection

Koin, using a hand-written DSL in `common/di/AppModules.kt`. Every new ViewModel, use case, repository,
or data source has to be registered there by hand, and a missing binding only shows up at runtime.
Data sources, DAOs, and repositories are `single`; use cases are `factory`. Data-layer classes take the
one injected `CoroutineDispatcher` (bound to `Dispatchers.IO`) through their constructor instead of
using `Dispatchers.IO` directly.

### Platform integration

- `MediaPermissionGate` re-checks access on every resume. `MediaAccess.PARTIAL` (Android 14 "Select
  photos") is a normal state, not an error. The per-API permission list lives only in
  `MediaPermissions`. `MainAppViewModel` subscribes to `MediaStoreObserver` only once access exists,
  and it triggers a sync when access changes.
- Coil 3's singleton `ImageLoader` is configured in `GalvaApp`. `MediaStoreThumbnailFetcher` serves
  grid-sized requests from `ContentResolver.loadThumbnail`, larger (viewer) requests fall through to
  Coil's default fetcher, and `VideoFrameDecoder` handles video frames. Video playback uses Media3
  ExoPlayer (`features/viewer/presentation/VideoPage.kt`).
- Settings persist in DataStore Preferences. Enums are stored by `name`, never by ordinal, and grid
  columns are clamped to `AppSettings.COLUMN_RANGE` (2–6).
- The visual design follows Ente Photos: pill tab bar, `EaseOutExpo`/200ms nav motion, selection bars
  replacing the chrome, settings in a drawer. Comments call out deliberate departures. Use
  `GalvaDimens` for spacing and `LocalGalvaPalette` for the nav fills that Material color roles can't
  express.

## Conventions

KDoc and comments explain *why*: platform quirks, data-safety trade-offs, rejected alternatives. Keep
that style, and treat the invariants they document as load-bearing.

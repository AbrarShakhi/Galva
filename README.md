# Galva

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A gallery app for Android. Galva indexes the photos and videos already on your device, keeps that
index current in the background, and serves every screen from it — so browsing a large library stays
fast whether you're scrolling a day-grouped timeline, opening an album, or searching.

Galva is local-only. Nothing is uploaded, nothing is moved on disk, and no account is required.

## Features

- **Timeline** — all media grouped by capture date, newest first. Tap a day header to select the
  whole day.
- **Albums** — device folders, a Favourites album, and albums you create in the app. App-created
  albums are references, so adding a photo never copies or moves the file.
- **Search** — free-text over file and folder names, with filters for images, videos and favourites.
- **Viewer** — full-screen, edge-to-edge, swipe between items; pinch-to-zoom for photos and Media3
  playback for video.
- **Multi-select** — share, favourite, delete or add to an album in bulk from any grid.
- **Safe deletes** — deletion goes through the system consent dialog and the local index is only
  pruned once the user confirms.
- **Adaptive layout** — tab navigation moves to a leading rail on wide or short windows, and grid
  density scales with the window so cell size stays constant across rotation and split-screen.
- **Live updates** — a `ContentObserver` on the media collections triggers an incremental re-sync, so
  a photo taken in another app appears without a manual refresh.
- **Partial access** — Android 14's "Select photos" grant is supported as a first-class state, not a
  failure.
- **Settings** — system/light/dark theme, grid density (2–6 columns), album layout and album sort.

## Requirements

- Android Studio (or a JDK 17 toolchain and the Android SDK)
- Android 10 (API 29) or newer on the device
- `local.properties` with `sdk.dir` pointing at your Android SDK

## Getting started

```bash
git clone <repository-url>
cd Galva
./gradlew :app:installDebug
```

On first launch Galva asks for media access. The app can read only what you grant: full access shows
everything, "Select photos" shows only the items you picked.

## Development

```bash
./gradlew :app:assembleDebug             # build a debug APK
./gradlew :app:testDebugUnitTest         # JVM unit tests
./gradlew :app:connectedDebugAndroidTest # instrumented tests (device or emulator required)
./gradlew :app:lintDebug                 # Android Lint
```

Run a single unit test:

```bash
./gradlew :app:testDebugUnitTest --tests "com.abrarshakhi.galva.ExampleUnitTest.addition_isCorrect"
```

Dependencies are declared in the version catalog at `gradle/libs.versions.toml`. Room schemas are
exported to `app/schemas/` and are checked in — a schema change ships with a hand-written migration
and the regenerated JSON.

## Architecture

Galva is a single Gradle module built around a unidirectional data flow. MediaStore is read in one
place and mirrored into Room; every screen reads from that mirror.

```
MediaStore ──► MediaSyncManager ──► Room ──► Repository ──► UseCase ──► ViewModel ──► Compose
   ▲                  ▲                                                     │
   └── ContentObserver┘                                            Intent ◄─┘
```

**Source layout**

| Package     | Contents                                                                        |
| ----------- | ------------------------------------------------------------------------------- |
| `common/`   | App shell, shared Scaffold, MVI base classes, navigation, shared UI, theme       |
| `core/`     | Domain models, repositories and data sources: `media`, `settings`, `permission`, `share` |
| `features/` | Presentation only — `gallery`, `albums`, `search`, `settings`, `viewer`          |

**Sync.** `MediaSyncManager` reconciles the Room index with MediaStore by fingerprint diff: ids and
modification times are read for the whole collection, and only rows that are new or actually changed
are re-read in full. Passes are serialised, so launch, resume and a burst of observer notifications
collapse into one run. A read that comes back empty, or that would drop a large share of the index,
is treated as a bad read and skipped rather than applied as deletions — favourites and album
membership cascade off the media table, and those are the only records the user authored.

**Presentation.** Each screen is an `MviViewModel<State, Intent, Effect>`: state is a `StateFlow` the
UI re-reads on recomposition, intents are reduced one at a time, and effects are one-shot and
lifecycle-gated. The contract for each feature lives in its `*Contract.kt`.

**Navigation.** Navigation 3, with a serializable sealed `AppRouteKey` back stack that survives
process death. Tab switches replace the stack rather than push onto it, and routes carry identity
rather than data — the viewer receives a media *source* plus a starting id and re-resolves the list
itself, so a large library is never serialised into a navigation key.

**Chrome.** One `Scaffold` is owned by the app shell. Screens contribute their bars through
`ScreenChrome` instead of nesting Scaffolds, which keeps the navigation bar from re-animating on
every destination change and lets a selection swap both bars at once.

## Tech stack

Kotlin · Jetpack Compose · Material 3 · Navigation 3 · Koin · Room · DataStore · Coil 3 · Media3
ExoPlayer · Coroutines & Flow · KSP

## Credits

* <a href="https://www.flaticon.com/free-icons/letter-g" title="letter g icons">Letter g icons created by Maniprasanth - Flaticon</a>

## License

Released under the [MIT License](LICENSE). The app icon is covered by the attribution above, not by
this licence.

# Sizes

An Android app that scans clothing/shoe labels with the camera, remembers what
you own and how each item actually fit, and answers "I want to buy Adidas
shoes, what size do I wear?" from that history. It backs up its local database
to a hidden, app-only folder in your Google Drive -- the same mechanism
WhatsApp uses for chat backups.

## How it works

1. **Scan** -- point the camera at a garment/shoe label, capture a photo.
   On-device OCR (ML Kit) reads the text; a regex-based parser makes a best
   guess at the brand and size, but you always review/correct it before
   saving -- labels are too inconsistent to trust blindly.
2. **Rate the fit** -- when you save an item you also record how it fit
   (too small / snug / true to size / loose / too big). This is the signal
   the recommender learns from; nothing here is guessed from a body scan or
   photo, only from what you tell it.
3. **Ask** -- on the "Sizes" tab, pick a category and type a brand. The app:
   - returns your own logged size directly if you already own something
     from that brand+category, or
   - for shoes, converts from another brand's item you rated as fitting
     well, using a standard EU/US/UK/foot-length conversion table, or
   - for clothing, suggests the closest brand's size as a starting point,
     with a plain-language caveat that clothing sizing isn't standardized
     across brands the way shoe sizing roughly is.
   - if there's nothing to go on yet, it says so and asks you to log a
     reference item first, rather than inventing a number.
4. **Back up** -- Settings lets you back up (or restore) the local SQLite
   database to your Google Drive `appDataFolder`: a per-app hidden storage
   area that doesn't show up in the normal Drive UI and is deleted
   automatically if you uninstall the app. A daily automatic backup can be
   toggled on, which runs as a WorkManager job on unmetered networks.

## Tech stack

| Layer | Choice | Why |
|---|---|---|
| Language | Kotlin | Standard for Android; Kotlin Multiplatform (KMP) is the documented path if iOS support is wanted later without a rewrite. |
| UI | Jetpack Compose + Material 3 | Current recommended Android UI toolkit. |
| Local DB | Room (SQLite) | Typed queries/migrations over raw SQLite, still just a `.db` file underneath -- makes the Drive backup a plain file copy. |
| DI | None (hand-written `AppContainer` in `SizesApplication`) | The app is a handful of screens; a DI framework (Hilt) would add annotation-processor version coupling for no real benefit at this size. |
| Camera | CameraX | Current recommended Android camera API, handles lifecycle/orientation for you. |
| OCR | ML Kit Text Recognition (on-device) | Free, offline, no data leaves the device just to read a label. |
| Background work | WorkManager | Standard for the daily backup job. |
| Drive backup | Google Play Services Identity **Authorization API** + hand-rolled Drive v3 REST calls (OkHttp + kotlinx.serialization) | Only requests the narrow `drive.appdata` scope (this app's own hidden folder, nothing else in your Drive). Deliberately skips the heavier `google-api-client`/`google-api-services-drive` Java libraries and full "Sign in with Google" identity flow -- we don't need to know who you are, only that you've granted this one scope. |

### A build detail worth knowing about

This project was scaffolded in **August 2026** against **AGP 9.2.0**, which
made Kotlin support *built into* the Android Gradle Plugin itself and
deprecated the old `org.jetbrains.kotlin.android` + `kapt` combo. `kapt` is
flatly incompatible with AGP's built-in Kotlin mode, so Room's annotation
processor runs via **KSP** instead. KSP's latest published release at the
time only tracked Kotlin up to **2.3.10** (KSP2 now version-locks 1:1 to the
Kotlin release it supports), so Kotlin is pinned to `2.3.10` in
`gradle/libs.versions.toml` rather than a newer point release, specifically
so KSP has a matching build. If a newer Kotlin release has a matching KSP
build by the time you read this, Android Studio's "Upgrade Assistant" /
version catalog inspection will flag it -- safe to accept.

All other library versions (Compose BOM, Room, CameraX, etc.) were pinned to
the latest stable release available at scaffold time; Android Studio will
suggest newer ones over time via the same mechanism.

## Project structure

```
app/src/main/java/com/sizesapp/
├── data/
│   ├── db/            Room entities, DAO, database (ClosetItem, AppDatabase)
│   ├── repository/     ClosetRepository -- the only thing UI/ViewModels touch
│   ├── sizing/          SizeRecommender + static shoe-size/brand-fit tables
│   └── backup/          GoogleAuthManager (Drive OAuth), DriveBackupManager
│                         (REST calls), BackupWorker (daily WorkManager job)
├── ocr/                 OcrTextRecognizer (ML Kit wrapper), LabelParser (regex heuristics)
├── ui/
│   ├── home/             Closet list
│   ├── scan/             CameraX capture + OCR screen
│   ├── itemedit/         Add/edit form (used both after a scan and for manual entry)
│   ├── recommend/        "What size do I wear?" screen
│   ├── settings/         Drive backup controls
│   ├── navigation/       Nav graph + route definitions
│   └── common/           Hand-written ViewModel factory (AppContainer wiring)
└── SizesApplication.kt   Holds the AppContainer (database, repository, recommender, backup manager)
```

## Getting it running

### 1. Open in Android Studio

Open the `sizesapp` folder directly (not a sub-folder) -- it's a normal
Gradle project, Android Studio will sync automatically. The Gradle wrapper
is already committed, so you don't need a system-wide Gradle install.

You'll need an AVD (virtual device) to run it, since none was created
automatically: **Device Manager -> Create device** in Android Studio, any
phone profile, an API 33+ system image.

### 2. Google Drive backup setup (optional, only needed for the backup feature)

The scan/recommend/local-storage features work with zero setup. Drive
backup needs one manual step in [Google Cloud Console](https://console.cloud.google.com/):

1. Create a project (or reuse one) and enable the **Google Drive API**
   (APIs & Services -> Library).
2. Configure the **OAuth consent screen** (External is fine; add your own
   Google account as a test user while the app is unpublished).
3. Create an **OAuth client ID** of type **Android** (Credentials -> Create
   Credentials -> OAuth client ID):
   - Package name: `com.sizesapp`
   - SHA-1 certificate fingerprint: get it from Android Studio
     (**Gradle panel -> app -> Tasks -> android -> signingReport**), or from
     a terminal:
     ```
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```

No client ID string needs to go into the code or `local.properties` --
Play Services matches the request to your registered client by package name
+ signing certificate automatically. If you later make a signed release
build, you'll need a second Android OAuth client registered with the
release signing certificate's SHA-1.

### 3. Build from the command line (optional)

```
./gradlew assembleDebug
```

The resulting APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Current scope / known limitations

This is a working v1 scaffold, not a finished product:

- **Label parsing is a heuristic, not magic.** OCR + regex gets you a
  starting guess for brand/size; you confirm or fix it every time. Labels
  vary too much (multiple sizes for multiple regions on one label, stylized
  brand logos OCR can't read, etc.) for this to be fully automatic.
- **The shoe size conversion table is a standard published chart**, not
  brand-specific lasts -- real-world half-size differences between brands
  are called out via `BrandFitNotes` (a small hardcoded list of a dozen
  brands) rather than modeled precisely.
- **Clothing (non-shoe) size recommendations are intentionally rougher** --
  there's no reliable universal conversion for tops/bottoms across brands,
  so it suggests your closest known-good reference size with a caveat
  rather than pretending to convert it.
- **Drive backup keeps one snapshot**, not version history -- each backup
  overwrites the previous one.
- Test coverage so far is focused on `LabelParser`: fast JVM unit tests in
  `app/src/test` (including regression cases from real scanned labels), plus
  an instrumented test in `app/src/androidTest` that runs actual on-device
  ML Kit OCR against bundled sample label photos. `SizeRecommender` and the
  Room layer don't have tests yet.

## License

GPLv3 -- see [LICENSE](LICENSE). Contributions and forks are welcome, but
any distributed derivative work must also be open-sourced under GPLv3.

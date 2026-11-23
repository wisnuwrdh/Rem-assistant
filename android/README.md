# EcosystemKu — Android Native (Kotlin + Jetpack Compose)

Project Android Studio siap-buka hasil migrasi dari `assistant/` + `api/` sesuai rencana arsitektur 3-tab (`Asisten` | `Apps` | `Setelan`).

## Cara Membuka & Build di Android Studio
1. Buka folder `android/` ini melalui **Android Studio** (*File → Open → pilih folder `android/`*).
2. Tunggu Gradle sync selesai (Android Studio akan mengunduh dependencies Compose, Room, OkHttp, Media3, dan WebKit).
3. Pastikan `BuildConfig.API_BASE_URL` di `app/build.gradle.kts` mengarah ke server backend `/api/chat` dan `/api/tts` Anda yang aktif.
4. Jalankan **Run ▶** pada emulator atau perangkat Android (minSdk 26 / Android 8.0+).

## Struktur Fitur Saat Ini
- **Tab 1 — `Asisten` (Logika Penuh Aktif)**:
  - Panggung Live2D Rem (`assets/live2d/characters/rem/`) berjalan offline via `WebViewAssetLoader` + `Live2DStageController` interface.
    - `WebView` memakai latar solid `#0F0E0C` + aura CSS di `stage.html` serta `preserveDrawingBuffer: true` dan `antialias: false` agar rendering WebGL konsisten di Compose `AndroidView` (tanpa `.alpha()` layer).
    - `StageJsBridge` men-dispatch callback ke `MainLooper`, `playRemMotion` memakai `MotionPriority.FORCE` (`3`) agar motion loop bisa berganti, dan override wajah/tangan/lip-sync diterapkan di event `beforeModelUpdate`.
    - Render dijeda (`StageAPI.pause/resume`) saat tab Apps/Setelan atau sub-app menutupi panggung, dan saat aplikasi di background.
    - Jika konteks WebGL hilang (`webgl_context_lost`) atau renderer WebView dimatikan sistem (`renderer_gone`), panggung dipulihkan otomatis; jika gagal, muncul pesan error + tombol **Muat Ulang** (membuat ulang WebView via `stageGeneration`).
  - Chat streaming SSE (`POST /api/chat` lewat `OkHttp` + `okhttp-sse`), parsing tag `[EXPR:...]` dan `[JA]...[/JA]`.
  - Suara TTS Jepang (`POST /api/tts`) & klip suara lokal (`audio/*.mp3`) lewat `Media3 ExoPlayer` (thread-safe di `Dispatchers.Main.immediate`, dengan fallback `MediaPlayer` untuk aset lokal) dan `TeeAudioProcessor` RMS lip-sync real-time.
  - Penyimpanan riwayat chat lokal persisten di perangkat memakai **Room** (`rem_assistant.db`).
  - Tombol Mikrofon & 2 tombol kaca kanan atas masih **UI-only** sesuai rencana tahap 1.
- **Tab 2 — `Apps` & Tab 3 — `Setelan` & Sub-App Overlay**:
  - Overlay sub-app (`Kronik`, `LinkKu`, `CuciMoney+`, `IdeKu`, `Daily OS`) dibungkus `Box` solid dengan pointer blocker di `MainActivity.kt` agar sentuhan di area atas/kosong tidak tembus ke dashboard `AppsHubScreen`.
- **Diagnostik crash**: `EcosystemKuApp` memasang `UncaughtExceptionHandler` yang membuka `CrashActivity` (proses `:crash`) berisi stacktrace + tombol **Salin Error**.

## Catatan Aset Font (Opsional)
Saat ini tipografi memakai `FontFamily.Serif` dan `FontFamily.SansSerif` bawaan Android agar langsung kompilasi tanpa perlu file eksternal. Jika ingin memakai font asli mockup (`DM Serif Display`, `DM Sans`, `Lora`), taruh file `.ttf`-nya di `app/src/main/res/font/` lalu perbarui `ui/theme/Type.kt`.

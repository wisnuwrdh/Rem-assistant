# Asisten Rem — Android Native (Kotlin + Jetpack Compose)

Project Android Studio siap-buka hasil migrasi dari `assistant/` + `api/` sesuai rencana arsitektur 3-tab (`Asisten` | `Apps` | `Setelan`), dengan ikon launcher kustom (`ic_launcher_rem_photo.jpg`) dan nama aplikasi `Asisten Rem`.

## Cara Membuka & Build di Android Studio
1. Buka folder `android/` ini melalui **Android Studio** (*File → Open → pilih folder `android/`*).
2. Tunggu Gradle sync selesai (Android Studio akan mengunduh dependencies Compose, Room, OkHttp, Media3, dan WebKit).
3. Jalankan **Run ▶** pada emulator atau perangkat Android (minSdk 26 / Android 8.0+). Seluruh panggilan AI & TTS kini berjalan **langsung dari perangkat** ke endpoint LLM/TTS yang diatur di tab **Setelan** (tidak bergantung pada server proxy eksternal).

## Signing & Update Tanpa Uninstall
- Build `debug` dan `release` memakai keystore tetap `app/rem-assistant.jks` (alias `rem`, di-commit ke repo; cukup untuk app pribadi) lewat `signingConfigs.fixed` di `app/build.gradle.kts`. Signature APK jadi sama di setiap build lokal maupun GitHub Actions, sehingga APK baru bisa menimpa versi lama tanpa uninstall.
- Syarat update in-place: `applicationId` (`com.ecosystemku.assistant`) tetap, signature sama, dan `versionCode` tidak lebih rendah dari yang terpasang (naikkan `versionCode` tiap rilis).
- Peralihan dari APK lama (signature acak runner CI) ke APK ini tetap butuh uninstall **sekali**. Backup dulu via Setelan → *Export ZIP Sekarang*, lalu restore setelah install.

## Struktur Fitur Saat Ini
- **Tab 1 — `Asisten` (Logika Penuh Aktif)**:
  - Panggung Live2D Rem (`assets/live2d/characters/rem/`) berjalan offline via `WebViewAssetLoader` + `Live2DStageController` interface.
    - `WebView` memakai latar solid `#0F0E0C` + aura CSS di `stage.html` serta `preserveDrawingBuffer: true`, `antialias: true`, dan resolusi WebGL hingga `3x` DPR agar rendering tetap tajam dan konsisten di Compose `AndroidView` (tanpa `.alpha()` layer). Mendukung wallpaper gambar kustom di layer `#stage-bg` + `#stage-bg-dim` di belakang kanvas WebGL via `WebViewAssetLoader.InternalStoragePathHandler` dan `StageAPI.setBackground`.
    - `StageJsBridge` men-dispatch callback ke `MainLooper`, `playRemMotion` memakai `MotionPriority.FORCE` (`3`) agar motion loop bisa berganti, override wajah/tangan/lip-sync diterapkan di `beforeModelUpdate`, memakai satu ticker terpadu (`autoUpdate: false` + `model.update`), `paramIndexCache`, serta `ResizeObserver` ter-coalesce rAF.
    - Render dijeda (`StageAPI.pause/resume`) saat tab Apps/Setelan atau sub-app menutupi panggung, dan saat aplikasi di background.
    - Jika konteks WebGL hilang (`webgl_context_lost`) atau renderer WebView dimatikan sistem (`renderer_gone`), panggung dipulihkan otomatis; jika gagal, muncul pesan error + tombol **Muat Ulang** (membuat ulang WebView via `stageGeneration`).
  - Chat streaming SSE langsung ke endpoint OpenAI-compatible (`/chat/completions` lewat `OkHttp` + `okhttp-sse` & [`DirectLlmClient`](app/src/main/java/com/ecosystemku/assistant/data/network/DirectLlmClient.kt)), parsing tag `[EXPR:...]` dan `[JA]...[/JA]`, dengan `inputText`, `streamingText`, dan `idleBubbleText` terisolasi ke `StateFlow` terpisah agar tidak memicu rekomposisi global.
  - Suara TTS Jepang langsung ke Fish Audio / endpoint TTS kustom & klip suara lokal (`audio/*.mp3`) lewat `Media3 ExoPlayer` (thread-safe di `Dispatchers.Main.immediate`, fallback `MediaPlayer`, serta throttle lip-sync ~30 Hz) dan `TeeAudioProcessor` RMS lip-sync real-time.
  - Penyimpanan riwayat chat lokal persisten di perangkat memakai **Room** (`rem_assistant.db`, observasi dibatasi `observeRecent(150)`).
  - Tombol Mikrofon memakai Android Speech-to-Text (`RecognizerIntent.ACTION_RECOGNIZE_SPEECH`, `id-ID`), tombol kaca kanan atas mendukung bisukan suara Rem (Mute TTS) serta menu `...` (Bisu/Suara TTS, Voice Klip, & Bersihkan Chat), dan avatar chat memakai foto profil Rem (`R.drawable.rem_chat_avatar`).
- **Tab 2 — `Apps` (5 Modul Aktif) & Sub-App Overlay**:
  - Menampilkan 5 modul utama (`Kronik`, `LinkKu`, `CuciMoney+`, `IdeKu`, `Daily OS`) beserta sapaan waktu dinamis & nama profil pengguna.
  - Overlay sub-app dibungkus `Box` solid dengan pointer blocker di `MainActivity.kt` agar sentuhan di area atas/kosong tidak tembus ke dashboard `AppsHubScreen`.
  - Helper model memakai `DateTimeFormatter` statis, `ThreadLocal<NumberFormat>`, `LruCache` untuk parsing JSON berulang, dan `LazyColumn` ber-`key` stabil.
- **Tab 3 — `Setelan` (Fase 1 & Fase 2 Aktif Penuh)**:
  - **Profil, Memori AI, & Otak Digital (Knowledge Graph)**: Edit nama profil (tersinkron ke sapaan dashboard & prompt Rem), toggle auto-learn memori percakapan + **Pertanyaan Refleksi Ringan** (`kgMicroQuestionsEnabled`) + **Check-In Refleksi Mingguan** terjadwal (notifikasi *deep-link* hari/jam kustom ke **Sesi Kenali Aku**), edit/tambah/hapus bagian pada **Ringkasan Memori** naratif (dilindungi flag `manuallyEdited`), form tambah cepat fakta manual, dialog penuh **Kelola Fakta & Rangkuman** (`MemoryManagerDialog`), serta akses ke **Peta Otak Digital (`KnowledgeGraphScreen`)** — kanvas graf interaktif pan/zoom/tap untuk simpul/relasi Room v15 (`kg_nodes`, `kg_edges`), pelacakan evolusi `[Historis]` (garis putus-putus & penurunan bobot otomatis/manual saat kebiasaan berubah), sinkronisasi sinyal perilaku 0-token (`BehavioralSignalSync`), **Sesi Kenali Aku**, dan ekspor **Digital Twin JSON (`rem-digital-twin-kg-v1`)**.
  - **AI & Suara (Full Custom)**: Konfigurasi penuh tanpa default server untuk LLM OpenAI-compatible (`Base URL`, `Model`, `API Key`) serta TTS Fish Audio (`API Key`, `Voice ID`, `Model`, `Base URL`). Key disimpan terenkripsi AES-256-GCM di Android Keystore (`ApiKeyStore`), seluruh klien AI/TTS memanggil penyedia secara langsung dari perangkat, dan tombol **Test Koneksi** menguji endpoint LLM langsung.
  - **Backup & Restore Ekosistem (ZIP v3 & Cloud Google Drive)**: Ekspor & impor satu file `.zip` lintas seluruh modul (`kronik/`, `linkku/`, `cucimoney/`, `ideku/`, `dailyos/`, `assistant/`, dan `knowledge_graph.json`) melalui `EcosystemBackupRepository` lengkap dengan dekripsi AES-GCM PIN LinkKu, serta **Cloud Backup Google Drive (`appDataFolder`)** melalui `GoogleDriveBackupClient` + `CloudBackupWorker` (`WorkManager` Harian/Mingguan) dengan enkripsi opsional passphrase AES-256-GCM (`REMC1` di `CloudBackupCrypto`).
  - **Notifikasi Daily OS & Tampilan**: Penjadwalan pengingat lokal `AlarmManager` (`DailyOsNotificationHelper` + `DailyOsAlarmReceiver`) untuk Habit & To-Do dengan pemilih jam, izin `POST_NOTIFICATIONS`, tombol **Test Notifikasi**, pemilihan tema reaktif (`Sistem` / `Terang` / `Gelap`), pengaturan **Wallpaper Asisten Rem** (pilih gambar, preview, slider redupkan background, & reset), serta dialog konfirmasi reset per-aplikasi maupun hapus semua data.
- **Diagnostik crash**: `EcosystemKuApp` memasang `UncaughtExceptionHandler` yang membuka `CrashActivity` (proses `:crash`) berisi stacktrace + tombol **Salin Error**.

## Catatan Aset Font (Opsional)
Saat ini tipografi memakai `FontFamily.Serif` dan `FontFamily.SansSerif` bawaan Android agar langsung kompilasi tanpa perlu file eksternal. Jika ingin memakai font asli mockup (`DM Serif Display`, `DM Sans`, `Lora`), taruh file `.ttf`-nya di `app/src/main/res/font/` lalu perbarui `ui/theme/Type.kt`.

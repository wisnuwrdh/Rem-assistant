# EcosystemKu — Rem Assistant

Asisten virtual **Rem** berbasis Live2D yang terintegrasi dengan ekosistem aplikasi produktivitas pribadi (**Daily OS**, **CuciMoney+**, **IdeKu**, **Kronik**, dan **LinkKu**). Tersedia dalam dua klien: **Android Native (Kotlin + Jetpack Compose + Room)** dan **Web Preview (Express + Vanilla JS)**.

---

## Fitur Utama

### 1. Asisten Virtual Rem
* **Live2D Stage Interaktif:** Render karakter Rem (Cubism 4) dengan kedip mata, pernapasan, pelacakan kursor/sentuhan, ekspresi wajah & gerakan tangan (`[EXPR:...]`), serta lip-sync saat berbicara.
* **Streaming Chat (SSE):** Percakapan real-time dengan LLM OpenAI-compatible melalui header konfigurasi dari klien, lengkap dengan pembersihan otomatis format Markdown (`**`, `*`, `` ` ``) dan *em dash* (`—`) menjadi teks biasa yang rapi.
* **Konteks Waktu & Jeda Pesan:** Rem menerima tanggal/waktu lokal perangkat serta jeda waktu sejak pesan terakhir Tuan, sehingga sapaan dan respons menyesuaikan konteks waktu nyata.
* **Kontrol Pesan per Bubble:** Mendukung **Salin Pesan**, **Ulangi Jawaban** (*regenerate* balasan Rem), dan **Hapus Pesan** (tekan lama pada bubble di Android atau tombol aksi cepat di Web Preview).
* **Sistem Sesi & Log Obrolan:** Percakapan dikelompokkan per sesi obrolan dengan judul otomatis, tombol **Obrolan Baru**, panel **Log Obrolan** (pindah, ubah judul, atau hapus sesi), cadangan ZIP lintas-sesi, serta *smart auto-rollover* (memulai sesi baru otomatis saat berganti hari atau jeda >= 5 jam jika sesi sebelumnya sudah berisi obrolan).
* **Rolling Context Compaction:** Saat obrolan di dalam satu sesi melebihi 10 pesan, pesan-pesan lama dipadatkan secara inkremental di latar belakang menjadi ringkasan konteks sesi (`rollingSummary`) agar obrolan panjang tetap nyambung tanpa membebani jendela konteks.
* **On-Demand Search Lintas Ekosistem (2-Tahap):** Ketika Tuan menanyakan arsip lama (transaksi bulan tertentu, jurnal lama, ide, bookmark publik, Daily OS, maupun riwayat obrolan sesi lalu), Rem dapat memicu pencarian lokal lewat `[SEARCH:scope|kata_kunci]` lalu menyusun jawaban akurat dari hasil query lokal.
* **Suara Jepang (TTS) & Voice Klip Kontekstual:** Jawaban bahasa Indonesia disertai terjemahan Jepang tersembunyi (`[JA]...[/JA]`) yang diputar via Fish Audio (`/api/tts`), serta klip suara bawaan Rem untuk interaksi tap/idle dan **voice klip reaksi obrolan otomatis** (dipetakan dari realisasi wishlist, aksi modul, pencarian arsip, sapaan/pamit, hingga ekspresi `[EXPR:...]`) sebagai fallback ketika TTS dimatikan, belum diatur, atau gagal. Di Android, mute **Suara TTS** dan **Voice Klip** terpisah.
* **Pilihan Interaktif (Quick Replies):** Kartu pilihan di dalam obrolan yang mendukung:
  * **Pilihan Tunggal (`single`):** Chip pilihan cepat (mis. pilih buku *Personal* atau *Bisnis* di CuciMoney+) — tap langsung mengirim balasan.
  * **Pilihan Ganda (`multi`):** Daftar centang multi-opsi dengan tombol **Kirim**.
* **Memori Personal & Ringkasan Memori:** Ekstraksi otomatis fakta pengguna (`identitas`, `preferensi`, `kebiasaan`, `tujuan`, `catatan`), pembaruan/penghapusan memori langsung dari obrolan (`[MEMORY:SET/DEL]` & `deleteKeys`), serta halaman **Ringkasan Memori** naratif per bagian yang diperbarui otomatis (`/api/memory/extract`).

### 2. Ekosistem Sub-App Terintegrasi
Rem dapat membaca konteks dan mengeksekusi aksi langsung dari obrolan ke modul berikut:
* **Daily OS:** Pelacak habit harian, to-do dengan prioritas (`urgent`, `soon`, `normal`), AI Coach briefing, analisis memori 30 hari, smart to-do parser, dan rekomendasi tugas (`/api/dailyos`).
* **CuciMoney+:** Pencatatan transaksi pemasukan/pengeluaran multi-buku & multi-akun, budget bulanan, serta tabungan wishlist. Rem membaca daftar kategori resmi (beserta pilihan chip kategori), breakdown pemasukan/pengeluaran per kategori bulan ini, sisa budget bulanan, serta progres tabungan wishlist.
* **IdeKu:** Pengelola ide & wishlist dengan mode **Perkaya** dan **Breakdown Prompt** berbasis AI (`/api/ideku`).
* **Kronik:** Buku jurnal pribadi 3-fase dengan deteksi mood AI (`Fokus`, `Lelah`, `Terinspirasi`, `Berat`, `Stabil`, `Semangat`, `Kabur`) via `/api/kronik/mood`.
* **LinkKu:** Pengelola bookmark dengan auto-fetch judul/thumbnail URL & YouTube (`/api/linkku/meta`) serta Mode Privat ber-PIN (enkripsi AES-GCM).

---

## Struktur Proyek

```text
.
├── server.js                 # Express server: API routes, Live2D assets, & Web Preview
├── package.json              # Script & dependensi Node.js (Express, @google/genai, dll.)
├── api/                      # Handler endpoint backend
│   ├── _llm.js               # Resolver konfigurasi LLM dari header X-LLM-*
│   ├── chat.js               # POST /api/chat (SSE streaming + pemisah [JA]...[/JA])
│   ├── memory.js             # POST /api/memory/extract (ekstraksi memori, profile, & compact_session)
│   ├── tts.js                # POST /api/tts (Fish Audio MP3)
│   ├── dailyos.js            # POST /api/dailyos (coach, memory, parse_todo, recommend_todo)
│   ├── ideku.js              # POST /api/ideku (perkaya, breakdown)
│   ├── kronikMood.js         # POST /api/kronik/mood
│   └── linkkuMeta.js         # POST /api/linkku/meta
├── preview/                  # Web Preview non-PWA (UI 3-tab: Asisten, Apps, Setelan)
│   ├── index.html
│   ├── app.js
│   ├── cucimoney.js / .css
│   ├── ideku.js / .css
│   └── dailyos.js / .css
├── android/                  # Aplikasi Android Native (Kotlin Compose, minSdk 26, targetSdk 35)
│   ├── app/src/main/java/com/ecosystemku/assistant/
│   │   ├── MainActivity.kt
│   │   ├── data/local/       # Room DB (AssistantDatabase v13: chat_sessions + rollingSummary, DAO, Entities, ApiKeyStore)
│   │   ├── data/network/     # ChatSseClient, SseTagParser, & client API modul
│   │   └── ui/               # AsistenScreen, AsistenViewModel, AppsHubScreen, SetelanScreen, & sub-apps
│   └── app/src/main/assets/live2d/characters/rem/
│       └── character.json    # Konfigurasi model Live2D, ekspresi, & system prompt dasar
└── .github/workflows/
    └── android-apk.yml       # CI build Android Debug APK (JDK 17, SDK 35, Gradle 8.9)
```

---

## Protokol Tag Chat Rem

Respons LLM dapat menyertakan tag tersembunyi (disembunyikan otomatis saat streaming dan dibersihkan dari teks bubble):

| Tag | Fungsi |
| --- | --- |
| `[SEARCH:scope\|kata_kunci]` | Pencarian on-demand 2-tahap ke arsip lokal (`kronik`, `linkku`, `cuci`, `ideku`, `dailyos`, `chat`, `all`) |
| `[CHOICES:single\|Opsi 1\|Opsi 2]` | Menampilkan chip pilihan tunggal di bawah pesan Rem |
| `[CHOICES:multi\|Opsi 1\|Opsi 2]` | Menampilkan daftar pilihan ganda + tombol **Kirim** |
| `[MEMORY:SET\|kategori\|fact_key\|nilai]` | Menyimpan atau memperbarui fakta memori personal dari obrolan |
| `[MEMORY:DEL\|fact_key_atau_kata_kunci]` | Menghapus/melupakan fakta memori personal dari obrolan |
| `[EXPR:nama]` | Mengubah ekspresi & motion Live2D (`happy`, `smile`, `shame`, `sad`, `cry`, `angry`, `surprise`, `serious`, `proud`, `doubt`, `puzzle`, `upset`, `appeal`, `navi`, `none`) |
| `[OPEN:app]` | Membuka modul (`kronik`, `linkku`, `cucimoney`, `ideku`, `dailyos`) |
| `[KRONIK:ADD\|judul\|isi\|mood]` | Menambah entri jurnal ke Kronik |
| `[LINKKU:ADD\|url\|judul\|catatan\|kategori\|tag1,tag2]` | Menyimpan bookmark publik ke LinkKu |
| `[CUCI:ADD\|out/in\|nominal\|judul\|kategori\|akun]` | Mencatat transaksi ke CuciMoney+ |
| `[CUCI:SAVE\|nama_wishlist\|nominal\|akun\|buku]` | Menyetor tabungan dari akun ke pot tabungan wishlist |
| `[CUCI:REALIZE\|nama_wishlist\|akun\|nominal_beli\|buku]` | Mencairkan/menandai target barang wishlist sudah kebeli (`Selesai`) |
| `[IDEKU:ADD\|Idea\|judul\|kategori\|prioritas\|catatan\|jalur]` | Menambah ide baru ke IdeKu |
| `[IDEKU:ADD\|Wishlist\|nama\|harga\|Belanja\|prioritas\|catatan]` | Menambah wishlist ke IdeKu & CuciMoney+ |
| `[DAILYOS:HABIT\|nama\|kategori\|ikon]` | Menambah habit baru di Daily OS |
| `[DAILYOS:TODO\|judul\|priority\|YYYY-MM-DD]` | Menambah tugas to-do di Daily OS |
| `[DAILYOS:CHECK\|kata_kunci]` | Menandai habit atau to-do selesai hari ini |
| `[JA]...[/JA]` | Teks bahasa Jepang khusus untuk sintesis suara TTS |

---

## Konfigurasi & API Header

Backend tidak menyimpan kredensial default. Klien mengirim konfigurasi melalui menu **Setelan** atau header HTTP berikut:
* **LLM:** `X-LLM-Base` (Base URL OpenAI-compatible), `X-LLM-Model`, `X-LLM-Key`
* **TTS:** `X-TTS-Key` (wajib untuk suara), opsional `X-TTS-Base`, `X-TTS-Model`, `X-TTS-Voice`

---

## Menjalankan Secara Lokal

### Backend & Web Preview
```bash
npm install
npm start
```
Server berjalan di `http://0.0.0.0:3000` (atau sesuai `PORT` environment variable).
* Web Preview: `http://localhost:3000/` atau `http://localhost:3000/preview/`
* Aset Live2D: `/live2d/...`

Pengecekan sintaks & lint:
```bash
npm run build
npm run lint
```

### Build Android APK
Persyaratan sesuai workflow CI ([`.github/workflows/android-apk.yml`](.github/workflows/android-apk.yml)):
* **JDK 17**
* **Android SDK Platform 35** & Build-Tools `35.0.0`
* **Gradle 8.9**

```bash
cd android
gradle :app:assembleDebug
```
*(Opsional: gunakan Gradle property atau env `API_BASE_URL` untuk menentukan URL server backend).*

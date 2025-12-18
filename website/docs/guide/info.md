# WebSu Plus Documentation

Selamat datang di dokumentasi resmi **WebSu Plus (WebPlus)**. WebSu Plus adalah platform eksperimental untuk menjalankan modul berbasis WebUI dengan fleksibilitas akses sistem di Android.

## 🏗️ Getting Started

Pilih metode otorisasi yang sesuai dengan status perangkat Anda. WebSu Plus dirancang untuk tetap bertenaga baik dalam kondisi **Root** maupun **Non-Root**.

### 1. Akses Root (Recommended)
Untuk pengguna yang menginginkan performa penuh dan kendali sistem total.

::: info PROSES OTORISASI
Cukup buka aplikasi WebSu Plus, lalu berikan izin saat jendela pop-up Superuser muncul dari salah satu manager berikut:
* **Magisk**
* **KernelSU**
* **APatch (Sukisu)**
:::

### 2. Akses Shizuku (Non-Root)
Solusi cerdas bagi Anda yang ingin menggunakan modul sistem tanpa melakukan modifikasi permanen (Root) pada perangkat.

**Langkah-langkah Aktivasi:**
1. **Setup Shizuku:** Pastikan layanan Shizuku sudah berstatus *Running*. Jika belum, ikuti [Panduan Setup Shizuku](https://shizuku.rikka.app/guide/setup/).
2. **Otorisasi Aplikasi:**
   * Buka aplikasi **Shizuku Manager**.
   * Pilih menu **Authorized Applications**.
   * Cari **WebSu Plus** dan aktifkan *toggle* ke posisi **ON**.
3. **Mulai:** Kembali ke WebSu Plus dan aplikasi siap digunakan.

## 🛠️ Fitur Inti (Core Features)

### 💻 Terminal Executor
WebSu Plus dilengkapi dengan executor universal yang mampu mengenali environment perangkat secara cerdas.
* **Auto-detect:** Otomatis menyesuaikan perintah antara mode `root` dan `shell`.
* **Universal Command:** Menjalankan script kustom langsung di dalam aplikasi.

### 🌐 WebUI Module
Mendukung modul yang dibangun dengan teknologi web (HTML/JS/CSS).
* **JS API Support:** Integrasi API JavaScript yang memungkinkan modul web berinteraksi dengan sistem Android.
* **UI Fleksibel:** Tampilan modul yang sepenuhnya bisa dikustomisasi oleh pengembang.

## ❓ FAQ (Pertanyaan Sering Diajukan)

| Pertanyaan | Jawaban |
| :--- | :--- |
| **Apakah aman?** | Sangat aman. WebSu Plus adalah proyek open-source yang transparan. |
| **Bisa jalan di Android versi berapa?** | WebSu Plus mendukung sebagian besar versi Android modern yang didukung oleh Shizuku/Root Manager terkait. |
| **Kenapa modul tidak jalan?** | Pastikan izin (Root/Shizuku) sudah diberikan dan modul tersebut kompatibel dengan versi WebSu Plus Anda. |

## 🤝 Kontribusi & Dukungan
WebSu Plus adalah proyek berkelanjutan. Kami sangat terbuka bagi para developer yang ingin bereksperimen atau melaporkan bug demi kemajuan ekosistem Android yang lebih terbuka.


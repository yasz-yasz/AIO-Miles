# AIO Miles — All-in-One Frequent Flyer Miles Calculator & Mix Advisor

Aplikasi Android berbasis **Kotlin + Jetpack Compose (Material 3)** untuk menghitung kebutuhan penukaran miles sekaligus memberikan rekomendasi **Mix Miles lintas aliansi maupun aliansi sama** bagi pemegang **GarudaMiles (SkyTeam)**, **KrisFlyer (Star Alliance)**, dan **Asia Miles (oneworld)**.

## Fitur Utama

1. **Kalkulator Redeem 5 Kelas Kabin**
   - Mendukung **Economy**, **Premium Economy**, **Business**, **First Class**, dan **Suites (Singapore Airlines A380)**.
   - Mendukung mode **Sekali Jalan (1-Way)** & **Pulang-Pergi (PP)** serta toggle **KrisFlyer Saver vs Advantage**.
2. **Saran Mix Miles Beda Aliansi & Aliansi Sama**
   - Membandingkan penerbangan **Direct (Langsung)**, **1-Stop Sama Aliansi (1 PNR vs Split 2 PNR)**, serta **Mix Miles Beda Aliansi** (misalnya Leg 1 GarudaMiles + Leg 2 Asia Miles atau Leg 1 KrisFlyer + Leg 2 Asia Miles).
   - Otomatis memilih hub transit efisien tanpa *detour* ekstrem.
3. **Database 12,094 Rute Penerbangan Global**
   - Mencakup **68 maskapai** (18 SkyTeam, 26 Star Alliance termasuk ITA Airways & catatan transisi Asiana Des 2026, 16 oneworld termasuk Fiji Airways, Oman Air & Hawaiian Airlines, serta 8 mitra bilateral non-aliansi seperti Etihad, LATAM, Scoot, Virgin Australia, JetBlue, Juneyao, Gulf Air, Bangkok Airways).
   - Mencakup **331 bandara internasional & domestik** dengan kalkulasi jarak *great-circle* (Haversine).
   - Dilengkapi filter **Indonesia**, **Fifth Freedom**, **A380 Suites**, pencarian instan, dan direktori maskapai.
4. **Dompet Saldo Miles & Referensi Award Chart**
   - Simpan saldo GarudaMiles, KrisFlyer, dan Asia Miles secara lokal; kalkulator langsung menandai kombinasi rute/mix yang **saldonya sudah cukup untuk di-redeem**.

## Cara Build APK Otomatis via GitHub Actions

1. Buat repository baru di GitHub (misalnya `AIO-Miles`).
2. Ekstrak seluruh isi file ZIP ini dan upload/push ke branch `main`.
3. Buka tab **Actions** di GitHub → workflow **Build AIO Miles Android APK** akan berjalan otomatis menggunakan JDK 17 + Gradle 8.9.
4. Setelah selesai, unduh APK dari bagian **Artifacts** → `AIO-Miles-debug-apk`.

## Cara Buka di Android Studio

1. Ekstrak ZIP dan buka folder `AIO-Miles` di Android Studio (Ladybug / terbaru).
2. Tunggu Gradle sync selesai (Android SDK 35, JDK 17).
3. Jalankan ke emulator atau perangkat Android (minimal Android 7.0 / API 24).

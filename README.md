# bidmart-booking

Modul **Pemesanan dan Notifikasi** untuk platform lelang BidMart.

Modul ini menangani proses pasca-lelang: pembuatan pesanan otomatis, pelacakan pengiriman, konfirmasi penerimaan, sengketa pembelian, serta pengiriman notifikasi real-time ke pengguna.

---

## Cara Menjalankan

### Prasyarat

- Java 21
- Docker (untuk PostgreSQL)

### 1. Jalankan Database

```bash
docker-compose up -d
```

Database berjalan di port `5433` dengan nama `bidmart_booking`.

### 2. Jalankan Aplikasi

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.8/libexec/openjdk.jdk/Contents/Home \
  ./gradlew bootRun
```

Aplikasi berjalan di `http://localhost:8085`.

### 3. Jalankan Tests

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.8/libexec/openjdk.jdk/Contents/Home \
  ./gradlew check jacocoTestCoverageVerification
```

---

## API Overview

Dokumentasi interaktif tersedia di `http://localhost:8085/swagger-ui.html` setelah aplikasi berjalan.

### Booking Endpoints

| Method | Path | Role | Deskripsi |
|--------|------|------|-----------|
| `GET` | `/api/bookings/me` | BUYER | Daftar booking sebagai pembeli |
| `GET` | `/api/bookings/selling` | SELLER | Daftar booking sebagai penjual |
| `GET` | `/api/bookings/{id}` | BUYER | Detail booking |
| `PATCH` | `/api/bookings/{id}/shipment` | SELLER | Update status pengiriman |
| `PATCH` | `/api/bookings/{id}/confirm-delivery` | BUYER | Konfirmasi penerimaan barang |
| `POST` | `/api/bookings/{id}/dispute` | BUYER | Ajukan sengketa setelah pengiriman |
| `GET` | `/api/bookings/{id}/dispute` | BUYER/SELLER | Lihat detail sengketa |

### Notification Endpoints

| Method | Path | Deskripsi |
|--------|------|-----------|
| `GET` | `/api/notifications/me` | Daftar notifikasi masuk |
| `GET` | `/api/notifications/stream` | SSE stream notifikasi real-time |
| `GET` | `/api/notifications/preferences/me` | Preferensi notifikasi |
| `PATCH` | `/api/notifications/preferences/me` | Update preferensi notifikasi |
| `PATCH` | `/api/notifications/{id}/read` | Tandai notifikasi sudah dibaca |

### Dev Endpoints (hanya aktif saat `DEV_ENDPOINTS_ENABLED=true`)

| Method | Path | Deskripsi |
|--------|------|-----------|
| `POST` | `/api/dev/events/winner-determined` | Simulasikan event pemenang lelang |

---

## Siklus Hidup Booking

```
CREATED → PAID → SHIPPED → DELIVERED → COMPLETED
                                    ↘ DISPUTED → COMPLETED
```

- **CREATED**: Booking dibuat otomatis saat event `WinnerDetermined` diterima.
- **PAID**: Dana pemenang berhasil dikonversi (event `BalanceConverted`).
- **SHIPPED**: Penjual memperbarui status pengiriman ke `SHIPPED`.
- **DELIVERED**: Penjual memperbarui status pengiriman ke `DELIVERED`.
- **DISPUTED**: Pembeli mengajukan sengketa setelah barang diterima.
- **COMPLETED**: Pembeli mengkonfirmasi penerimaan, atau sengketa diselesaikan.

---

## Event yang Dikonsumsi

| Event | Aksi |
|-------|------|
| `WinnerDetermined` | Buat booking + notifikasi menang/kalah |
| `AuctionClosed` | Dicatat (idempoten) |
| `BidPlaced` | Notifikasi penjual & bidder sebelumnya |
| `BalanceConverted` | Notifikasi konfirmasi pembayaran, transisi PAID |
| `BalanceReleased` | Notifikasi saldo dilepas |

Semua event dikonsumsi secara idempoten menggunakan tabel `processed_events`. Event yang gagal setelah 3 kali retry disimpan ke `dead_letter_events`.

---

## Notifikasi Real-time

Notifikasi dikirim via **Server-Sent Events (SSE)**:

```
GET /api/notifications/stream
Header: X-User-Id: <user-id>
```

Tipe event SSE: `connected`, `notification`, `auction-update`, `booking-status-changed`.

---

## Environment Variables

| Variable | Default | Deskripsi |
|----------|---------|-----------|
| `PORT` | `8085` | Port aplikasi |
| `DB_URL` | `jdbc:postgresql://localhost:5433/bidmart_booking` | URL database |
| `DB_USERNAME` | `bidmart_booking` | Username database |
| `DB_PASSWORD` | `b15-adpro` | Password database |
| `APP_SEED_DUMMY_DATA` | `true` | Isi data dummy saat startup |
| `DEV_ENDPOINTS_ENABLED` | `true` | Aktifkan endpoint dev simulasi |

Set `DEV_ENDPOINTS_ENABLED=false` di production untuk menonaktifkan endpoint simulasi.

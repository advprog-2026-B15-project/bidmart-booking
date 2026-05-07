# Kontrak Realtime dan Reliability Bidmart Booking v1

Status: Proposed
Owner: `bidmart-booking`
Milestone: 75%

## 1) Tujuan

Dokumen ini mendefinisikan scope milestone 75 untuk realtime delivery dan reliability event pada modul booking dan notification.

Implementasi tetap memakai istilah `booking` yang sudah ada di codebase, dengan mapping ke lifecycle order yang disebutkan pada milestone.

## 2) Strategi Realtime

Realtime delivery akan menggunakan Server-Sent Events (SSE).

SSE dipilih karena flow realtime yang dibutuhkan adalah pengiriman event satu arah dari server ke client. SSE tidak membutuhkan broker tambahan dan lebih sederhana dibanding WebSocket untuk kebutuhan stream satu arah.

### Endpoint

`GET /api/notifications/stream`

Header wajib:

- `X-User-Id`

### Tipe Event Realtime

| Event Type | Trigger | Penerima |
|---|---|---|
| `NOTIFICATION_CREATED` | notification baru disimpan | pemilik notification |
| `BOOKING_STATUS_CHANGED` | status booking berubah | buyer dan/atau seller |
| `AUCTION_UPDATE` | event terkait auction diproses | user terkait |

### Bentuk Payload SSE

```json
{
  "type": "NOTIFICATION_CREATED",
  "userId": "usr-1001",
  "data": {
    "notificationId": 10,
    "title": "Payment confirmed",
    "message": "Your payment has been confirmed"
  },
  "occurredAt": "2026-05-06T10:00:00Z"
}
```

## 3) Reliability Event Consumer

Semua event handler harus idempotent.

Event handler yang didukung:

- `WinnerDetermined`
- `AuctionClosed`
- `BidPlaced`
- `BalanceConverted`
- `BalanceReleased`

### Aturan Idempotency

Setiap event yang masuk harus memiliki `eventId` yang unik.

Sebelum memproses event, modul akan mengecek apakah `eventId` sudah ada di tabel `processed_events`.

Jika event sudah pernah diproses, consumer akan mengabaikannya dan tidak membuat booking, notification, audit log, atau realtime event duplikat.

## 4) Strategi Retry

Pemrosesan event akan menggunakan retry wrapper.

Aturan:

- maksimal retry: `3`
- retry berlaku untuk kegagalan processing yang bersifat sementara
- jika semua retry gagal, event akan disimpan ke `dead_letter_events`
- data failure harus menyimpan metadata event asli dan pesan error

## 5) Dead-Letter Handling

Event yang gagal diproses akan disimpan di `dead_letter_events`.

Field minimum:

- `event_id`
- `event_type`
- `payload`
- `error_message`
- `retry_count`
- `failed_at`

Implementasi awal milestone ini hanya menyimpan dead-letter event. Fitur replay atau admin recovery bisa ditambahkan pada fase berikutnya.

## 6) Audit Log Status Booking

Setiap perubahan status booking harus membuat entry audit log.

Perubahan status yang diaudit:

- `CREATED -> PAID`
- `PAID -> SHIPPED`
- `SHIPPED -> DELIVERED`
- `DELIVERED -> COMPLETED`

Field minimum:

- `booking_id`
- `from_status`
- `to_status`
- `changed_by_user_id`
- `changed_by_role`
- `reason`
- `created_at`

## 7) Indexing Database

Index harus mendukung query utama:

- lookup processed event berdasarkan `event_id`
- list dead-letter event berdasarkan `failed_at`
- list audit log berdasarkan `booking_id`
- list notification berdasarkan `user_id` dan `created_at`
- stream unread notification berdasarkan `user_id`

Index yang sudah ada harus digunakan kembali jika masih relevan, supaya tidak membuat index duplikat.

## 8) Scope End-to-End Test

Milestone ini harus memiliki end-to-end test untuk memvalidasi flow lengkap:

1. menerima event pemenang auction
2. memproses event secara idempotent
3. membuat booking
4. membuat notification
5. mengirim realtime notification event
6. mengubah status booking
7. menulis audit log perubahan status booking

## 9) CI Coverage Gate

GitHub Actions harus menjalankan:

```bash
./gradlew test jacocoTestCoverageVerification
```

Trigger:

- push ke `main`
- push ke `staging`
- pull request ke `main`
- pull request ke `staging`

## 10) Batasan yang Diketahui

- SSE menangani realtime delivery dari backend ke frontend.
- SSE tidak menggantikan message broker untuk integrasi antar-service.
- Wiring message broker tidak diwajibkan untuk milestone ini kecuali tim memutuskan untuk mulai integrasi RabbitMQ/Kafka.
- Email delivery berada di luar scope realtime milestone ini.

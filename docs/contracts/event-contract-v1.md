# Bidmart Booking Event Contract v1

Owner: `bidmart-booking`  
Producer: `bidmart-auction`  
Consumer: `bidmart-booking`  
Last Updated: 2026-03-04

## 1) Tujuan
Dokumen ini mendefinisikan format event dari modul Auction ke modul Booking/Notification agar integrasi stabil dan tidak ambigu.

## 2) Event Envelope (Wajib untuk semua event)

| Field | Type | Required | Example | Notes |
|---|---|---|---|---|
| `eventId` | `string` (UUID) | Yes | `evt-4f4d5f8a-6a2d-4c26-9f78-8f93c31f47bc` | Unik per event, dipakai untuk idempotency |
| `eventType` | `string` | Yes | `WinnerDetermined` | Enum: `WinnerDetermined`, `AuctionClosed` |
| `eventVersion` | `integer` | Yes | `1` | Versi kontrak event |
| `occurredAt` | `string` (ISO-8601 UTC) | Yes | `2026-03-04T09:20:00Z` | Waktu event terjadi |
| `source` | `string` | Yes | `bidmart-auction` | Service pengirim |
| `payload` | `object` | Yes | `{...}` | Isi bisnis event |

Contoh envelope:
```json
{
  "eventId": "evt-4f4d5f8a-6a2d-4c26-9f78-8f93c31f47bc",
  "eventType": "WinnerDetermined",
  "eventVersion": 1,
  "occurredAt": "2026-03-04T09:20:00Z",
  "source": "bidmart-auction",
  "payload": {}
}
```

## 3) Event: `WinnerDetermined` (Wajib)

### Payload Schema

| Field | Type | Required | Example | Notes |
|---|---|---|---|---|
| `auctionId` | `string` | Yes | `auc-1001` | ID lelang |
| `listingId` | `string` | Yes | `lst-5001` | ID listing/catalog |
| `sellerUserId` | `string` | Yes | `usr-2001` | User penjual |
| `winnerUserId` | `string` | Yes | `usr-3001` | User pemenang |
| `finalPrice` | `integer` (`int64`) | Yes | `1750000` | Harga akhir dalam rupiah (tanpa desimal) |
| `currency` | `string` | Yes | `IDR` | Default `IDR` |
| `itemName` | `string` | Yes | `Mechanical Keyboard` | Nama item |
| `quantity` | `integer` (`int32`) | Yes | `1` | Minimum 1 |
| `loserUserIds` | `array<string>` | No | `["usr-3002","usr-3003"]` | Untuk notif kalah |

Contoh event:
```json
{
  "eventId": "evt-001",
  "eventType": "WinnerDetermined",
  "eventVersion": 1,
  "occurredAt": "2026-03-04T09:20:00Z",
  "source": "bidmart-auction",
  "payload": {
    "auctionId": "auc-1001",
    "listingId": "lst-5001",
    "sellerUserId": "usr-2001",
    "winnerUserId": "usr-3001",
    "finalPrice": 1750000,
    "currency": "IDR",
    "itemName": "Mechanical Keyboard",
    "quantity": 1,
    "loserUserIds": ["usr-3002", "usr-3003"]
  }
}
```

## 5) Processing Rules (Consumer: bidmart-booking)
1. Idempotency wajib berdasarkan `eventId`.
2. `WinnerDetermined` memicu pembuatan booking otomatis.
3. `WinnerDetermined` memicu notifikasi:
    - `WIN` untuk `winnerUserId`
    - `LOSE` untuk semua `loserUserIds` (jika tersedia)
4. Event dengan field wajib tidak lengkap harus di-`reject` dan di-log.
5. Timestamp wajib UTC ISO-8601.

## 6) Versioning Rules
1. Penambahan field non-wajib = backward-compatible.
2. Rename/hapus field wajib = breaking change, naikkan `eventVersion`.
3. Consumer harus mengabaikan field yang tidak dikenal.

## 7) Open Points

1. Broker/topic final yang dipakai (mis. `auction.events`).
2. `loserUserIds` tersedia dari producer atau tidak.
3. Jaminan publish event setelah transaksi auction commit.


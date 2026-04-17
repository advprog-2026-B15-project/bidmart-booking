# Bidmart Booking Event Contract v2

Owner: `bidmart-booking`  
Primary Producers: `bidmart-auction`, `bidmart-wallet`  
Consumer: `bidmart-booking`  
Last Updated: 2026-04-14

## 1) Tujuan
Dokumen ini mendefinisikan kontrak event untuk milestone 50, yaitu:

- lifecycle booking yang lebih lengkap
- notifikasi tambahan dari auction dan wallet
- aturan compatibility dari kontrak event v1

## 2) Event Envelope (Wajib untuk semua event)

| Field | Type | Required | Example | Notes |
|---|---|---|---|---|
| `eventId` | `string` (UUID) | Yes | `evt-4f4d5f8a-6a2d-4c26-9f78-8f93c31f47bc` | Unik per event, dipakai untuk idempotency |
| `eventType` | `string` | Yes | `WinnerDetermined` | Enum event type |
| `eventVersion` | `integer` | Yes | `2` | Versi kontrak event |
| `occurredAt` | `string` (ISO-8601 UTC) | Yes | `2026-04-14T09:20:00Z` | Waktu event terjadi |
| `source` | `string` | Yes | `bidmart-auction` | Service pengirim |
| `payload` | `object` | Yes | `{...}` | Isi bisnis event |

Contoh envelope:
```json
{
  "eventId": "evt-4f4d5f8a-6a2d-4c26-9f78-8f93c31f47bc",
  "eventType": "BidPlaced",
  "eventVersion": 2,
  "occurredAt": "2026-04-14T09:20:00Z",
  "source": "bidmart-auction",
  "payload": {}
}
```

## 3) Booking Lifecycle Contract

Lifecycle booking milestone 50:

`CREATED -> PAID -> SHIPPED -> DELIVERED -> COMPLETED`

Aturan transisi:

1. `CREATED -> PAID` hanya terjadi setelah pembayaran dikonfirmasi.
2. `PAID -> SHIPPED` terjadi saat seller mengirim barang.
3. `SHIPPED -> DELIVERED` terjadi saat status shipment menunjukkan barang sudah sampai.
4. `DELIVERED -> COMPLETED` terjadi saat buyer mengkonfirmasi penerimaan.
5. Transisi di luar urutan di atas harus ditolak.

## 4) Event: `WinnerDetermined`

### Payload Schema

| Field | Type | Required | Example | Notes |
|---|---|---|---|---|
| `auctionId` | `string` | Yes | `auc-1001` | ID lelang |
| `listingId` | `string` | Yes | `lst-5001` | ID listing/catalog |
| `sellerUserId` | `string` | Yes | `usr-2001` | User penjual |
| `winnerUserId` | `string` | Yes | `usr-3001` | User pemenang |
| `finalPrice` | `integer` (`int64`) | Yes | `1750000` | Harga akhir dalam rupiah |
| `currency` | `string` | Yes | `IDR` | Default `IDR` |
| `itemName` | `string` | Yes | `Mechanical Keyboard` | Nama item |
| `quantity` | `integer` (`int32`) | Yes | `1` | Minimum 1 |
| `loserUserIds` | `array<string>` | No | `["usr-3002","usr-3003"]` | Untuk notif kalah |

## 5) Event: `AuctionClosed`

### Payload Schema

| Field | Type | Required | Example | Notes |
|---|---|---|---|---|
| `auctionId` | `string` | Yes | `auc-1001` | ID lelang |
| `listingId` | `string` | Yes | `lst-5001` | ID listing |
| `closedAt` | `string` (ISO-8601 UTC) | Yes | `2026-04-14T09:20:00Z` | Waktu lelang ditutup |
| `hasWinner` | `boolean` | Yes | `true` | Menandakan ada pemenang atau tidak |
| `winnerUserId` | `string` | No | `usr-3001` | Wajib jika `hasWinner=true` |

## 6) Event: `BidPlaced`

### Payload Schema

| Field | Type | Required | Example | Notes |
|---|---|---|---|---|
| `auctionId` | `string` | Yes | `auc-1001` | ID lelang |
| `listingId` | `string` | Yes | `lst-5001` | ID listing |
| `sellerUserId` | `string` | Yes | `usr-2001` | User penjual |
| `bidderUserId` | `string` | Yes | `usr-3004` | User yang memasang bid baru |
| `previousHighestBidderUserId` | `string` | No | `usr-3001` | Untuk notif outbid |
| `bidAmount` | `integer` (`int64`) | Yes | `1800000` | Nilai bid baru |
| `currency` | `string` | Yes | `IDR` | Default `IDR` |
| `itemName` | `string` | No | `Mechanical Keyboard` | Untuk message notifikasi |

Processing intent:

1. Seller menerima new bid alert.
2. `previousHighestBidderUserId` menerima outbid notification jika tersedia.

## 7) Event: `BalanceConverted`

### Payload Schema

| Field | Type | Required | Example | Notes |
|---|---|---|---|---|
| `bookingId` | `string` | No | `bkg-public-1001` | Public booking identifier jika tersedia |
| `auctionId` | `string` | No | `auc-1001` | Dipakai fallback correlation |
| `userId` | `string` | Yes | `usr-3001` | User yang melakukan pembayaran |
| `amount` | `integer` (`int64`) | Yes | `1750000` | Nilai pembayaran |
| `currency` | `string` | Yes | `IDR` | Default `IDR` |
| `conversionReference` | `string` | No | `bal-conv-1001` | External/internal reference |

Processing intent:

1. Buat payment confirmation notification.
2. Booking dapat ditransisikan ke `PAID` jika korelasi booking ditemukan.

## 8) Event: `BalanceReleased`

### Payload Schema

| Field | Type | Required | Example | Notes |
|---|---|---|---|---|
| `bookingId` | `string` | No | `bkg-public-1001` | Public booking identifier jika tersedia |
| `auctionId` | `string` | No | `auc-1001` | Dipakai fallback correlation |
| `userId` | `string` | Yes | `usr-2001` | User penerima release balance |
| `amount` | `integer` (`int64`) | Yes | `1750000` | Nilai release |
| `currency` | `string` | Yes | `IDR` | Default `IDR` |
| `releaseReference` | `string` | No | `bal-rel-1001` | External/internal reference |

Processing intent:

1. Buat notification bahwa balance telah dirilis.

## 9) Processing Rules (Consumer: bidmart-booking)

1. Idempotency wajib berdasarkan `eventId`.
2. `WinnerDetermined` membuat booking otomatis dengan status awal `CREATED`.
3. `AuctionClosed` minimal divalidasi dan dicatat.
4. `BidPlaced` membuat notifikasi bid baru dan outbid sesuai payload.
5. `BalanceConverted` membuat payment confirmation notification dan dapat memicu transisi `CREATED -> PAID`.
6. `BalanceReleased` membuat release confirmation notification.
7. Event dengan field wajib tidak lengkap harus di-reject dan di-log.

## 10) Versioning Rules

1. Penambahan field non-wajib bersifat backward-compatible.
2. Rename atau hapus field wajib adalah breaking change dan harus menaikkan `eventVersion`.
3. Consumer harus mengabaikan field yang tidak dikenal.

## 11) Open Points

1. Apakah `bookingId` publik akan dipublish oleh wallet atau hanya `auctionId`.
2. Apakah `BidPlaced` harus memuat daftar watcher/follower auction untuk future real-time notification.
3. Broker dan topic final untuk auction events dan wallet events.

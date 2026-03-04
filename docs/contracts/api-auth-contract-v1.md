# Bidmart Booking API/Auth Contract v1

Owner: `bidmart-booking`  
Last Updated: 2026-03-04

## 1) Tujuan
Dokumen ini mendefinisikan kontrak API dan autentikasi untuk MVP Booking dan Notification.

## 2) Auth Strategy (MVP)

Untuk milestone ini gunakan header sementara:

- `X-User-Id: <user-id>`

Contoh:
```http
X-User-Id: usr-3001
```

Catatan:

- Ini sementara untuk MVP.
- Target berikutnya migrasi ke JWT (`Authorization: Bearer <token>`).
- Semua endpoint wajib auth.

## 3) Common Rules

1. Semua response format JSON.
2. Ownership check wajib: user hanya boleh akses data miliknya.
3. Timestamp response pakai ISO-8601 UTC.
4. Notifikasi diurutkan terbaru ke lama.

## 4) Error Response Standard

Contoh:

```json
{
  "code": "NOT_FOUND",
  "message": "Booking not found"
}
```

Kode error yang dipakai:

- `UNAUTHORIZED` (401)
- `FORBIDDEN` (403)
- `NOT_FOUND` (404)
- `BAD_REQUEST` (400)
- `INTERNAL_ERROR` (500)

## 5) Endpoint Contract

### 5.1 GET `/api/bookings/me`

Deskripsi: Ambil daftar booking milik user login.

Headers:

- `X-User-Id` (required)

Query params (opsional):

- `page` (default `0`)
- `size` (default `10`)

Success `200`:
```json
[
  {
    "id": "bkg-1001",
    "auctionId": "auc-1001",
    "listingId": "lst-5001",
    "buyerUserId": "usr-3001",
    "sellerUserId": "usr-2001",
    "status": "CREATED",
    "totalAmount": 1750000,
    "currency": "IDR",
    "createdAt": "2026-03-04T10:00:00Z"
  }
]
```
### 5.2 GET /api/bookings/{id}
Deskripsi: Ambil detail booking berdasarkan ID.

Headers:
- `X-User-Id` (required)

Path params:
- `id` (booking id)

Success `200`:
```json
{
  "id": "bkg-1001",
  "auctionId": "auc-1001",
  "listingId": "lst-5001",
  "buyerUserId": "usr-3001",
  "sellerUserId": "usr-2001",
  "status": "CREATED",
  "totalAmount": 1750000,
  "currency": "IDR",
  "items": [
    {
      "itemName": "Mechanical Keyboard",
      "quantity": 1,
      "unitPrice": 1750000
    }
  ],
  "shipment": {
    "status": "PENDING"
  },
  "createdAt": "2026-03-04T10:00:00Z"
}
```

Error:
- `403` jika bukan pemilik booking
- `404` jika booking tidak ditemukan
### 5.3 GET /api/notifications/me
Deskripsi: Ambil daftar notifikasi user login.

Headers:
- `X-User-Id` (required)

Query params (opsional):

- `page` (default 0)
- `size` (default 20)
- `unreadOnly` (default false)
Success `200`:
```json
[
  {
    "id": "ntf-9001",
    "type": "WIN",
    "title": "You won the auction",
    "message": "You won auction auc-1001 with final price IDR 1,750,000",
    "isRead": false,
    "createdAt": "2026-03-04T10:05:00Z"
  }
]
```

### 5.4 PATCH /api/notifications/{id}/read
Deskripsi: Tandai notifikasi sebagai sudah dibaca.

Headers:
- `X-User-Id` (required)

Path params:
- `id` (notification id)

Request body:
```json
{
  "read": true
}
```
Success `200`:
```json
{
  "id": "ntf-9001",
  "type": "WIN",
  "title": "You won the auction",
  "message": "You won auction auc-1001 with final price IDR 1,750,000",
  "isRead": true,
  "createdAt": "2026-03-04T10:05:00Z",
  "readAt": "2026-03-04T10:10:00Z"
}
```

Error:
- `403` jika notifikasi bukan milik user
- `404` jika notifikasi tidak ditemukan

## 6) Mapping ke Event Contract
1. Event WinnerDetermined membuat booking + notifikasi WIN.
2. Jika loserUserIds tersedia, buat notifikasi LOSE untuk setiap user kalah.
3. finalPrice event dipetakan ke totalAmount booking.

## 7) Open Points
1. Kapan migrasi auth dari `X-User-Id` ke `JWT`.
2. Final format pagination.
3. Enum final untuk status booking, shipment, dan notification type.
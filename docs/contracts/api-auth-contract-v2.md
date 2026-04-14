# Bidmart Booking API/Auth Contract v2

Owner: `bidmart-booking`  
Last Updated: 2026-04-14

## 1) Tujuan

Dokumen ini mendefinisikan kontrak API dan autentikasi untuk milestone 50:

- lifecycle booking lengkap
- shipment update oleh seller
- delivery confirmation oleh buyer
- notification preference management
- OpenAPI sebagai source of truth dokumentasi endpoint

## 2) Auth Strategy

Untuk tahap sekarang masih gunakan header sementara:

- `X-User-Id: <user-id>`
- `X-User-Role: BUYER | SELLER`

Contoh:
```http
X-User-Id: usr-3001
X-User-Role: BUYER
```

Catatan:

1. Ini tetap bersifat sementara sampai JWT lintas service siap.
2. Ownership check dan role check wajib dilakukan di server.

## 3) Common Rules

1. Semua response format JSON.
2. Timestamp response pakai ISO-8601 UTC.
3. Error response standar tetap `{code, message}`.
4. OpenAPI spec harus mencakup semua endpoint v1 dan v2.

## 4) Booking Lifecycle Rules

1. Status booking yang valid:
   - `CREATED`
   - `PAID`
   - `SHIPPED`
   - `DELIVERED`
   - `COMPLETED`
2. Seller hanya boleh update data shipment.
3. Buyer hanya boleh confirm delivery untuk booking miliknya sendiri.
4. Transisi status di luar lifecycle yang disepakati harus ditolak dengan `400 BAD_REQUEST`.

## 5) Existing Endpoints (Tetap Berlaku)

### 5.1 GET `/api/bookings/me`

Deskripsi: Ambil daftar booking milik buyer yang sedang login.

Headers:

- `X-User-Id` (required)

### 5.2 GET `/api/bookings/{id}`

Deskripsi: Ambil detail booking milik buyer yang sedang login.

Headers:

- `X-User-Id` (required)

### 5.3 GET `/api/notifications/me`

Deskripsi: Ambil daftar notifikasi milik user login.

Headers:

- `X-User-Id` (required)

### 5.4 PATCH `/api/notifications/{id}/read`

Deskripsi: Tandai notifikasi sebagai sudah dibaca.

Headers:

- `X-User-Id` (required)

Request body:
```json
{
  "read": true
}
```

## 6) New Endpoints for Milestone 50

### 6.1 PATCH `/api/bookings/{id}/shipment`

Deskripsi: Seller mengubah status shipment dan, jika tersedia, tracking number.

Headers:

- `X-User-Id` (required)
- `X-User-Role: SELLER` (required)

Request body:
```json
{
  "status": "SHIPPED",
  "trackingNumber": "JNE-123456789",
  "courierName": "JNE"
}
```

Success `200`:
```json
{
  "bookingId": 1,
  "shipmentStatus": "SHIPPED",
  "trackingNumber": "JNE-123456789",
  "courierName": "JNE"
}
```

### 6.2 PATCH `/api/bookings/{id}/confirm-delivery`

Deskripsi: Buyer mengkonfirmasi bahwa barang telah diterima.

Headers:

- `X-User-Id` (required)
- `X-User-Role: BUYER` (required)

Success `200`:
```json
{
  "bookingId": 1,
  "status": "COMPLETED"
}
```

### 6.3 GET `/api/notifications/preferences/me`

Deskripsi: Ambil notification preference user login.

Headers:

- `X-User-Id` (required)

Success `200`:
```json
{
  "userId": "usr-3001",
  "emailEnabled": true,
  "inAppEnabled": true
}
```

### 6.4 PATCH `/api/notifications/preferences/me`

Deskripsi: Ubah notification preference user login.

Headers:

- `X-User-Id` (required)

Request body:
```json
{
  "emailEnabled": false,
  "inAppEnabled": true
}
```

Success `200`:
```json
{
  "userId": "usr-3001",
  "emailEnabled": false,
  "inAppEnabled": true
}
```

## 7) Notification Rules

1. `BidPlaced`:
   - seller menerima new bid alert
   - previous highest bidder menerima outbid notification jika tersedia
2. `BalanceConverted`:
   - buyer menerima payment confirmation notification
3. `BalanceReleased`:
   - seller menerima balance released notification
4. Jika `inAppEnabled=false`, notification in-app tidak disimpan.
5. Jika `emailEnabled=true`, email flow dapat dipanggil oleh notification service atau adapter terkait.

## 8) OpenAPI Requirement

1. Semua endpoint v1 dan v2 harus tersedia di OpenAPI spec.
2. Request body, response body, dan error response harus terdokumentasi.
3. Enum status booking dan shipment harus muncul jelas di schema dokumentasi.

## 9) Open Points

1. Nama header role final akan dipertahankan atau diganti saat JWT siap.
2. Apakah endpoint seller shipment update akan dipisah antara status update dan tracking update.
3. Apakah email delivery benar-benar dikirim di milestone 50 atau masih sebatas preference + placeholder adapter.

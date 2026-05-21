# Kontrak Milestone 100 Bidmart Booking

Status: Final
Owner: `bidmart-booking`
Milestone: 100%

## 1) Tujuan

Dokumen ini mendefinisikan scope milestone 100 untuk modul Pemesanan dan Notifikasi.

Milestone ini melengkapi fitur-fitur berikut di atas milestone 75:
- Fitur sengketa (dispute) untuk pembeli setelah barang diterima
- Endpoint daftar booking untuk penjual
- Keamanan endpoint dev melalui configuration flag
- Dokumentasi lengkap dan coverage test penuh

---

## 2) Fitur Baru

### 2.1 Dispute (Sengketa)

Pembeli dapat mengajukan sengketa setelah barang diterima (`DELIVERED`). Sengketa menyebabkan booking berpindah ke status `DISPUTED`.

#### Siklus hidup booking yang diperbarui

```
CREATED → PAID → SHIPPED → DELIVERED → COMPLETED
                                    ↘ DISPUTED → COMPLETED
```

#### Endpoint

**POST** `/api/bookings/{id}/dispute`

Header wajib:
- `X-User-Id`: ID pembeli
- `X-User-Role`: harus bernilai `BUYER`

Request body:
```json
{
  "reason": "Barang tidak sesuai deskripsi"
}
```

Constraint:
- `reason` wajib diisi, minimum 10 karakter, maksimum 1000 karakter
- Booking harus dalam status `DELIVERED`
- Hanya pembeli dari booking tersebut yang dapat mengajukan sengketa
- Tidak dapat mengajukan sengketa dua kali untuk booking yang sama (`409 Conflict`)

Response `201`:
```json
{
  "id": 1,
  "bookingId": 42,
  "filedByUserId": "usr-123",
  "reason": "Barang tidak sesuai deskripsi",
  "status": "OPEN",
  "resolutionNote": null,
  "resolvedAt": null,
  "createdAt": "2026-05-18T10:00:00Z",
  "updatedAt": "2026-05-18T10:00:00Z"
}
```

---

**GET** `/api/bookings/{id}/dispute`

Header wajib:
- `X-User-Id`: ID pengguna (pembeli atau penjual dari booking tersebut)

Response `200`: sama seperti response POST di atas.

---

### 2.2 Seller Booking List

Penjual dapat melihat daftar booking di mana mereka adalah penjual.

**GET** `/api/bookings/selling`

Header wajib:
- `X-User-Id`: ID penjual

Response `200`: array dari `BookingSummaryResponse`, diurutkan dari terbaru.

---

### 2.3 Keamanan Dev Endpoint

Dev endpoint (`/api/dev/events/*`) hanya aktif saat `app.dev.endpoints.enabled=true` (default `true`).

Di production, set environment variable:
```
DEV_ENDPOINTS_ENABLED=false
```

Ini akan menonaktifkan bean `BookingDevController` sepenuhnya melalui `@ConditionalOnProperty`.

---

## 3) Skema Database Baru

### Tabel `disputes`

```sql
CREATE TABLE disputes (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    filed_by_user_id VARCHAR(100) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    resolution_note VARCHAR(1000),
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_disputes_booking FOREIGN KEY (booking_id)
        REFERENCES bookings (id) ON DELETE CASCADE
);
```

### Kolom baru di `bookings`

```sql
ALTER TABLE bookings ADD COLUMN disputed_at TIMESTAMP WITH TIME ZONE;
```

---

## 4) Authorization

| Endpoint | Siapa yang boleh akses |
|----------|------------------------|
| `GET /api/bookings/me` | Pembeli (buyer) |
| `GET /api/bookings/selling` | Penjual (seller) |
| `GET /api/bookings/{id}` | Pembeli dari booking tersebut |
| `PATCH /api/bookings/{id}/shipment` | Penjual dari booking tersebut (`X-User-Role: SELLER`) |
| `PATCH /api/bookings/{id}/confirm-delivery` | Pembeli dari booking tersebut (`X-User-Role: BUYER`) |
| `POST /api/bookings/{id}/dispute` | Pembeli dari booking tersebut (`X-User-Role: BUYER`) |
| `GET /api/bookings/{id}/dispute` | Pembeli atau penjual dari booking tersebut |

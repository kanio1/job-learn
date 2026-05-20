# API Contracts: Phase 1 Merchant Management

**Date**: 2026-05-18 | **Spec**: [spec.md](../spec.md)

## Authorization

All merchant endpoints require a valid JWT access token with the appropriate authority, except `GET /api/status` which remains public.

| Authority | Allowed Operations |
|---|---|
| `platform:merchants:create` | `POST /api/merchants` |
| `platform:merchants:read` | `GET /api/merchants`, `GET /api/merchants/{id}` |
| `platform:merchants:update-status` | `POST /api/merchants/{id}/activate`, `POST /api/merchants/{id}/suspend` |

Missing authentication → `401 Unauthorized`. Missing required authority → `403 Forbidden`.

---

## POST /api/merchants

Create a new merchant in `DRAFT` status.

**Request**:
```json
{
  "merchantReference": "MERCH-001",
  "displayName": "Acme Payments Inc."
}
```

**Validation**:
- `merchantReference`: required, trimmed, normalized to uppercase, 3-64 chars, `^[A-Z0-9][A-Z0-9-]{1,62}[A-Z0-9]$`, must not already exist (normalized)
- `displayName`: required, trimmed, 2-120 chars

| Status | Body | Condition |
|---|---|---|
| `201` | `{ "merchantId": "uuid", "merchantReference": "MERCH-001", "displayName": "Acme Payments Inc.", "status": "DRAFT", "createdAt": "2026-05-18T...", "updatedAt": "2026-05-18T..." }` | Success |
| `400` | `{ "error": "validation", "message": "Invalid merchant request", "details": {"merchantReference": "must match..." } }` | Invalid field |
| `409` | `{ "error": "duplicate_merchant_reference", "message": "A merchant with reference 'MERCH-001' already exists" }` | Duplicate normalized reference |
| `401` | | Unauthenticated |
| `403` | | Missing `platform:merchants:create` |

---

## GET /api/merchants/{id}

Retrieve a merchant by internal ID.

**Path**: `id` — UUID string.

| Status | Body | Condition |
|---|---|---|
| `200` | `{ "merchantId": "uuid", "merchantReference": "MERCH-001", "displayName": "...", "status": "DRAFT", "createdAt": "...", "updatedAt": "..." }` | Success |
| `400` | `{ "error": "validation", "message": "Malformed merchant ID" }` | Malformed UUID |
| `404` | `{ "error": "not_found", "message": "Merchant not found" }` | Unknown ID |
| `401` | | Unauthenticated |
| `403` | | Missing `platform:merchants:read` |

---

## GET /api/merchants

List merchants, newest-first, up to 50.

**Response** (first-page wrapper):
```json
{
  "merchants": [
    {
      "merchantId": "uuid",
      "merchantReference": "MERCH-002",
      "displayName": "Beta Payments",
      "status": "DRAFT",
      "createdAt": "2026-05-18T17:00:00Z",
      "updatedAt": "2026-05-18T17:00:00Z"
    },
    {
      "merchantId": "uuid",
      "merchantReference": "MERCH-001",
      "displayName": "Acme Payments Inc.",
      "status": "ACTIVE",
      "createdAt": "2026-05-18T16:00:00Z",
      "updatedAt": "2026-05-18T16:30:00Z"
    }
  ]
}
```

**Deterministic tie-breaker**: `createdAt DESC, merchantId ASC`.

| Status | Condition |
|---|---|
| `200` | Success (may return empty `merchants` list) |
| `401` | Unauthenticated |
| `403` | Missing `platform:merchants:read` |

No pagination parameters in Phase 1. No search/filter query parameters. Response always returns up to 50 newest merchants.

---

## POST /api/merchants/{id}/activate

Activate a `DRAFT` merchant: `DRAFT → ACTIVE`.

| Status | Body | Condition |
|---|---|---|
| `200` | Updated merchant object with `status: "ACTIVE"` and new `updatedAt` | Success |
| `400` | `{ "error": "validation", "message": "Malformed merchant ID" }` | Malformed UUID |
| `404` | `{ "error": "not_found", "message": "Merchant not found" }` | Unknown ID |
| `409` | `{ "error": "invalid_transition", "message": "Cannot activate merchant in status 'ACTIVE'" }` | Invalid transition |
| `401` | | Unauthenticated |
| `403` | | Missing `platform:merchants:update-status` |

Valid transitions for activate: only from `DRAFT`.

---

## POST /api/merchants/{id}/suspend

Suspend an `ACTIVE` merchant: `ACTIVE → SUSPENDED`.

| Status | Body | Condition |
|---|---|---|
| `200` | Updated merchant object with `status: "SUSPENDED"` and new `updatedAt` | Success |
| `400` | `{ "error": "validation", "message": "Malformed merchant ID" }` | Malformed UUID |
| `404` | `{ "error": "not_found", "message": "Merchant not found" }` | Unknown ID |
| `409` | `{ "error": "invalid_transition", "message": "Cannot suspend merchant in status 'DRAFT'" }` | Invalid transition |
| `401` | | Unauthenticated |
| `403` | | Missing `platform:merchants:update-status` |

Valid transitions for suspend: only from `ACTIVE`.

---

## GET /api/status (Existing — Unchanged)

Public. No authentication required. Returns `200` with `{"application":"payment-quality-lab","phase":"foundation","status":"UP"}`. Must not expose merchant data.

---

## Error Response Convention

All error responses follow a consistent shape:

```json
{
  "error": "<error_code>",
  "message": "<human-readable description>",
  "details": { }   // optional, for validation errors
}
```

Error codes: `validation`, `duplicate_merchant_reference`, `not_found`, `invalid_transition`.

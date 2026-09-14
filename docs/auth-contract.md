# RideLink Authentication Contract

Owner: Account Service (Nuran). All services must implement token validation exactly as described here.

## 1. Token format
- Type: JWT, signed with HMAC SHA-256 (HS256).
- Signing key: shared secret read from the environment variable `JWT_SECRET`
  (minimum 32 characters). Never committed to the repository.
- Lifetime: `JWT_EXPIRY_MINUTES` (default 60).x
- Sent by clients as `Authorization: Bearer <token>`.

## 2. Claims
| Claim   | Type   | Example                                  | Meaning                         |
|---------|--------|------------------------------------------|---------------------------------|
| sub     | string | 2f1c9a4e-...(UUID)                       | User id (stable identifier)     |
| email   | string | ayesha@example.com                       | Login email                     |
| role    | string | PASSENGER \| DRIVER \| ADMIN             | Exactly one role per user       |
| status  | string | ACTIVE \| SUSPENDED \| DEACTIVATED       | Account status at issue time    |
| iat     | number | 1757840000                               | Issued at (epoch seconds)       |
| exp     | number | 1757843600                               | Expiry (epoch seconds)          |

## 3. How other services validate
Each service validates tokens locally using the same `JWT_SECRET`.
No service calls the Account Service on every request.
1. Read the `Authorization` header. Missing or malformed → 401.
2. Verify signature and expiry. Invalid or expired → 401.
3. Load `sub`, `role`, `status` into the security context.
4. If `status != ACTIVE` → 403 `ACCOUNT_SUSPENDED`.
5. Enforce roles with `@PreAuthorize("hasRole('DRIVER')")` etc.
   Spring expects authorities prefixed `ROLE_`, so map `role` → `ROLE_<role>`.

## 4. Role rules
| Operation                        | Allowed roles      |
|----------------------------------|--------------------|
| Register, login                  | public             |
| Own profile (view/update)        | any authenticated  |
| Driver/vehicle profile, availability | DRIVER         |
| Request/cancel a ride            | PASSENGER          |
| Accept/start/complete a ride     | DRIVER (assigned)  |
| Fare estimate                    | PASSENGER, DRIVER  |
| Suspend user, change role, list users | ADMIN         |

## 5. Error envelope (all services)
All error responses use this JSON shape:
{ "code": "ACCOUNT_SUSPENDED", "message": "Human-readable text", "details": [] }

Standard codes: VALIDATION_FAILED (400), UNAUTHORIZED (401), FORBIDDEN (403),
ACCOUNT_SUSPENDED (403), NOT_FOUND (404), CONFLICT (409),
INVALID_STATE_TRANSITION (409), SERVICE_UNAVAILABLE (503).

## 6. Shared configuration
Every service reads these environment variables:
JWT_SECRET, JWT_EXPIRY_MINUTES
A `.env.example` in each service lists them with placeholder values.

## 7. Known limitation
Because validation is local, suspending a user does not invalidate tokens already
issued; they remain valid until `exp`. Mitigation: short expiry (60 min).
Documented as an accepted trade-off for this coursework.
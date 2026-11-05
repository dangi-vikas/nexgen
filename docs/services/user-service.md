# user-service

- **Port**: 8081
- **Database**: `userdb` (Flyway `V1__init.sql`, `V2__...`)
- **Role in the system**: the only issuer of tokens. Every other service validates the access token this service signs.

## Endpoints

| Method | Path | Access |
| --- | --- | --- |
| POST | `/api/v1/users/register` | public |
| POST | `/api/v1/users/login` | public |
| POST | `/api/v1/users/refresh-token` | public (refresh token in body) |
| POST | `/api/v1/users/logout` | authenticated |
| GET | `/api/v1/users/me` | authenticated |
| PUT | `/api/v1/users/update` | authenticated |
| PUT | `/api/v1/users/change-password` | authenticated |
| PATCH | `/api/v1/users/{username}/role` | `ROLE_ADMIN` |

## Tokens

- HS256, shared `JWT_SECRET` (minimum 32 bytes), `JWT_ISSUER=nexgen-user-service`, `JWT_AUDIENCE=nexgen-api`.
- Access and refresh tokens are distinguished by a `token_type` claim; resource servers reject a refresh token presented as an access token.
- Refresh tokens are rotated on use. Retired tokens are tracked in Redis, so a replay of an old refresh token is rejected.
- Registration always assigns `ROLE_USER`; roles are never accepted from the client.

## Roles and provisioning

There is no self-service path to `ROLE_ADMIN`; two mechanisms exist and nothing else:

1. **Bootstrap administrator.** At startup, if `BOOTSTRAP_ADMIN_USERNAME` and `BOOTSTRAP_ADMIN_PASSWORD` are both set, the service creates that account as `ROLE_ADMIN` when the username does not already exist. It is one-shot and idempotent: an existing account is never modified (no silent password reset, no silent promotion), and the runner does nothing when the variables are blank. Passwords shorter than 12 characters are refused.
2. **Admin-only role changes.** `PATCH /api/v1/users/{username}/role` with `{"role": "ROLE_ADMIN"}` or `{"role": "ROLE_USER"}`. The caller must hold `ROLE_ADMIN`, the target role is whitelisted (a client cannot invent `ROLE_INTERNAL`), and changing your own role is refused with `400` so a single account cannot lock the system out. Re-applying the current role is a no-op.

Each change is logged with the actor, the target and the old and new role, and the target's refresh tokens are deleted so the next sign-in picks up the new authority. Existing **access** tokens keep the role they were minted with until they expire (`JWT_ACCESS_TTL_MS`, 15 minutes by default) — there is no token introspection on every request.

Provisioning looks like this:

```bash
# 1. sign in as the bootstrap administrator
ADMIN=$(curl -s -X POST localhost:8081/api/v1/users/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<bootstrap password>"}' | jq -r .accessToken)
# 2. promote someone else
curl -s -X PATCH localhost:8081/api/v1/users/alice/role \
  -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"role":"ROLE_ADMIN"}'
```

## Events published

Written to the transactional outbox and relayed to Kafka:

`user-registration`, `user-login`, `user-profile-updated`, `user-password-changed`.

Each carries the username, e-mail and an `eventId`; notification-service consumes them to welcome the customer, warn about new sign-ins and confirm profile/password changes.

## Implementation notes

- `users.enabled` is `NOT NULL` and mapped on the entity; `isEnabled()` reflects it rather than hard-coding `true`.
- Unknown paths return `404` (`NoResourceFoundException` handler in `GlobalExceptionHandler`) instead of being swallowed into a `500`.
- Profile caching was removed; there is no stale user cache to invalidate.
- Actuator is on the classpath, so `/actuator/health` is available for readiness checks.

## Configuration

`DB_NAME=userdb`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_ISSUER`, `JWT_AUDIENCE`, `KAFKA_URL`, `REDIS_HOST`, `REDIS_PORT`, `CORS_ALLOWED_ORIGINS`, `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_PASSWORD`.

## Tests

54 unit tests (`./mvnw -B test`).

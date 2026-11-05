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

## Tokens

- HS256, shared `JWT_SECRET` (minimum 32 bytes), `JWT_ISSUER=nexgen-user-service`, `JWT_AUDIENCE=nexgen-api`.
- Access and refresh tokens are distinguished by a `token_type` claim; resource servers reject a refresh token presented as an access token.
- Refresh tokens are rotated on use. Retired tokens are tracked in Redis, so a replay of an old refresh token is rejected.
- Registration always assigns `ROLE_USER`; roles are never accepted from the client.

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

`DB_NAME=userdb`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_ISSUER`, `JWT_AUDIENCE`, `KAFKA_URL`, `REDIS_HOST`, `REDIS_PORT`, `CORS_ALLOWED_ORIGINS`.

## Tests

54 unit tests (`./mvnw -B test`).

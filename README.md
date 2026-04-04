# Authentication Application

A multi-module Spring Boot authentication and authorisation system built around a reusable **core-security-starter** library.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Module Breakdown](#module-breakdown)
3. [Prerequisites](#prerequisites)
4. [Build & Run](#build--run)
5. [Example Requests](#example-requests)
6. [Configuration Reference](#configuration-reference)
7. [Design Decisions & Trade-offs](#design-decisions--trade-offs)
8. [Testing](#testing)

---

## Architecture Overview

```
auth-system/                        ← Maven parent (pom only)
├── core-security-starter/          ← Reusable Spring Boot Starter (jar)
│   └── src/main/java/com/example/security/
│       ├── config/                 CoreSecurityAutoConfiguration.java
│       ├── filter/                 JwtAuthenticationFilter.java
│       ├── token/                  JwtTokenProvider.java
│       ├── exception/              SecurityExceptionHandler.java
│       ├── model/                  ApiErrorResponse.java
│       └── properties/             SecurityProperties.java
└── sample-application/             ← Consuming app (fat jar)
    └── src/main/java/com/example/app/
        ├── SampleApplication.java
        ├── config/                 DataInitializer, AppExceptionHandler
        ├── controller/             AuthController, UserController, AdminController
        ├── model/                  User, AuthDtos
        ├── repository/             UserRepository
        └── service/                AuthService, AppUserDetailsService
```

**Request flow:**

```
HTTP Request
    │
    ▼
JwtAuthenticationFilter          ← lives in starter
    │  extracts + validates JWT
    │  populates SecurityContext
    ▼
Spring Security Authorization    ← configured in starter
    │  URL-level rules
    │  @PreAuthorize (method-level)
    ▼
Controller → Service → Repository
    │
    ▼
ApiErrorResponse (on error)      ← envelope defined in starter
```

---

## Module Breakdown

### `core-security-starter`

| Component | Responsibility |
|-----------|---------------|
| `CoreSecurityAutoConfiguration` | Registers all beans via `@AutoConfiguration`; wires the `SecurityFilterChain` |
| `JwtTokenProvider` | Creates and validates signed JWTs (JJWT 0.12.x) |
| `JwtAuthenticationFilter` | `OncePerRequestFilter`; sets `SecurityContext` from JWT |
| `SecurityExceptionHandler` | Handles 401 (`AuthenticationEntryPoint`) and 403 (`AccessDeniedHandler`); also a `@RestControllerAdvice` |
| `ApiErrorResponse` | Immutable error envelope with builder |
| `SecurityProperties` | `@ConfigurationProperties(prefix="app.security")` |

Registered via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (Spring Boot 3.x standard).

### `sample-application`

Demonstrates all three required endpoints. Contains **zero JWT plumbing** – all cross-cutting concerns are delegated to the starter.

| Endpoint | Auth | Role |
|----------|------|------|
| `GET /api/public/health` | None | – |
| `POST /api/public/auth/login` | None | – |
| `POST /api/public/auth/register` | None | – |
| `GET /api/user/me` | JWT | Any authenticated |
| `GET /api/admin/users` | JWT | `ROLE_ADMIN` |

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.9+ |

No Docker or external database required – H2 in-memory is used for demo purposes.

---

## Build & Run

### 1. Clone

```bash
git clone <repo-url>
cd auth-system
```

### 2. Build both modules

```bash
./mvnw clean install
```

This installs `core-security-starter-1.0.0.jar` into your local Maven repo and builds the `sample-application` fat jar.

### 3. Run the sample application

```bash
java -jar sample-application/target/sample-application-1.0.0.jar
```

Or with Maven:

```bash
./mvnw spring-boot:run -pl sample-application
```

The app starts on **http://localhost:2026**.

Two users are seeded automatically:

| Username | Password | Roles |
|----------|----------|-------|
| `admin` | `AdminPass1!` | `ROLE_ADMIN`, `ROLE_USER` |
| `john` | `UserPass1!` | `ROLE_USER` |

---

## Example Requests

> Replace `<TOKEN>` with the `accessToken` value returned by the login endpoint.

### Public – Health check

```bash
curl http://localhost:8080/api/public/health
# → 200 UP
```

### Public – Login

```bash
curl -s -X POST http://localhost:8080/api/public/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"AdminPass1!"}' | jq .
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": "3fa85f64-...",
  "username": "admin",
  "roles": ["ROLE_ADMIN", "ROLE_USER"]
}
```

### Public – Register

```bash
curl -s -X POST http://localhost:8080/api/public/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","password":"MyPass123!"}' | jq .
```

### Authenticated – My profile

```bash
curl -s http://localhost:8080/api/user/me \
  -H "Authorization: Bearer <TOKEN>" | jq .
```

**Without token → 401:**
```json
{
  "timestamp": "2024-...",
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication is required to access this resource",
  "path": "/api/user/me"
}
```

### Admin – List users (ROLE_ADMIN required)

```bash
# Login as admin first, then:
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/public/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"AdminPass1!"}' | jq -r .accessToken)

curl -s http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

**With ROLE_USER token → 403:**
```json
{
  "timestamp": "2024-...",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to access this resource",
  "path": "/api/admin/users"
}
```

---

## Configuration Reference

All starter properties live under `app.security`:

```yaml
app:
  security:
    jwt-secret: "min-32-char-secret-key-here"  # required
    jwt-expiration-ms: 86400000                 # default: 24h
    jwt-issuer: sample-application              # default: core-security-starter
    public-paths:                               # paths bypassing JWT check
      - /api/public/**
      - /actuator/health
```

Override any property in the consuming app's `application.yml`. In production, inject `jwt-secret` via environment variable:

```bash
APP_SECURITY_JWT_SECRET=<vault-secret> java -jar sample-application.jar
```

---

## Design Decisions & Trade-offs

### 1. Auto-configuration via `AutoConfiguration.imports`
The starter uses Spring Boot 3's `AutoConfiguration.imports` (not the legacy `spring.factories`). This is the recommended approach for Boot 3+ and avoids startup-time deprecation warnings.

**Trade-off:** Not compatible with Spring Boot 2.x without adding a `spring.factories` fallback.

### 2. Roles sourced from JWT, not DB on every request
The JWT filter reads roles from the token claims rather than calling the database. This makes the system horizontally scalable (stateless) at the cost of roles not being revoked instantly – a new token is required to reflect role changes.

**Mitigation:** Keep token TTL short (e.g. 15 min access token + refresh token pattern) if real-time revocation is required.

### 3. `@ConditionalOnMissingBean` on all starter beans
Every bean in the auto-configuration is guarded with `@ConditionalOnMissingBean`. This means consuming applications can replace any single component (e.g. a custom `SecurityFilterChain`) without touching the rest.

### 4. Double-guarded admin endpoint
`/api/admin/**` is restricted at both the `SecurityFilterChain` URL level and with `@PreAuthorize` at the controller level. Defence in depth: a misconfigured URL rule doesn't automatically expose the data.

### 5. H2 in-memory database
Used solely for demo portability. In production, replace the datasource configuration with PostgreSQL/MySQL. No application code changes are needed.

### 6. BCrypt with default strength (10)
Strength 10 is the industry standard starting point (~100ms per hash on modern hardware). Increase to 12 for higher-security deployments at the cost of latency.

### 7. No refresh token implementation
Refresh tokens add significant complexity (token storage, rotation, revocation). The current design focuses on demonstrating the core JWT + RBAC mechanics. Refresh tokens would be the next logical addition.

---

## Testing

### Run all tests

```bash
./mvnw test
```

### Run only starter unit tests

```bash
./mvnw test -pl core-security-starter
```

### Run only integration tests

```bash
./mvnw test -pl sample-application
```

### Test coverage summary

| Test Class | Type | Scenarios |
|------------|------|-----------|
| `JwtTokenProviderTest` | Unit | Token generation, validation, expiry, wrong secret, claims extraction |
| `AuthIntegrationTest` | Integration (MockMvc) | All 3 endpoints × auth states, registration, validation errors, 401/403 responses |

---

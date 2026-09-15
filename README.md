# Cloud File Manager

A secure cloud file collaboration platform: per-file role-based sharing (owner/editor/viewer),
expiring shareable links, direct-to-S3 transfer via presigned URLs, malware scanning on
upload, a full audit trail, and rate limiting — built on Spring Boot + React + PostgreSQL +
S3-compatible object storage.

> **Origin note:** this project started from an Amigoscode Spring Boot/React tutorial
> template (basic auth + single-owner file CRUD). Everything past that — RBAC, sharing
> links, presigned S3 transfer, ClamAV scanning, the audit log, rate limiting, structured
> logging — is a substantial rebuild on top of that foundation, not the tutorial's own
> content. Said plainly here rather than glossed over.

## Features

- **Per-file RBAC** — Owner / Editor / Viewer roles, enforced on every file endpoint
  (view, download, delete), not just at the UI layer
- **Expiring shareable links** — generate a scoped, time-limited link that works for
  anonymous recipients with no account, revocable at any time
- **Direct-to-storage transfer** — uploads/downloads go straight between the browser and
  S3-compatible storage via presigned URLs; the backend only ever issues short-lived signed
  URLs, not proxied bytes (with an automatic fallback to backend-proxied transfer if
  presigned URLs aren't available in a given environment)
- **Malware scanning** — every upload is scanned against a real ClamAV instance
  (hand-written INSTREAM protocol client, no third-party ClamAV library) before it's
  persisted; infected files are rejected and cleaned up, fail-closed by default
- **Audit log / activity feed** — every mutating action (upload, download, delete,
  permission grant/revoke, link create/revoke/redeem) is recorded with actor, target file,
  and timestamp, queryable per-file (owner view) or as "my activity" (self view)
- **Rate limiting** — per-user (falling back to per-IP for anonymous requests) request
  throttling on the API, with no impact on bulk file-transfer throughput since bytes never
  pass through the rate-limited path
- **Structured logging** — JSON logs with a request-correlation ID propagated through
  every component touched by a request, for containerized/production environments
- **JWT authentication** with email verification, BCrypt password storage

## Tech Stack

### Backend
- **Spring Boot 3** (Java 17), **Spring Security** with JWT
- **PostgreSQL** — application data, **Flyway** — migrations
- **AWS SDK v2 (S3 + S3Presigner)** — object storage and presigned URL generation
- **MinIO** — S3-compatible object storage for local/docker-compose use (real signed-URL
  semantics without needing an AWS account)
- **ClamAV** — malware scanning via a from-scratch INSTREAM TCP client
- **logstash-logback-encoder** — structured JSON logging

### Frontend
- **React 18**, **React Router**, **Chakra UI**, **Axios**, **Vite**

## Setup

### Quick start (recommended — full feature set: RBAC, presigned URLs, malware scanning)

```bash
docker compose up --build
```

This brings up Postgres, MinIO (S3-compatible storage), ClamAV, the backend, and the
frontend together. First boot takes a few minutes (Maven dependency resolution + ClamAV
virus definitions download); subsequent starts are fast.

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- MinIO console: `http://localhost:9001` (user/pass: `minioadmin` / `minioadmin`)

> **Port note:** if `5332` (Postgres) is already in use on your machine, override it:
> `DB_HOST_PORT=5433 docker compose up --build`.

### Manual setup (backend + frontend against just a dockerized database)

Do **not** run this alongside `docker compose up` — both bind backend port 8080.

```bash
docker compose up -d db          # database only
cd backend && mvn spring-boot:run
cd frontend && npm install && npm run dev
```

In this mode, `AWS_S3_MOCK=true` by default (an in-process fake S3, no real HTTP endpoint —
presigned URLs aren't available in this mode and the app transparently falls back to
backend-proxied upload/download), and `EMAIL_ENABLED` defaults to `true` with no SMTP
credentials configured, which will make signup fail unless you either set
`EMAIL_ENABLED=false` or provide real `MAIL_USERNAME`/`MAIL_PASSWORD`.

### Prerequisites (manual setup only)

- Java 17+, Maven 3.6+, Node.js 18+, PostgreSQL 14+
- Docker (for the recommended quick-start path)

## Architecture

```
Browser ──┬── JWT-authenticated API calls ──> Spring Boot backend ──> PostgreSQL
          │                                         │
          │                                         ├──> ClamAV (scan on upload)
          │                                         └──> issues presigned URLs
          │
          └── direct PUT/GET (presigned URL) ──────> S3 / MinIO
```

- File bytes flow **directly between the browser and object storage** for both upload and
  download, not through the backend — except during upload completion, where the backend
  reads the object back once to run it through ClamAV before confirming the upload.
- RBAC is enforced in the service layer on every access path (owned files, shared files,
  and presigned-URL issuance all go through the same permission check), not just in the UI.
- Shareable links are a separate, scoped grant (role + expiry, single token) distinct from
  per-user permissions, and work without the recipient having an account.

## API Overview

| Area | Endpoints |
|---|---|
| Auth | `POST /api/v1/auth/{register,login,verify-email,resend-verification}` |
| Files | `POST /api/v1/files` (proxied upload) · `GET /api/v1/files/{id}/download` (proxied) · `GET /api/v1/files{,/all,/shared-with-me}` · `DELETE /api/v1/files/{id}` |
| Presigned transfer | `POST /api/v1/files/presigned-upload[/complete]` · `GET /api/v1/files/{id}/presigned-download` |
| Sharing | `{GET,POST} /api/v1/files/{id}/permissions` · `DELETE .../permissions/{userId}` |
| Share links | `{GET,POST} /api/v1/files/{id}/share-links` · `DELETE .../share-links/{id}` · public: `GET /api/v1/share/{token}[/download,/presigned-download]` |
| Audit log | `GET /api/v1/files/{id}/audit-log` (owner) · `GET /api/v1/audit-log/me` (self) |

## Testing

- JUnit 5 + Mockito unit tests: `FileServiceTest`, `FilePermissionServiceTest` (full
  role-permission matrix via parameterized tests), `ShareLinkServiceTest` (including
  expired/revoked-link denial), `RateLimitFilterTest`
- Testcontainers-backed repository test (`TestcontainersTest`) — spins up a real Postgres
  container and runs the Flyway migrations against it
- Malware scanning verified live against a real ClamAV instance using the standard EICAR
  test file (not a mock)
- Real load-test numbers (k6, 20 concurrent users, 30s, 1MB files) comparing the proxied
  and presigned transfer paths — see below

## Real Measured Performance

Replacing a previous unverified "25% latency reduction" claim with actual numbers,
recorded from a live k6 run against this stack (proxied path vs. presigned-URL path, same
load profile for both):

| Metric | Proxied path | Presigned path | Change |
|---|---|---|---|
| Avg request duration | 74.3ms | 30.9ms | **−58%** |
| p95 request duration | 153.9ms | 75.9ms | **−51%** |
| Completed transfer cycles/sec | 63.6/s | 111.1/s | **+75%** |

## Deployment

`backend/src/main/resources/application-aws.yml` configures the `aws` Spring profile:
Postgres via AWS Secrets Manager-backed JDBC (no hardcoded credentials), and real S3
(`AWS_S3_MOCK=false`). Presigned URLs work against real S3 with no code changes — just real
AWS credentials via the standard SDK credential chain (IAM role in production) instead of
MinIO's static ones. Running ClamAV in a managed AWS environment needs its own decision
(sidecar container vs. a separate ECS/Fargate service) that isn't made yet; the malware-scan
feature is currently only exercised via the docker-compose ClamAV container documented
above.

## Project Structure

```
backend/src/main/java/com/cloudfilemanager/
├── auth/            # Login, AuthenticationService, auth request/response DTOs
├── user/            # User entity, repository, service, email verification, DTOs
├── file/            # File entity, repository, service, controller (proxied + presigned), DTOs
├── file/sharing/     # FilePermission, FileShareLink, RBAC service, public ShareController
├── audit/           # Audit log entity, repository, service, controller
├── malware/         # MalwareScanner interface, ClamAvMalwareScanner, NoOpMalwareScanner
├── ratelimit/        # RateLimitFilter
├── storage/         # S3Service, S3Config (real S3Client + S3Presigner), S3Buckets, FakeS3
├── email/           # EmailService interface, SmtpEmailService, NoOpEmailService
├── security/        # JWT filter/util, Spring Security config, CORS
└── common/          # RequestIdFilter, shared exceptions, global exception handler

frontend/src/
├── api/             # API client (proxied + presigned + public share endpoints)
├── components/      # file/ (upload, card, ShareModal), layout/, forms/
├── pages/           # Route page components (Files, Activity, SharedFileView, ...)
└── utils/
```

## License

This project is part of a portfolio demonstration.

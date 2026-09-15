# Resume Bullets — Cloud File Manager

Every claim below is backed by something verified live in this repo (tests, curl/Playwright
runs, or the k6 load test) — no unverified numbers. Pick 2-4 depending on space; the first
three are the strongest / most differentiated.

- Designed and implemented per-file role-based access control (Owner/Editor/Viewer) and
  expiring shareable links for a Spring Boot + React file platform, enforced at the service
  layer across 8 REST endpoints and verified with a 22-case parameterized permission-matrix
  test suite plus live multi-account testing (curl and Playwright/Chromium).

- Replaced backend-proxied file transfer with presigned S3 URLs (direct browser-to-storage
  upload/download), cutting average API request latency 58% (74.3ms → 30.9ms) and
  increasing sustained transfer throughput 75% (63.6 → 111.1 completed cycles/sec) under a
  20-concurrent-user k6 load test — real numbers, not an estimate.

- Integrated real-time malware scanning (ClamAV) into the upload pipeline via a
  hand-written INSTREAM protocol client (no third-party dependency), with fail-closed
  behavior and automatic S3 object cleanup on detection; verified against the live EICAR
  test file with the actual scanner, not a mock.

- Built a full audit trail (8 tracked action types: upload/download/delete/permission
  grant-revoke/share-link create-revoke-redeem) with denormalized actor/file snapshots so
  history survives account or file deletion, exposed via per-file and per-user activity
  feeds.

- Added per-user rate limiting and structured JSON logging with request-correlation IDs
  propagated across every component in the request path (verified via live cross-component
  log correlation), without adding latency to bulk file transfer since bytes bypass the
  rate-limited API path entirely.

- Diagnosed and fixed a critical CORS misconfiguration (`allowCredentials=true` combined
  with a wildcard origin) that silently broke 100% of cross-origin requests — invisible to
  curl-based testing, only caught via live browser verification (Playwright), demonstrating
  the gap between API-level and true end-to-end testing.

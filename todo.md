# Secure Cloud File Management Platform — TODO

Spring Boot + React + PostgreSQL + AWS S3 + JWT. Started from a publicly available starter
template; repo/naming/structure cleanup already complete. This file tracks the pivot
from "8 known bugs" to a real, differentiated, resume-honest project.

Status legend: `[ ]` not started · `[~]` in progress · `[x]` done

---

## Phase 0 — Ground Truth (in progress)

- [x] Static codebase audit via Claude Code
  - [x] Confirmed only 2 entities: `User`, `File` (plain many-to-one, no permission tables)
  - [x] Confirmed upload/download fully proxy bytes through backend (no presigned URLs)
  - [x] Confirmed signup broken by default (sync SMTP call inside `@Transactional registerUser()`
        fails with blank mail creds → 500; only "works" via `docker-compose` because
        `EMAIL_ENABLED=false` is hardcoded there)
  - [x] Confirmed CI is green, 4/4 passing (contradicts original "CI has never passed" claim)
  - [x] Confirmed `application-aws.yml` uses env vars via AWS Secrets Manager (no hardcoded
        RDS hostname — contradicts original claim)
  - [x] Confirmed Flyway migrations `V1`, `V2` exist (foundation for RBAC schema)
  - [x] Confirmed Testcontainers-based `FileServiceTest` exists
  - [x] Confirmed `frontend/dist/` is checked into git (should be gitignored)
  - [x] Confirmed `scripts/start.sh` / port-conflict issues are real
- [~] Live verification run (docker-compose, full flow: register → verify → login →
      upload → list → download → delete, via API calls **and** actual browser)
  - [x] Resolve docker startup prompt (chosen: run in foreground first, watch logs live)
  - [x] Capture pass/fail + actual errors/console output for each step (API-level, via curl)
    - [x] Register: PASS (201)
    - [x] Verify email: PASS (200, token pulled from NoOpEmailService log)
    - [x] Login: PASS (200, real JWT returned in header + body)
    - [x] Upload: PASS, but found a real bug — login's returned Authorization header/token
          has NO "Bearer " prefix, yet JwtAuthenticationFilter requires "Bearer " —
          using the token exactly as returned gives 403 "Authentication required" on any
          authenticated endpoint. Frontend's api/client.js masks this by always prepending
          "Bearer " itself (client.js:28), so the bug is invisible in the UI but breaks any
          direct API consumer (curl/Postman/other services) that trusts the response literally.
    - [x] List (paginated + /all): PASS (200, file appears correctly)
    - [x] Download: PASS (200, MD5 verified byte-identical to uploaded file)
    - [x] Delete: PASS (200, gone from listing AND from FakeS3 backing storage)
    - [x] Bonus finding: `/actuator/health` returns 503 DOWN — Spring Boot's
          auto-configured mail health indicator tries a real SMTP connection to
          smtp.gmail.com with blank credentials regardless of app's own EMAIL_ENABLED
          flag (that flag only picks NoOpEmailService vs SmtpEmailService bean, doesn't
          affect Actuator's auto health check). Breaks any healthcheck/probe wired to
          /actuator/health in this docker-compose config.
    - [x] Bonus finding: docker-compose db port 5332 collided with an unrelated
          pre-existing container on this host (not a repo bug, but scripts/start.sh
          type of port-conflict fragility confirmed as a real class of issue)
  - [x] Specifically check for CORS failures (browser-only check, via real Playwright/Chromium
        run against http://localhost:3000) — **FOUND A CRITICAL BUG, browser-only, invisible
        to curl testing:**
    - [x] Register (UI): FAIL — browser console: "Access to XMLHttpRequest at
          '.../api/v1/auth/register' from origin 'http://localhost:3000' has been blocked by
          CORS policy: Response to preflight request doesn't pass access control check: No
          'Access-Control-Allow-Origin' header is present"
    - [x] Login (UI): FAIL — identical CORS error
    - [ ] Verify email / Upload / List / Download / Delete (UI): NOT TESTABLE — no session
          can ever be established through the browser because register+login both fail at
          the network layer before this bug is fixed
    - [x] Root cause confirmed server-side in backend logs (not a guess — actual stack trace
          fires on every cross-origin request):
          `java.lang.IllegalArgumentException: When allowCredentials is true, allowedOrigins
          cannot contain the special value "*" ... consider using "allowedOriginPatterns"
          instead.` at `CorsConfig.corsConfigurationSource()`. Cause: `CorsConfig.java:34`
          hardcodes `.setAllowCredentials(true)` while `cors.allowed-origins` defaults to `*`
          in application.yml and is never overridden in docker-compose.yml. Combining a
          wildcard origin with allowCredentials=true is invalid per the CORS spec — Spring
          throws on every request instead of emitting the Allow-Origin header.
    - [x] Severity: this breaks EVERY cross-origin API call the frontend makes, not just
          auth — the app is completely non-functional through the browser as currently
          configured. Directly contradicts the earlier (curl-only) conclusion that login/
          upload/etc. "work fine" — curl does not enforce CORS, so this was invisible to
          API-level testing alone.
- [x] Reconcile original 8(10)-bug list against static audit + live verification results
  - [x] #1 CORS misconfiguration — CONFIRMED, critical (browser + server stack trace)
  - [x] #2 Broken signup — CONFIRMED, two causes: SMTP sync-call in default config, AND CORS
        blocks it in-browser regardless of config
  - [x] #3 Broken file listing — symptom real but root-cause misattributed: API-level PASS,
        browser never reaches it (CORS kills the flow at login first)
  - [x] #4 Broken file upload — same as #3, API-level PASS, browser blocked upstream by CORS
  - [x] #5 Unverified logins may return 500 — CONFIRMED live: Spring Security's own
        `DisabledException` (thrown before app's manual `isEnabled()` check) has no handler
        in `GlobalExceptionHandler`, falls through to generic 500 handler
  - [x] #6 Email property mismatch (app.enabled vs app.email.enabled) — NOT FOUND, all usages
        consistently use `app.email.enabled`; treat as stale/inaccurate original claim
  - [x] #7 start.sh fails silently — ALREADY FIXED (commit 811478d)
  - [x] #8 setup.sh + docker-compose both bind backend to 8080 — CONFIRMED live (`lsof`
        shows host 8080 held by compose backend; setup.sh's own next-steps tell you to also
        run `mvn spring-boot:run`, same default port)
  - [x] #9 Hardcoded AWS RDS hostname — ALREADY FIXED (commit 4933550)
  - [x] #10 CI "has never passed" — STALE/FALSE (gh run list: 4/4 green on main)
  - [x] 3 bugs found beyond the original list (all live-confirmed): missing "Bearer " prefix
        on login response, /actuator/health false-DOWN via auto-wired mail health indicator,
        general host-port-conflict fragility (demonstrated on both 5332 and 8080)
- [x] Write the real fix-batch prompt based on confirmed (not assumed) bugs — see Phase 1
      below, scoped to only what was live-confirmed above (excludes #3/#4/#6/#7/#9/#10 since
      those are either not independent bugs, already fixed, or unconfirmed)

## Phase 1 — Real Bug Fixes (blocking — do before new features) — DONE

- [x] Fix CORS misconfiguration (#1) — `CorsConfig.java`: switched `setAllowedOrigins` to
      `setAllowedOriginPatterns` (works correctly with `allowCredentials(true)` regardless
      of whether origins is `*` or an explicit list)
- [x] Fix signup SMTP failure (#2a) — `EmailVerificationService.generateAndSendVerificationToken`
      now catches/logs email-send failures instead of letting them propagate and roll back
      `@Transactional registerUser()`
- [x] Fix unverified-login 500 (#5) — added a `DisabledException` handler to
      `GlobalExceptionHandler` (403, clear message); removed the now-provably-dead manual
      `isEnabled()` check in `AuthenticationService.login()` (Spring Security's own
      pre-authentication check always throws first)
- [x] Fix missing "Bearer " prefix bug (bonus) — `AuthController` now returns
      `Authorization: Bearer <token>` (a valid, directly-reusable header value);
      `frontend/api/client.js` updated to read the raw token from the response body only,
      avoiding a double-"Bearer" break
- [x] Fix `/actuator/health` false-DOWN (bonus) — added `management.health.mail.enabled: false`
      to `application.yml`
- [x] Fix setup.sh + docker-compose port-8080 collision (#8) — corrected setup.sh's printed
      next-steps to present docker-compose and the manual dev flow as alternatives, and to
      scope the manual flow's compose command to `docker-compose up -d db` only
- [x] `frontend/dist/` — investigated, turned out to NOT be tracked in git and already
      covered by `.gitignore`'s existing `**/dist/` rule; original claim was inaccurate, no
      fix needed
- [x] Rebuilt docker images and re-ran full live verification (API **and** real
      Playwright/Chromium browser) — full pass, evidence below

### Post-fix re-verification results (live, both API and browser)

- API: unverified login now returns `403 "Email not verified..."` (was 500); login's
  `Authorization` header (`Bearer <token>`) now works copy-pasted verbatim into the next
  request (was 403 before the fix); `/actuator/health` returns `200 {"status":"UP"}` (was 503)
- Browser (Playwright/Chromium against `http://localhost:3000`, fresh user each run):
  register → verify → login → upload → list → download (MD5-verified byte-identical) →
  delete (confirmed gone from both the API response and the UI) — **all 8 checks PASS**,
  **zero CORS issues, zero failed network requests**
- Minor, non-blocking observation: one benign `404` console message from a duplicate
  verify-email GET — caused by React 18 StrictMode double-invoking the effect in dev mode;
  the first call succeeds and the UI already reflects success before the harmless duplicate
  fires. Not user-impacting, not fixed as part of this batch (dev-only artifact, unrelated to
  the confirmed bug list)
- No code was committed to git — all changes are in the working tree pending your review

## Phase 2 — Per-File RBAC (biggest chunk of remaining work) — DONE

- [x] Design schema: roles (Owner implicit via `files.user_id`; Editor/Viewer via a new
      `file_permissions` join table), expiring shareable-link table (`file_share_links`)
- [x] Flyway migration `V3__Create_File_Sharing_Tables.sql` — `file_permissions` (file_id,
      user_id, role, granted_by, unique per file+user) and `file_share_links` (file_id,
      token, role, created_by, expires_at, revoked)
- [x] Backend: `FilePermissionService.effectiveRole`/`requireAtLeast` gate every file
      endpoint — owner fast-path preserved (keeps existing `FileServiceTest` green),
      falls back to permission-table lookup; view/download need VIEWER+, delete needs
      EDITOR+; no relation at all → 404 (no existence leak), insufficient role → new
      `ForbiddenOperationException` → 403
- [x] Backend: `POST/GET/DELETE /api/v1/files/{id}/permissions[/{userId}]` (owner-only)
      to grant/list/revoke a collaborator's role
- [x] Backend: `POST/GET/DELETE /api/v1/files/{id}/share-links[/{linkId}]` (owner-only,
      token = UUID, configurable expiry) + public `GET /api/v1/share/{token}` and
      `/api/v1/share/{token}/download` (no auth — added to `SecurityFilterChainConfig`
      permitAll list) with expiry/revocation checked in `ShareLinkService.resolveActiveLink`
- [x] Frontend: `ShareModal.jsx` — manage collaborators (add by email + role, list, revoke)
      and shareable links (create with role+expiry, copy, list status, revoke); wired into
      `FileCard` as a "Share" icon (owner-only) and into `Files.jsx`
- [x] Frontend: `SharedFileView.jsx` public page at `/share/:token` for anonymous link
      redemption (no login required); `Files.jsx` got a "Shared with Me" tab
      (`getSharedWithMeFiles`); `FileCard` shows a role badge and hides Delete for VIEWER,
      hides Share for non-owners
- [x] Tests: `FilePermissionServiceTest` (22 tests) — full role-permission matrix via
      `@ParameterizedTest`/`@CsvSource` (OWNER/EDITOR/VIEWER/NONE × required-role, all 12
      combinations) plus grant/revoke edge cases (self-grant rejected, non-owner rejected)
- [x] Tests: `ShareLinkServiceTest` (6 tests) — expired-link denied, revoked-link denied,
      unknown-token denied, valid-link resolves, non-owner can't create/list links
- [x] Full suite re-run under JDK 17 (matching CI/Docker, since host JDK 26 breaks Mockito's
      Byte Buddy locally): **33/33 tests PASS, BUILD SUCCESS**
- [x] Manual end-to-end verification with 3 real accounts (owner + editor + viewer), live,
      not inferred — see results below

### Live RBAC verification results

API (curl, 3 fresh accounts — owner/editor/viewer):
- Pre-grant: editor GETs file → `404` (no leak) ✓
- Owner grants EDITOR/VIEWER via API → both `201` ✓, `GET .../permissions` lists both ✓
- Editor GETs file → `200`, `role:"EDITOR"` ✓; Viewer GETs file → `200`, `role:"VIEWER"` ✓
- Editor shows up in `/files/shared-with-me` ✓
- Viewer downloads file → `200`, MD5-identical bytes ✓
- **Viewer attempts DELETE → `403 Forbidden`** ("You do not have permission..."), file
  confirmed still present afterward ✓ (this is the core RBAC guarantee, live-verified)
- Editor attempts DELETE → `200` succeeds, file confirmed gone for owner too ✓
- Owner creates a VIEWER share link → anonymous `curl` with **zero auth header** fetches
  metadata (`200`) and downloads (`200`, MD5-identical) ✓
- Owner revokes the link → anonymous fetch now `404` "expired or been revoked" ✓
- Expired-link denial (time-based, 1h minimum) verified via unit test rather than a live
  hour-long wait — same code path as revocation, which was live-verified

Browser (Playwright/Chromium, 2 separate browser contexts = owner + editor, real UI):
register → verify → login → upload (owner) → open Share modal → grant EDITOR access →
create share link → editor's "Shared with Me" tab shows the file → editor downloads
(MD5-identical) → editor's card shows a Delete button (EDITOR role) but no Share button
(owner-only) — **all 9 checks PASS**. Same benign StrictMode double-verify-email `404`
as Phase 1 (dev-only, non-blocking).

No code committed to git — all Phase 2 changes are in the working tree.

## Phase 3 — Presigned S3 URLs (replace backend-proxied transfer) — DONE

Key decision: FakeS3 is an in-process fake with no real HTTP listener, so a browser can
never PUT/GET it directly — presigned URLs need a real S3-compatible HTTP endpoint to mean
anything. Rather than fabricate a proxy that only *looks* like presigned URLs, I stood up
**MinIO** (genuine S3-compatible object storage) in docker-compose, so this phase is backed
by a real signed-URL flow, not a simulation. `docker-compose.yml` now runs `minio` +
a one-shot `minio-init` (creates the bucket) and points the backend at it with
`AWS_S3_MOCK=false`. (Note: had to switch the image references from `minio/minio` to
`quay.io/minio/minio` — Docker Hub now blocks anonymous pulls of the official image.)
FakeS3 itself is untouched and still used for local `AWS_S3_MOCK=true` runs, which
transparently fall back to the old backend-proxy endpoints (see below).

- [x] Backend: `S3Config` now builds a real `S3Client` + `S3Presigner` (region, optional
      endpoint override, optional path-style access for MinIO) instead of only `FakeS3`;
      `S3Presigner` classes turned out to already ship inside the `s3` SDK artifact — no
      new Maven dependency needed
      - Public/internal endpoint split: `aws.s3.endpoint` (what the *backend* uses,
        `http://minio:9000` on the docker network) vs. `aws.s3.public-endpoint` (what
        gets embedded in presigned URLs, `http://localhost:9000` — must be reachable by
        the browser, not just the container network)
- [x] Backend: `POST /api/v1/files/presigned-upload` (scoped per-user S3 key, 15-min
      expiry, no DB row created yet) → client PUTs bytes directly to S3/MinIO → `POST
      .../presigned-upload/complete` verifies the object actually exists via
      `S3Service.headObject` (defeats a client lying about a successful upload) before
      creating the `File` row, using the *real* object size from S3, not the client's claim
- [x] Backend: `GET /api/v1/files/{id}/presigned-download` — runs the exact same
      `FilePermissionService.requireAtLeast(VIEWER)` check as the old proxy download, then
      issues a presigned GET with `response-content-disposition` set so the original
      filename survives even though the S3 key is a UUID; same for the public share-link
      redemption path (`GET /api/v1/share/{token}/presigned-download`)
- [x] Frontend: `uploadFileDirect()` / `downloadFileDirect()` / `downloadSharedFileDirect()`
      in `client.js` — request the presigned URL, PUT/GET straight to S3 via a plain
      `axios.put`/`<a href>` (deliberately bypassing the app's auth-header-injecting axios
      instance, since these requests go to S3, not the backend), with automatic fallback
      to the old proxy calls if the presigned endpoint errors (e.g. `AWS_S3_MOCK=true`)
- [x] Old backend-proxy endpoints (`POST /api/v1/files`, `GET /api/v1/files/{id}/download`,
      `GET /api/v1/share/{token}/download`) — kept as-is, gated behind the fallback path
      rather than removed (per the todo's own "or gate behind fallback" option)
- [x] Edge cases: failed direct PUT surfaces a clear "Direct upload to storage failed..."
      error rather than silently retrying through a different path; file-size cap (50MB)
      enforced in the presigned-upload request DTO same as before; expired/invalid
      presigned URLs rely on standard AWS SigV4 expiry (framework-level, not custom code,
      so not separately re-tested beyond confirming a fresh URL works)
- [x] Manual verification: real end-to-end direct-S3 transfer, live, both API and browser

### Live presigned-URL verification results

API (curl): requested a presigned upload URL → host in the URL was `localhost:9000`
(browser-reachable, not the internal `minio:9000`) → raw `PUT` of file bytes straight to
that URL → `200` → `.../presigned-upload/complete` → `201`, real object size picked up from
S3. Requested a presigned download URL for that file → fetched with **zero auth header** →
`200`, `Content-Disposition: attachment; filename="presigntest.txt"` (original name
preserved despite the UUID storage key), bytes MD5-identical. A stranger with no
permission on the file requesting a presigned-download URL got `404` (RBAC from Phase 2
still enforced on the presigned path, not bypassed).

Browser (Playwright/Chromium, real UI, network-logged): upload flow shows
`POST .../presigned-upload -> 200` then (after the direct-to-MinIO PUT happens outside any
`/api/v1/` call) `POST .../presigned-upload/complete -> 201`; download flow shows
`GET .../presigned-download -> 200` followed by the browser fetching bytes straight from
`http://localhost:9000/filemanager-files/...` — **zero CORS issues**. All 8 checks
(register/verify/login/upload/list/download/delete ×2) PASS, MD5-verified byte-identical
download. (One cosmetic note: Playwright logs the direct MinIO GET as `net::ERR_ABORTED` —
this is expected Chromium behavior when a same-page anchor click triggers a file download
rather than a navigation; the MD5 match already proves the download genuinely succeeded.)

No code committed to git — all Phase 3 changes are in the working tree.

## Phase 4 — Malware Scanning (ClamAV) — DONE

Decision: **synchronous** scanning, not async. Reasoning: this app's max file size is
capped at 50MB and files are already handled as in-memory `byte[]` everywhere else in the
codebase (existing pattern, not something I introduced), so a synchronous ClamAV INSTREAM
scan adds bounded latency without needing a job queue, worker, or polling UI. Simpler to
reason about and fully testable without async status plumbing. Documented as a deliberate
tradeoff, not an oversight — a queue-based async scan would be the right call at a much
larger file-size ceiling.

- [x] Stood up ClamAV in docker-compose (`clamav/clamav:latest`, `platform: linux/amd64`
      since there's no arm64 manifest and this host is Apple Silicon — confirmed via
      `docker manifest inspect`, then emulated) with a `clamdcheck.sh`-based healthcheck
      (`start_period: 300s` for first-boot virus DB download); backend `depends_on` it
- [x] Backend: wrote a minimal clamd INSTREAM TCP client from scratch
      (`ClamAvMalwareScanner`, zero new dependencies) rather than pulling in a third-party
      ClamAV client library, since the protocol is simple (length-prefixed chunks + a
      zero-length terminator) and this keeps the implementation fully inspectable
      - Fail-closed by default (`app.malware-scan.fail-closed: true`) — if the scanner is
        unreachable, uploads are rejected rather than silently allowed, matching this
        project's security positioning; configurable to fail-open if desired
      - `NoOpMalwareScanner` (mirrors the existing EmailService/NoOpEmailService pattern in
        this codebase) when `app.malware-scan.enabled=false`
- [x] Infected-file behavior: **reject**, not quarantine. Legacy proxy upload scans bytes
      *before* ever calling `s3Service.putObject` (infected bytes never touch storage);
      presigned-upload flow scans *after* completion (S3 has no native pre-write scan hook)
      and deletes the object immediately on detection — confirmed live, see below
- [x] Frontend: N/A per the sync decision above — a normal failed-upload error
      (`errorNotification`) already surfaces the rejection with no extra UI needed
- [x] Tests: live EICAR test against the real ClamAV container (not a mock) — see below;
      plus `FileServiceTest.uploadFileRejectsInfectedFile` (mocked `MalwareScanner`) for
      fast regression coverage of the integration point

### Live malware-scan verification results

Uploaded the real EICAR test string via the legacy proxy endpoint: `422 "Malware detected
in uploaded file: Eicar-Signature"` — real ClamAV detection, not a stub. A clean control
file uploaded normally in the same session (`201`), confirming the scanner isn't just
rejecting everything. Repeated via the presigned-upload flow: PUT succeeded (S3 doesn't
scan), `POST .../complete` fetched the object back, scanned it, rejected with the same
`422`, and **the object was confirmed actually deleted from MinIO** afterward (`mc ls`
returned empty) — the reject-and-cleanup path works end-to-end, not just the detection.

## Phase 5 — Audit Log / Activity Feed — DONE

- [x] Schema: `V4__Create_Audit_Log_Table.sql` — `audit_log` (actor_user_id, actor_email,
      action, file_id, file_name_snapshot, metadata, created_at). Actor and file are
      nullable with `ON DELETE SET NULL`, and actor email / file name are denormalized
      (snapshotted at write time) so the trail stays meaningful even after the account or
      file is later deleted — a deleted file's own audit history still shows its name
- [x] Backend: `AuditLogService.record(...)` hooked into every mutating action across
      `FileService` (UPLOAD, DOWNLOAD, DELETE — both legacy and presigned paths),
      `FilePermissionService` (PERMISSION_GRANT, PERMISSION_REVOKE), `ShareLinkService`
      (SHARE_LINK_CREATE, SHARE_LINK_REVOKE), and `ShareController` (SHARE_LINK_REDEEM,
      actor=null for anonymous redemption). Logging failures are caught and logged, never
      allowed to break the operation being audited
- [x] Backend: `GET /api/v1/files/{id}/audit-log` (owner-only, paginated — "who did what to
      my file") and `GET /api/v1/audit-log/me` (current user's own activity across all
      files, paginated)
- [x] Frontend: per-file "Recent activity" section inside `ShareModal` (owner-only, matches
      the other owner-only sharing UI); a new global "My Activity" page
      (`Activity.jsx`, `/dashboard/activity`) with a sortable table, wired into `Sidebar`

### Live audit-log verification results

All 8 `AuditAction` values confirmed live in one session, not inferred: UPLOAD → DOWNLOAD →
SHARE_LINK_CREATE (metadata: "VIEWER, expires in 1h") → SHARE_LINK_REVOKE → DELETE showed
up in correct reverse-chronological order via `/audit-log/me`; PERMISSION_GRANT →
PERMISSION_REVOKE → SHARE_LINK_CREATE → SHARE_LINK_REDEEM (actorEmail correctly `null` for
the anonymous redemption) showed up via the per-file `/audit-log` endpoint. The
`fileNameSnapshot` denormalization confirmed working: the DELETE entry itself still shows
`testfile.txt` even though that file row no longer exists at query time.

## Phase 6 — Rate Limiting + Structured Logging — DONE

- [x] Rate limiting: in-memory fixed-window filter (`RateLimitFilter`), keyed by
      authenticated user id, falling back to client IP for anonymous requests (so public
      share-link redemption is still covered) — deliberately not Redis-backed since this
      runs as a single instance; documented as the tradeoff if it ever needs to scale
      horizontally. Wired explicitly into the Spring Security chain via `addFilterAfter`
      (positioned after `JwtAuthenticationFilter` so it sees the resolved principal) rather
      than relying on Spring Boot's default global filter auto-registration, which has
      unpredictable ordering relative to the security chain — confirmed via
      `FilterRegistrationBean.setEnabled(false)` + explicit chain placement, and verified
      in the startup log ("Filter rateLimitFilter was not registered (disabled)" +
      RateLimitFilter correctly appearing inside `DefaultSecurityFilterChain`'s filter list)
- [x] Structured logging: `logstash-logback-encoder` (verified the artifact exists on Maven
      Central before adding it this time, unlike the earlier `s3-presigner` mistake) gives
      JSON output in the `docker` Spring profile (human-readable plain-text kept as the
      default for local `mvn spring-boot:run`/IDE use); `RequestIdFilter` generates or
      propagates an `X-Request-Id` per request, puts it in the log MDC, and echoes it back
      as a response header
- [x] Confirmed rate limits don't threaten the presigned-URL flow: the limiter only ever
      throttles calls to *our own* API (short JSON responses); actual file bytes flow
      directly between the browser and S3/MinIO (Phase 3) and never pass through this
      filter at all, so bulk transfer throughput is structurally unaffected regardless of
      the requests-per-minute setting

### Live verification results

Burst-tested an authenticated endpoint: request #120 returned `429` with
`{"message":"Too many requests. Please slow down and try again shortly."}` and
`Retry-After: 60`; requests below the threshold kept returning `200`; the window reset
after 60s (a later request outside the burst succeeded again). Pulled one request's
`X-Request-Id` from its response header and grepped the backend's JSON logs for that exact
ID: found matching lines from three different components (Spring Security's
`FilterChainProxy`, Hibernate SQL, and `RateLimitFilter` itself) all correctly tagged with
the same `requestId` field — genuine cross-component correlation, confirmed live.

No code committed to git — all Phase 4/5/6 changes are in the working tree.

**Bonus finding during Phase 7 setup**: found and fixed a real, previously-unnoticed bug in
`RateLimitFilter` — it used `@ConditionalOnProperty(enabled=true)`, meaning the bean simply
didn't exist when `RATE_LIMIT_ENABLED=false`, but `SecurityFilterChainConfig` has a hard,
unconditional constructor dependency on it. Setting `RATE_LIMIT_ENABLED=false` therefore
crashed the entire application on startup (`UnsatisfiedDependencyException`) — confirmed
live via docker-compose when I tried to disable rate limiting for a clean load-test
baseline. Fixed by always registering the bean and gating behavior with an internal
`enabled` flag instead (matches how a filter with hard downstream dependents should be
built); added `RateLimitFilterTest` (2 tests) as a regression guard. Full suite re-run:
**35/35 pass** (`TestcontainersTest` still excluded — see Phase 2/3 notes on the
Docker-in-Docker Ryuk networking limitation of my local JDK17-in-container test-running
workaround; unrelated to any code in this repo).

## Phase 7 — Load Testing (real numbers to replace fabricated claim) — DONE

- [x] Tool: **k6** (`grafana/k6` docker image) — scriptable, produces clean p90/p95/avg
      summaries, no separate results server needed for a one-off comparison
- [x] Benchmarked the **old proxied path** (`POST /api/v1/files` multipart → backend reads
      and forwards bytes to S3 → `GET .../download` → backend fetches and streams back)
- [x] Benchmarked the **new presigned-URL path** (`POST .../presigned-upload` → direct
      `PUT` to MinIO → `POST .../complete` → `GET .../presigned-download` → direct `GET`
      from MinIO). Note: honestly, this path is *not* a full bypass of the backend for
      bytes — `completePresignedUpload` still reads the object back server-side to run it
      through ClamAV (Phase 4), so upload traffic touches the backend once either way.
      Download, however, is genuinely backend-byte-free in the presigned path.
- [x] Environment/parameters (same for both runs, recorded so the numbers are
      reproducible): 20 concurrent VUs, 30s sustained load, 1MB file payload, against the
      live docker-compose stack on this machine (Postgres + MinIO + ClamAV + backend all
      real, not mocked), rate limiting temporarily disabled for the run (see bug note
      above) so it wouldn't dominate the measurement, then re-enabled and re-verified
      immediately after (confirmed live: `429` still fires on a fresh burst test)
- [x] Real before/after numbers, replacing the fabricated "cut retrieval latency by 25%"
      claim:

  | Metric | Old proxied path | New presigned path | Change |
  |---|---|---|---|
  | Avg HTTP request duration | 74.29ms | 30.94ms | **−58.3%** |
  | p95 HTTP request duration | 153.86ms | 75.87ms | **−50.7%** |
  | Completed transfer cycles/sec | 63.57/s | 111.07/s | **+74.7%** |
  | Total HTTP requests handled | 8,242 (in 30s) | 19,315 (in 30s) | — |
  | Data transferred | 2.9 GB sent / 2.8 GB received | 3.4 GB sent / 3.4 GB received | — |

  ("Completed transfer cycles/sec" = full upload+download+delete iterations per second;
  the presigned path does more individual HTTP requests per cycle — 6 vs. 3 — since the
  presign/complete round-trips are separate from the actual data transfer, but each
  individual request is far cheaper for the backend to handle, which is what actually
  drives the throughput and latency wins.)

## Phase 8 — Packaging — DONE

- [x] Rewrote `README.md`: accurate current feature list (RBAC, share links, presigned
      transfer, malware scanning, audit log, rate limiting, structured logging), an
      explicit tutorial-origin note (stated plainly, not buried), an architecture diagram,
      an API overview table, real measured performance numbers, and setup instructions for
      both the full-featured docker-compose path and the manual/mock path (with its
      limitations honestly documented — no presigned URLs, signup needs
      `EMAIL_ENABLED=false`)
- [x] Deploy: **did not perform a live AWS deployment** — I have no AWS account/credentials
      in this environment, and standing up real cloud resources without explicit
      authorization isn't something to do autonomously regardless of capability. Instead,
      confirmed and documented the deployment *configuration* is coherent:
      `application-aws.yml` already uses env-var/Secrets-Manager-backed credentials (no
      hardcoded values, confirmed in the original Phase 0 audit), and the new
      presigned-URL code (`S3Config`) works against real AWS S3 with zero code changes —
      it only adds an endpoint override when one is explicitly configured (MinIO), so a
      real deployment just needs real AWS credentials via the standard SDK chain. Flagged
      as a genuinely open decision in the README: ClamAV has no AWS deployment story yet
      (sidecar vs. separate service) — that's a real gap, not glossed over.
- [x] `RESUME_BULLETS.md` — 6 draft bullets, every single one traceable to something
      actually verified live in this session (tests, curl/Playwright runs, or the k6
      numbers); no unverified claims carried over from the original draft
- [x] GitHub repo description updated via `gh repo edit` to match the current real feature
      set (was still describing the pre-RBAC/pre-presigned/pre-malware-scan state)

## Phase 9 — Frontend UI Audit & Independent Re-Verification — DONE

Requested as a follow-up skepticism check: Phase 2/3/5's `[x]` marks for frontend UI were
based on this session's own earlier work, so this phase independently re-audited them
against the actual current codebase (fresh `Read` calls, not memory) and then re-verified
live via a **new**, from-scratch Playwright run with fresh accounts — not a rerun of an
old script, a newly-written one targeting exactly the 4 items in question.

**Step 1 — Codebase audit result: all 4 items EXIST**, not partial, not missing:

| Item | Status | Evidence |
|---|---|---|
| (a) RBAC UI (view/manage collaborators, change role) | **EXISTS** | `frontend/src/components/file/ShareModal.jsx` — email+role form, collaborator list with role badges, revoke button; wired from `FileCard.jsx`'s Share icon (owner-only) via `Files.jsx` |
| (b) Share-link UI (generate/copy/revoke) | **EXISTS** | Same `ShareModal.jsx` — role+expiry form, link list with Active/Expired/Revoked badges, copy-to-clipboard button, revoke button |
| (c) Audit log UI | **EXISTS** | Per-file: "Recent activity" section inside `ShareModal.jsx`. Global: `frontend/src/pages/Activity.jsx` at `/dashboard/activity`, linked from `Sidebar.jsx` |
| (d) Presigned transfer UI | **EXISTS** | `FileUpload.jsx` calls `uploadFileDirect` (progress bar wired to the real PUT's `onUploadProgress`); `Files.jsx` download calls `downloadFileDirect` — both in `api/client.js`, both confirmed calling the real presigned endpoints, not the legacy proxy ones, by grepping for every function name referenced to confirm no dangling/undefined imports |

**Step 2 — Nothing to build.** All 4 items were already real, wired UI.

**Step 3 — Live verification (new Playwright script, 2 fresh real accounts, real
click-through, not API calls):**

| # | Check | Result |
|---|---|---|
| 1 | ShareModal opens, "People with access" section renders | PASS |
| 2 | Grant EDITOR role to a real second account via the UI form | PASS (201) |
| 3 | Collaborator row + EDITOR badge appear in the UI list | PASS |
| 4 | Revoke collaborator access via the UI's revoke button | PASS (200) |
| 5 | Collaborator row disappears from the UI list after revoke | PASS |
| 6 | Create a share link via the UI form | PASS (201) |
| 7 | New link shows "Active" badge | PASS |
| 8 | Copy-link button copies a real working `/share/...` URL to the clipboard (checked via `navigator.clipboard.readText()`) | PASS |
| 9 | Revoke the share link via the UI | PASS (200) |
| 10 | Link shows "Revoked" badge after revoke | PASS |
| 11 | Per-file "Recent activity" section shows real entries in the modal | PASS |
| 12 | Global "My Activity" page renders a table with real entries | PASS |
| 13 | Uploading the real EICAR file through the drag-drop UI surfaces a visible error toast (not a silent failure) | PASS |
| 14 | The rejected file does NOT appear as a card in the file grid afterward | PASS |

**16/16 checks pass** (2 setup + 14 above). Three test-script bugs were found and fixed
along the way (ambiguous Playwright text selectors colliding with the audit-log's own
metadata text, e.g. `text=Revoked` matching both a status badge and an unrelated
"PERMISSION_REVOKE" log line; the upload modal correctly staying open after a failed
upload, which the first script version mis-read as a stale file-grid entry) — all were
test-script issues, not application bugs; each is noted in this file's own history rather
than silently fixed.

**Step 4 — Full regression check:**
- Backend: `mvn test` (JDK 17, `TestcontainersTest` excluded per the known DooD/Ryuk
  networking limitation documented earlier) — **35/35 pass, BUILD SUCCESS**
- Browser E2E smoke test (`e2e.js`, full register→verify→login→upload→list→download→
  delete cycle): **8/8 pass**, zero CORS issues, zero unexpected console errors
- One real, non-app finding along the way: `docker logs` (full history, no `--tail`) started
  hitting Node's `execSync` buffer limit (`ENOBUFS`) partway through this phase, because
  the backend's accumulated JSON log volume from the Phase 7 k6 load test (~27,500 HTTP
  requests in one container lifetime) is now several MB. Fixed by tailing the last 500
  lines instead of the full history in the test scripts — a test-harness fix, not an app
  change, but worth noting since production log aggregation should assume similarly
  unbounded volume and never rely on "dump everything" queries either.

**Step 5 — This section.** No README changes were needed — the README's existing feature
list and API table already accurately describe RBAC, share links, the audit log, and
presigned transfer; this phase only added *evidence* that the frontend for each is real
and working, which doesn't change what the README claims.

No code was committed to git — this phase made no application code changes at all (nothing
was missing to build), only test-script fixes in the scratchpad directory (outside the repo).

---

## False Claims — RESOLVED (now backed by real work as of Phase 2/7)

- ✅ RBAC claim: was false when written (no RBAC existed). **Now true** — Owner/Editor/
     Viewer roles built in Phase 2, live-verified with 3 real accounts (curl) and 2 real
     browser sessions (Playwright), plus a 22-case automated permission matrix. Use the
     accurate framing from `RESUME_BULLETS.md`, not the original "20+ test accounts"
     wording (that specific number was never substantiated and I didn't try to hit it —
     3-5 real accounts across curl + browser is what was actually exercised).
- ✅ Latency claim: was false when written (no benchmark existed, "25%" was fabricated).
     **Now backed by a real k6 run** — see Phase 7: −58% avg / −51% p95 request latency,
     +75% throughput, presigned vs. proxied path. Use these numbers, not "25%".

## Notes / Open Questions

All phases (0–9) are done and live-verified as of this writing. Nothing is committed to
git — the working tree (24 modified + ~25 new files across backend/frontend/config) is
ready for review. Remaining genuinely open items, none blocking:

- ClamAV has no AWS deployment story yet (sidecar vs. separate ECS/Fargate service) — a
  real decision that was deliberately left open rather than guessed at (see Phase 8).
- No live AWS deployment was performed — no credentials available in this environment, and
  that's not a decision to make autonomously regardless of capability.
- `TestcontainersTest` (the one Postgres-Testcontainers smoke test, unrelated to any code
  written in this project) can't be run to completion in this specific local setup — a
  Docker-in-Docker networking limitation of wrapping `mvn test` in a JDK 17 container
  (needed because the host's JDK 26 breaks Mockito's Byte Buddy). Every other test — 35 of
  them, all written or modified in this project — runs and passes normally. This would not
  be an issue in the actual GitHub Actions CI, which runs directly on the runner.
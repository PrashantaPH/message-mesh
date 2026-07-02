# MessageMesh — Backend (`message-mesh-svc`)

Spring Boot 3 + Java 21 real-time chat backend (WebSocket/STOMP, JWT, JPA).

## Highlights
- **Real-time chat** over WebSocket/STOMP: 1:1 and group messaging, presence, typing, delivery/read receipts.
- **Rich messaging**: edit, soft-delete (tombstones), emoji reactions, threaded replies, and in-conversation search.
- **Group management**: rename, add/remove members, member roles, mute/archive, delete/leave — meta events pushed on `/topic/conv.{id}.meta`.
- **Profile self-service**: update display name, change password (rotates token), delete account.
- **Admin console API** (role-guarded): user management, conversation/message moderation, dashboard stats, and an append-only **audit trail**.
- **Security**: JWT with platform roles (USER/ADMIN) + token-version session revocation, method security, and token-bucket **rate limiting** on auth/admin endpoints.
- **Lombok** across entities, services, DTOs.
- **Profiles**: `dev` (embedded H2 file DB, verbose logs, H2 console) and `prod` (PostgreSQL, conservative DDL, rolling file logs).
- **MDC logging**: every HTTP request and STOMP frame is tagged with `requestId`, `username`, and `conversationId` (see `logback-spring.xml`). The `X-Request-Id` response header echoes the correlation id.
- **OpenAPI / Swagger UI** (springdoc): interactive docs with a JWT `Authorize` dialog; global bearer security scheme.

## Run (dev)
```powershell
$env:JAVA_HOME = (Split-Path (Split-Path (Get-Command java).Source -Parent) -Parent)
mvn spring-boot:run
```
Default profile is `dev`. App: http://localhost:8080 · H2 console: http://localhost:8080/h2-console · Swagger UI: http://localhost:8080/swagger-ui.html

> **Admin access:** on startup, if no administrator exists, the user named by `app.admin.bootstrap-username` (default `alice`, override with `APP_ADMIN_USERNAME`) is promoted to `ADMIN`. Register that user first, then sign in to reach the admin APIs.

## Run (prod)
```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:APP_JWT_SECRET = "<long-random-32+char-secret>"
$env:DB_URL = "jdbc:postgresql://localhost:5432/chatdb"
$env:DB_USERNAME = "chat"; $env:DB_PASSWORD = "chat"
mvn clean package
java -jar target/message-mesh-svc-1.0.0.jar
```

## Key endpoints
- **Auth**: `POST /api/auth/register`, `POST /api/auth/login`
- **Conversations**: `GET/POST /api/conversations`, `GET /api/conversations/{id}/messages?afterSeq=&limit=`, `GET /api/conversations/{id}/messages/search?q=`, `POST /api/conversations/{id}/read`, `GET/POST /api/conversations/{id}/members`, `PATCH /api/conversations/{id}` (rename), `PATCH /api/conversations/{id}/membership` (mute/archive), member remove/role, `DELETE /api/conversations/{id}` (delete/leave)
- **Messages**: `PATCH /api/messages/{id}` (edit), `DELETE /api/messages/{id}`, `POST/DELETE /api/messages/{id}/reactions`
- **Users & profile**: `GET /api/users`, `/api/users/me`, `PATCH /api/users/me`, `POST /api/users/me/password`, `DELETE /api/users/me`, `/api/users/online`
- **Admin** (ADMIN role): `/api/admin/stats`, `/api/admin/users` (+ role/status/reset-password/revoke-sessions/delete), `/api/admin/conversations` (+ restore/messages/moderation), `/api/admin/audit`
- **STOMP** endpoint: `/ws` (SockJS). Destinations: `/app/chat.send`, `/app/chat.ack`, `/app/chat.typing`, `/topic/conv.{id}`, `/topic/conv.{id}.typing`, `/topic/conv.{id}.meta`, `/topic/presence`, `/user/queue/ack`.

## API docs & ops
- OpenAPI spec: `GET /v3/api-docs` · Swagger UI: `/swagger-ui.html`
- Actuator: `/actuator/health`, `/actuator/info`, `/actuator/metrics`

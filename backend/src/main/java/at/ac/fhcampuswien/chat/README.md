# `chat/` — in-platform messaging

REST-based polling chat between a client and a designer, scoped to a job. **Implemented** — `ChatController` delegates to `ChatService`, backed by `ConversationRepository` + `MessageRepository`.

---

## files

| file | role |
|---|---|
| `ChatController.java` | REST endpoints under `/conversations`; reads the caller via `Authentication`, maps `ResponseStatusException` → JSON error bodies |
| `ChatService.java`    | participant guards + business logic (the controller stays thin) |
| `Conversation.java`   | model — `id`, `clientId`, `designerId`, `jobId`, `createdAt` |
| `Message.java`        | model — `id`, `conversationId`, `senderId`, `content`, `flagged`, `createdAt` |
| `ConversationRepository.java` / `MessageRepository.java` | hand-rolled JDBC against the H2 `conversations` / `messages` tables, matching `JobRepository`'s pattern |

---

## endpoints

| method | path | auth | behaviour |
|---|---|---|---|
| `GET`  | `/conversations` | authenticated | list conversations the caller participates in (either side) |
| `POST` | `/conversations` | authenticated | open new — body `{clientId, designerId, jobId}`; `403` if caller is neither party |
| `GET`  | `/conversations/{id}/messages` | authenticated, participant-only | paginated — `?page=0` (default), `403` for non-participants |
| `POST` | `/conversations/{id}/messages` | authenticated, participant-only | send a message; server assigns `id`, `senderId` = principal, `createdAt`; `flagged = false` |

Participant/auth failures are raised as `ResponseStatusException` inside `ChatService` and rendered as `{ "error": "..." }` by the controller's `@ExceptionHandler`.

---

## design choices

- **REST polling, not WebSockets.** Frontend pulls `GET /conversations/{id}/messages?page=0` on an interval (every 3–5 s while the chat view is open). This is acceptable at demo scale — scales to ~tens of users. Don't add WebSockets until you have a concrete reason.
- **`senderId` is server-set**, never trusted from the client. Read it via `Authentication.getPrincipal()` and write it on the message before persisting.
- **No edit / delete** of sent messages in V1 — keeps the audit trail simple for moderation.
- **`flagged` is writable by `moderation/`**, not by chat itself. When `moderation/` resolves a report against a message, it updates this field directly on the row.

---

## persistence sketch

```sql
CREATE TABLE IF NOT EXISTS conversations (
    id VARCHAR(36) PRIMARY KEY,
    client_id VARCHAR(36) NOT NULL,
    designer_id VARCHAR(36) NOT NULL,
    job_id VARCHAR(36) NOT NULL,
    created_at VARCHAR(50) NOT NULL,
    UNIQUE (client_id, designer_id, job_id)   -- one chat per pair-per-job
);

CREATE TABLE IF NOT EXISTS messages (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    flagged BOOLEAN NOT NULL DEFAULT FALSE,
    created_at VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation_created
    ON messages (conversation_id, created_at DESC);
```

The index keeps message-list queries fast — paginated reads always filter by `conversation_id` and order by `created_at DESC`.

---

## auth rules (when filling stubs)

- All endpoints require a valid JWT — already enforced by `SecurityConfig` (`/conversations/**` falls through to `authenticated()`).
- Inside each method:
  - `POST /conversations` — caller must be either `clientId` or `designerId`.
  - `GET /conversations/{id}/messages` — caller must be a participant of the conversation.
  - `POST /conversations/{id}/messages` — caller must be a participant; set `senderId` server-side.
- Return `403 Forbidden` on participant mismatch.

---

## moderation integration

Before persisting a message, run it through `ModerationService` (to be created in `moderation/`). The service decides whether to set `flagged = true` on the row, or whether to reject the message outright. Inject the service rather than calling the controller.

Until `moderation/` is implemented, persist messages with `flagged = false` and leave a `// TODO: moderation hook` comment.

---

## see also

- `account/README.md` — caller identity via `Authentication`.
- `application/README.md` — applications are typically what triggers a conversation; `conversation.jobId` ties back to the job.
- `moderation/Report.java` — the report shape for flagging a message after the fact.

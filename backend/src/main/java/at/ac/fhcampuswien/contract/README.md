# `contract/` — auto-generated freelance contracts

Triggered when a client hires a designer; produces a draft contract that both parties have to sign before it becomes active. **Currently all endpoints are 501 stubs (Phase 2).**

This README documents the contract the frontend already targets and what implementing it will need.

---

## files

| file | role |
|---|---|
| `ContractController.java` | REST endpoints under `/contracts` |
| `Contract.java`           | model — `id`, `applicationId`, `clientId`, `designerId`, `terms`, `status`, `createdAt` |

A `ContractRepository` is needed when implementing — follow `JobRepository`'s JDBC pattern. The `terms` field holds the rendered contract text (auto-generated from a template; not user-editable in V1).

---

## endpoints

| method | path | who calls | what it should do |
|---|---|---|---|
| `POST` | `/contracts`             | server-internal (from `application/` hire) | generate a draft contract from an accepted application — body `{ "applicationId": "..." }` |
| `GET`  | `/contracts/{id}`        | client or designer (parties only)           | fetch one |
| `PUT`  | `/contracts/{id}/sign`   | client or designer (parties only)           | sign — body `{ "party": "CLIENT"\|"DESIGNER" }`; activates when both have signed |

`POST /contracts` is exposed for symmetry but in practice **should be called via a `ContractService` injected into `ApplicationController`** rather than over HTTP. See `application/README.md` § cross-module call.

---

## status state machine

```
DRAFT ──► SIGNED_CLIENT ─┐
      └─► SIGNED_DESIGNER┴──► ACTIVE
                          
DRAFT / SIGNED_* ──► CANCELLED   (either party before ACTIVE)
```

- `DRAFT → SIGNED_CLIENT` (client signs first) or `→ SIGNED_DESIGNER` (designer signs first).
- `SIGNED_CLIENT → ACTIVE` only via designer sign; `SIGNED_DESIGNER → ACTIVE` only via client sign.
- `ACTIVE` and `CANCELLED` are terminal.
- Reject any transition not on this diagram (`400 Bad Request`).

The `party` field in the sign request must match the caller's role (or their relationship to the contract). Server reads `auth.getPrincipal()` and verifies it equals either `contract.clientId` or `contract.designerId` before accepting the sign.

---

## auto-generation

When `application/` calls into this package after a `HIRE`:

1. Look up the `JobApplication` by `applicationId` — must exist and be in `ACCEPTED` (or `HIRED`) status.
2. Pull `clientId` from the related `Job`, `designerId` from the application.
3. Render `terms` from a template (deliverables, deadline, budget, payment terms) — V1 can use a static string; V2 can pull from the job's fields.
4. Persist with `status = "DRAFT"`, `createdAt = Instant.now()`.
5. Return the new `Contract`.

---

## auth model

All endpoints require a valid JWT. `SecurityConfig` routes `/contracts/**` through `authenticated()`.

Inside each method:
- `POST /contracts` — caller must be the job owner (`clientId` of the underlying job). In practice this is enforced upstream by `application/` when it triggers contract creation.
- `GET /contracts/{id}` — caller must equal `contract.clientId` or `contract.designerId`.
- `PUT /contracts/{id}/sign` — same; additionally the `party` field must match which side the caller is on.

Return `403 Forbidden` on participant mismatch.

---

## persistence sketch

```sql
CREATE TABLE IF NOT EXISTS contracts (
    id              VARCHAR(36) PRIMARY KEY,
    application_id  VARCHAR(36) NOT NULL,
    client_id       VARCHAR(36) NOT NULL,
    designer_id     VARCHAR(36) NOT NULL,
    terms           TEXT,
    status          VARCHAR(20) NOT NULL,
    created_at      VARCHAR(50) NOT NULL,
    UNIQUE (application_id)              -- one contract per accepted application
);
```

`application_id` references `applications.id`; `client_id` / `designer_id` reference `users.id`. No FKs at demo scale.

---

## see also

- `application/README.md` — the hire path that triggers contract creation.
- `auth/README.md` — caller identity via `Authentication`.

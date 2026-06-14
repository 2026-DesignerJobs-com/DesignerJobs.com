# `application/` — apply & hire flow

Wires designers to jobs and (eventually) triggers contract creation. **Implemented** — `ApplicationController` + `JobApplicationRepository` are live; the only gap is the contract hand-off, which is still a `// TODO` because `contract/` is a stub.

---

## files

| file | role |
|---|---|
| `ApplicationController.java` | REST endpoints — mixed bases under `/jobs/{jobId}/...` and `/applications/...` |
| `JobApplication.java`        | model — `id`, `jobId`, `designerId`, `coverLetter`, `status`, `appliedAt` |
| `JobApplicationRepository.java` | hand-rolled JDBC against the H2 `applications` table; creates the table in its constructor |
| `DuplicateApplicationException.java` | thrown when the `UNIQUE (job_id, designer_id)` constraint is hit; mapped to `409` |

---

## endpoints

| method | path | who calls | behaviour |
|---|---|---|---|
| `POST` | `/jobs/{jobId}/apply` | designer | create a `JobApplication` (`PENDING`); `designerId` is server-set from the token; `409` if already applied; `403` if caller is not a `DESIGNER`; `404` if the job is missing |
| `GET`  | `/jobs/{jobId}/applications` | client (owner of the job) | list applications for one job; `403` for non-owners |
| `GET`  | `/applications/{id}` | client (job owner) or applicant | fetch one; `403` for anyone else |
| `PUT`  | `/applications/{id}/status` | client (job owner) | body `{"status":"ACCEPTED"\|"REJECTED"}`; only a `PENDING` application can transition |
| `POST` | `/applications/{id}/hire` | client (job owner) | only an `ACCEPTED` application → `HIRED`. **Contract auto-generation is still a `// TODO`** until `contract/` is implemented |

Status transitions use a compare-and-set (`updateStatusFrom(id, expected, new)`), so concurrent updates return `409` rather than corrupting the state machine.

---

## status state machine

```
PENDING ─┬─► ACCEPTED ─► HIRED
         └─► REJECTED
```

- `PENDING → ACCEPTED | REJECTED`
- `ACCEPTED → HIRED`
- `REJECTED` and `HIRED` are terminal — block transitions out.

Validate this server-side in `PUT /applications/{id}/status`. Don't trust the client to follow the FSM.

---

## auth model

All endpoints require a valid JWT. The current `SecurityConfig` already routes anything outside `/auth/**` and `GET /jobs/**` through `authenticated()`, so reaching these methods means there's a populated `Authentication` to read from.

Ownership rules to enforce inside each method:
- `POST /jobs/{jobId}/apply` — the caller must be a `DESIGNER`. The `designerId` field should be set to `auth.getPrincipal()`, **never** read from the request body.
- `GET /jobs/{jobId}/applications` — caller must be the job's `clientId`.
- `GET /applications/{id}` — caller must be either the job owner or the applicant.
- `PUT /applications/{id}/status` — caller must be the job owner.
- `POST /applications/{id}/hire` — caller must be the job owner; application status must be `ACCEPTED`.

Read the caller via:

```java
@PostMapping("/jobs/{jobId}/apply")
public ResponseEntity<?> apply(@PathVariable String jobId,
                               @RequestBody JobApplication body,
                               Authentication auth) {
    String designerId = (String) auth.getPrincipal();
    …
}
```

---

## cross-module call: hire → contract

When `POST /applications/{id}/hire` succeeds, it should create a `DRAFT` contract. The intended design is to inject a `ContractService` (to live in the `contract/` package) and call it directly — never call `ContractController` from another controller.

**Not wired yet:** `contract/` is still a 501 stub, so the hire path carries a `// TODO: trigger contract creation` comment and returns `200` with the updated (`HIRED`) application. Wire the call in once `ContractService` exists.

---

## persistence — the `applications` table

`JobApplicationRepository` creates the table in its constructor (`CREATE TABLE IF NOT EXISTS`) and uses prepared statements + try-with-resources, following the `JobRepository` pattern:

```sql
CREATE TABLE IF NOT EXISTS applications (
    id VARCHAR(36) PRIMARY KEY,
    job_id VARCHAR(36) NOT NULL,
    designer_id VARCHAR(36) NOT NULL,
    cover_letter TEXT,
    status VARCHAR(20) NOT NULL,
    applied_at VARCHAR(50) NOT NULL,
    CONSTRAINT uq_applications_job_designer UNIQUE (job_id, designer_id)
);
```

The `UNIQUE (job_id, designer_id)` constraint enforces "one application per designer per job" at the DB level — the controller's up-front `existsByJobIdAndDesignerId` check is just for a clean `409`, with the constraint (surfaced as `DuplicateApplicationException`) as the race-proof backstop. `job_id` references `jobs.id`, `designer_id` references `users.id` — no foreign keys at demo scale.

---

## see also

- `account/README.md` — how to read the caller via `Authentication`.
- `infrastructure/config/README.md` — where to add public route exceptions if any (probably none — apply/hire is private).

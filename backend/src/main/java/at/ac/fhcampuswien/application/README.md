# `application/` — apply & hire flow

Wires designers to jobs and triggers contract creation. **Currently all endpoints are 501 stubs.** This README documents the contract the frontend already targets and the intended behaviour so whoever picks this up next has a sharp brief.

---

## files

| file | role |
|---|---|
| `ApplicationController.java` | REST endpoints — mixed bases under `/jobs/{jobId}/...` and `/applications/...` |
| `JobApplication.java`        | model — `id`, `jobId`, `designerId`, `coverLetter`, `status`, `appliedAt` |

There is no repository yet. When implementing, follow the `JobRepository` template: hand-rolled JDBC against H2, `@Repository`, prepared statements, `CREATE TABLE IF NOT EXISTS applications` in the constructor (or extend `DatabaseInitializer`).

---

## endpoints

| method | path | who calls | what it should do |
|---|---|---|---|
| `POST` | `/jobs/{jobId}/apply` | designer | create `JobApplication` with status `PENDING`, `appliedAt = Instant.now()` |
| `GET`  | `/jobs/{jobId}/applications` | client (owner of the job) | list applications for one job |
| `GET`  | `/applications/{id}` | client (job owner) or applicant | fetch one |
| `PUT`  | `/applications/{id}/status` | client | body `{"status":"ACCEPTED"\|"REJECTED"}` |
| `POST` | `/applications/{id}/hire` | client | status → `HIRED`, **calls into `contract/` to auto-generate a draft contract** |

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

When `POST /applications/{id}/hire` succeeds, it must create a `DRAFT` contract. Inject a `ContractService` (to be created in the `contract/` package) and call it directly — do not call `ContractController` from another controller.

Until `contract/` is implemented, leave a `// TODO: trigger contract creation` comment in the hire path and return 200 with the updated application.

---

## persistence sketch

When you implement `JobApplicationRepository`:

```sql
CREATE TABLE IF NOT EXISTS applications (
    id VARCHAR(36) PRIMARY KEY,
    job_id VARCHAR(36) NOT NULL,
    designer_id VARCHAR(36) NOT NULL,
    cover_letter TEXT,
    status VARCHAR(20) NOT NULL,
    applied_at VARCHAR(50) NOT NULL
);
```

`job_id` references `jobs.id`, `designer_id` references `users.id` — no foreign keys yet, manual referential integrity is fine at demo scale.

---

## see also

- `account/README.md` — how to read the caller via `Authentication`.
- `infrastructure/config/README.md` — where to add public route exceptions if any (probably none — apply/hire is private).

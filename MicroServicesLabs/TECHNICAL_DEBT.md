# Technical Debt Register — v2 (Architecture Clinic #2, Session 24)

Checked against the repo on 2026-09-28, not the deck's assumed state. Where they differ, this file follows
the code.

## RESOLVED

| Item | Named in | Resolved by |
|---|---|---|
| No API versioning strategy | S8 Clinic #1 | S22, `ab595b1`: `/api/v1/` + Gateway legacy rewrite with `Deprecation`/`Sunset` (legacy writes still ADMIN-only). Remove the legacy route after one cycle. |
| Hardcoded JWT secret, no real Identity Provider | S3 / S8 | S19–20, `bd50ba9` + `1b3cdeb`: Keycloak, JwtAuthFilter/JwtUtil deleted, Gateway + order-service are Resource Servers, Client Credentials for order→inventory |
| No structured logging | S8 | **Partially**, S17: JSON logs + trace correlation in **product-service only**. Other services still log plain text. |
| No verified way to stop plaintext service-to-service traffic | S21 | **Partially**, S21, `8035f68`: STRICT mTLS + 80/20 VirtualService, but only product-service is in K8s/the mesh |

## STILL OPEN (carried into Phase 4)

| # | Item | Evidence | Priority |
|---|---|---|---|
| 1 | No idempotency on payment retry | S22 chose Versioning instead; the deck's v2 slide wrongly lists this as resolved | Medium. Latent until someone adds `@Retry` to the payment call, and it must land first. |
| 2 | Saga dual write (save then Kafka send, no shared tx) | `OrderService.createOrder` | Medium. Needs order-service on a real DB before the Outbox can exist. |
| 3 | In-memory `SagaState` | `OrderSagaOrchestrator`: `ConcurrentHashMap<String, SagaState>`, lost on restart | **High.** A restart mid-saga silently strands orders, and it's the same root cause as #2 (no persistence in order-service). |
| 4 | No DB migrations | No Flyway/Liquibase; product-service uses `ddl-auto: update` | Medium. Fine for one table, unsafe the day a column is renamed. |
| 5 | No dead-letter handling | No DLT/`DeadLetterPublishingRecoverer` anywhere; a poison message is retried then dropped | **High.** Silent message loss in the saga path. |
| 6 | Resilience patterns missing or unreachable | No Bulkhead/TimeLimiter/Retry; the only `@CircuitBreaker` (`createOrderWithPayment`) isn't called by any endpoint (S23, `k6/FINDINGS.md`) | Medium. Config that "exists" but can't fire is worse than none, because it gives false confidence. |
| 7 | Hardcoded route/security config | Public and ADMIN paths are literals in `SecurityConfig` (the successor to `PUBLIC_ROUTES`) | Low. Correct today, but every route needs editing in two places (yml + Java). |

## NEWLY SURFACED (Phase 3)

| # | Item | Found in | Priority |
|---|---|---|---|
| 8 | Gateway `RequestRateLimiter` never throttles (Lettuce/`replicate_commands` bug, fails open) | S3, reconfirmed S20 | Medium. The config suggests protection that doesn't exist. |
| 9 | `GET /api/v1/products/{id}` for a missing id returns **500**, not 404 (Redis cache rejects the `null` from an empty `Optional`) | S18 | Low. One-line fix: `unless = "#result == null"`. |
| 10 | Keycloak runs `start-dev` on embedded H2 with no volume: `compose down` wipes the realm, and a hard Docker stop corrupted it | S19–20 | Medium. The whole security stack depends on hand-recreating realm/clients/roles; needs a realm export/import or Postgres. |
| 11 | Dev secrets in `docker-compose.yml` (`KEYCLOAK_ADMIN_PASSWORD=admin`, `ORDER_SERVICE_CLIENT_SECRET=order-service-dev-secret`) | S19–20 | Low for training, blocking for anything real. Move them to `.env`/K8s Secrets. |
| 12 | The local stack no longer fits a laptop: ~11 containers exceed a 3 GB Docker VM (engine hangs); K8s + Istio + 4 JVMs exceed 5 GB | S20–21 | Medium for Capstone. Profiles or a "core" compose file are needed so teams can run a slice. |
| 13 | CQRS read-model issues: the projection drops `description` from GET, and there's no event-driven read store (same DB, same service) | S18 | Low. That's appropriate for the scale; revisit only if read load is real. |

## Architecture Clinic #2 — Review Memo

1. **CQRS (S18): marginal.** The split is clean, but reads and writes share one table, one service and one
   Redis cache, so it separates code paths, not scaling. It was justified by the *described* dashboard
   conflict, not a measured one. Keep it, and don't copy it elsewhere without a real read/write pain.
2. **Istio (S21): valuable to learn, not needed here.** One service in the mesh, one language, one team. Its
   real cost showed immediately: the sidecars plus the control plane pushed the laptop past its memory
   limits. The mTLS benefit is real, but a team this size could reasonably have waited.
3. **JwtAuthFilter cutover (S20): right for training, risky for production.** A clean cutover worked
   because every client was ours. With external consumers, a parallel run (accept both HS256 and Keycloak
   tokens for one window) would have been necessary.
4. **Unaddressed S8/S12 debt:** in-memory SagaState (#3) and no DLT (#5) matter most. Both lose orders
   silently, and #3 shares a root cause with the Outbox gap (#2): order-service has no database.
5. **One decision to make differently:** give order-service real persistence (JPA + Postgres) back in S7
   when the saga was built. Items #2, #3 and the Outbox all trace back to that one shortcut.

*(The individual reflection questions from slide 18 are personal. They're left for the trainee to answer.)*

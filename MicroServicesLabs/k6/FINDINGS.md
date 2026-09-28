# Lab 19 — Bottleneck Findings

**Status:** scripts written and validated (`k6 inspect`, k6 v2.3.0). **The stress test was not run live**, by
choice. The code review below shows it could not have shown what the lab asks for without first adding the
missing resilience patterns.

## Scripts

| Script | Type | Target |
|---|---|---|
| `smoke-test.js` | Smoke: 1 VU / 10 s, p95 < 500 ms | `GET /api/v1/products` (public) |
| `order-load-test.js` | Load: ramp to 20 VUs, hold 2 min | `POST /api/orders` with `TEST_JWT` |
| `stress-test.js` | Stress: 50 → 150 VUs | `POST /api/orders` with `TEST_JWT` |
| `checkout-stress-test.js` | Stress, full chain: 50 → 150 VUs | `POST /api/orders/saga` with `TEST_JWT` |

Run with a real Keycloak token (Session 19 customer token), with no auth bypass:
`TEST_JWT=<token> k6 run k6/checkout-stress-test.js`

## Finding: the documented firing order cannot happen on this platform

Session 5 documents **Bulkhead → TimeLimiter → CircuitBreaker → Retry**. In this repo:

| Pattern | Present? | Where |
|---|---|---|
| Bulkhead | **No** | no `@Bulkhead` or `resilience4j.bulkhead` config in any service |
| TimeLimiter | **No** | none |
| Retry | **No** | none (see `TECHNICAL_DEBT.md` #2) |
| CircuitBreaker | Yes | `OrderService.createOrderWithPayment` (`paymentService` instance) |

**The one CircuitBreaker is unreachable.** No controller calls `createOrderWithPayment`. The endpoints
that load tests can hit are all asynchronous:

- `POST /api/orders` saves to an in-memory map, sends a Kafka message and returns PENDING.
- `POST /api/orders/saga` starts the orchestrator. Inventory and payment are reached **over Kafka**, not
  HTTP.
- `GET /api/orders/stock-check` makes a synchronous Feign call to inventory (Session 20) with no resilience
  annotation.

So under the deck's stress profile, **no Resilience4j pattern can fire at all**. Load on the order path never
waits on payment or inventory over HTTP.

## Predicted bottleneck (not measured)

With no synchronous downstream calls, the first limits reached would be infrastructure, not resilience:

1. **Memory on this machine.** The full chain needs about 11 containers in a 5 GB Docker VM (Session 20:
   ~2.6 GB at idle). 150 VUs would add Tomcat and Netty threads and Kafka producer buffers on top.
2. **Gateway rate limiting will not throttle.** The Redis `RequestRateLimiter` fails open (known Lettuce bug,
   Session 3), and it's only on the product route anyway.
3. **The Kafka consumer side**, where the saga steps run, would lag behind the HTTP layer. The HTTP responses
   stay fast (PENDING) while the saga backlog grows. That backlog is the real throughput limit, and k6
   latency won't show it.

## Zipkin correlation

Not done, because there was no live run. The procedure for when it is run: take a failed request's timestamp
and `traceparent`/`X-B3-TraceId` from the Gateway logs, look it up in Zipkin (:9411), and find the longest or
error span (expected: the `order-events` Kafka produce, or a consumer span in inventory or payment).

## To make the lab observable (future work)

Expose `createOrderWithPayment` (for example `POST /api/orders/with-payment`), and add
`@Bulkhead(name = "paymentService")` with `max-concurrent-calls: 10`, plus `@Retry` with an idempotency key
(Session 22 pattern B). Then run `stress-test.js` against that endpoint while watching
`/actuator/bulkheads` and `/actuator/circuitbreakers`.

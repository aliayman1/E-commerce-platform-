import http from 'k6/http';
import { check } from 'k6';

// Full chain: Gateway (JWT) -> order-service saga -> Kafka -> inventory-service -> payment-service
export const options = {
  stages: [
    { duration: '20s', target: 50 },
    { duration: '40s', target: 150 },
    { duration: '20s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.5'],
    http_req_duration: ['p(95)<2000'],
  },
};

export default function () {
  const payload = JSON.stringify({
    productId: 'PROD-001',
    quantity: 1,
    amount: 49.99,
    customerId: `k6-vu-${__VU}`,
  });
  const params = {
    headers: { Authorization: `Bearer ${__ENV.TEST_JWT}`, 'Content-Type': 'application/json' },
    tags: { name: 'checkout-saga' },
  };
  const res = http.post('http://localhost:8080/api/orders/saga', payload, params);
  check(res, { 'status 200/202': (r) => [200, 202].includes(r.status) });
}

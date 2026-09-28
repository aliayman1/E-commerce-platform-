import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '20s', target: 50 },
    { duration: '20s', target: 150 }, // well beyond S5's Bulkhead max-concurrent-calls: 10
    { duration: '20s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.5'],  // deliberately loose — we EXPECT failures
  },
};

const payload = JSON.stringify({ productId: 'PROD-001', quantity: 1, amount: 49.99, customerId: 'k6-customer' });

export default function () {
  const params = {
    headers: { Authorization: `Bearer ${__ENV.TEST_JWT}`, 'Content-Type': 'application/json' },
  };
  const res = http.post('http://localhost:8080/api/orders', payload, params);
  check(res, { 'status 200/202': (r) => [200, 202].includes(r.status) });
}

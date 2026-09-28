import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },  // ramp up to 20 VUs
    { duration: '2m',  target: 20 },  // sustain 20 VUs — the LOAD TEST
    { duration: '30s', target: 0 },   // ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<800', 'p(99)<2000'],
    http_req_failed:   ['rate<0.05'],
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

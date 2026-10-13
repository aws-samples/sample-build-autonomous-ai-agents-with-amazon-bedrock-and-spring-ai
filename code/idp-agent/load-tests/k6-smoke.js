// k6 load / smoke test for the IDP agent.
//
// Install k6 (https://k6.io/docs/get-started/installation/), start the backend on 8081, then:
//   BASE_URL=http://localhost:8081 k6 run code/idp-agent/load-tests/k6-smoke.js
//
// Exercises the lightweight, no-cost endpoints (health, agent list, review queue) under a
// ramping load. Document processing itself is intentionally not load-tested here because it
// calls Amazon Bedrock (billable) and reads real PDFs; drive that separately and sparingly.

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

export const options = {
  stages: [
    { duration: '20s', target: 25 },
    { duration: '40s', target: 25 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

export default function () {
  const health = http.get(`${BASE_URL}/actuator/health`);
  check(health, { 'health 200': (r) => r.status === 200 });

  const agents = http.get(`${BASE_URL}/api/agents`);
  check(agents, {
    'agents 200': (r) => r.status === 200,
    'has LAWYER': (r) => (r.json() || []).includes('LAWYER'),
  });

  const review = http.get(`${BASE_URL}/api/review`);
  check(review, { 'review 200': (r) => r.status === 200 });
  sleep(1);
}

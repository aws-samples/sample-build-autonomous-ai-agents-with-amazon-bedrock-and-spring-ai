// k6 load / smoke test for the autonomous agent.
//
// Install k6 (https://k6.io/docs/get-started/installation/), start the backend, then:
//   k6 run code/autonomous-agent/load-tests/k6-smoke.js
//   BASE_URL=http://localhost:8080 CHAT=true k6 run code/autonomous-agent/load-tests/k6-smoke.js
//
// By default it exercises the lightweight, no-cost endpoints (health + agent info) under a
// ramping load so you can validate throughput, latency, and error rate without incurring
// Bedrock model cost. Set CHAT=true to also drive the real /api/chat/sync endpoint (this
// calls Amazon Bedrock and is billable), at a low rate.

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const CHAT = (__ENV.CHAT || 'false') === 'true';

export const options = {
  scenarios: {
    // Ramp light read traffic: warm up, hold, ramp down.
    light: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 25 },
        { duration: '40s', target: 25 },
        { duration: '10s', target: 0 },
      ],
      exec: 'light',
    },
    // Optional low-rate chat traffic (billable). Enabled only when CHAT=true.
    ...(CHAT
      ? {
          chat: {
            executor: 'constant-arrival-rate',
            rate: 2,
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 5,
            maxVUs: 10,
            exec: 'chat',
          },
        }
      : {}),
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{scenario:light}': ['p(95)<500'],
  },
};

export function light() {
  const health = http.get(`${BASE_URL}/actuator/health`);
  check(health, { 'health 200': (r) => r.status === 200 });

  const info = http.get(`${BASE_URL}/api/agent/info`);
  check(info, {
    'info 200': (r) => r.status === 200,
    'info has tools': (r) => (r.json('tools') || []).length > 0,
  });
  sleep(1);
}

export function chat() {
  const payload = JSON.stringify({
    prompt: 'In one short sentence, what is Amazon Bedrock?',
    sessionId: `k6-${__VU}`,
    memory: false,
  });
  const res = http.post(`${BASE_URL}/api/chat/sync`, payload, {
    headers: { 'Content-Type': 'application/json' },
    timeout: '120s',
  });
  check(res, { 'chat 200': (r) => r.status === 200 });
}

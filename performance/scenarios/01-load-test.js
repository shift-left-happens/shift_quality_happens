/**
 * LOAD TEST — "Monday Morning Handover"
 *
 * Scenario: 50 hospital staff arrive at 07:00 and check their schedules
 * simultaneously at the start of the morning shift handover. This is the
 * expected normal peak load for the system.
 *
 * VU ramp: 0 → 10 (30s) → hold 10 (3m) → 10 → 20 (30s) → hold 20 (3m) → 0 (30s)
 * Total duration: ~8 minutes
 *
 * NOTE — expected threshold failure:
 * The p(95) < 500 ms threshold will be exceeded at 20 VUs. This is intentional:
 * HikariCP is capped at maximum-pool-size=2 to simulate a constrained production
 * environment. At 20 concurrent users, DB connection queuing causes latency
 * spikes. The failure documents the bottleneck, not a bug.
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { BASE_URL, getManagerToken, authHeaders } from '../helpers/auth.js';

const overviewDuration = new Trend('req_overview_duration', true);
const shiftsDuration = new Trend('req_shifts_duration', true);

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '3m', target: 10 },
    { duration: '30s', target: 20 },
    { duration: '3m', target: 20 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  return { token: getManagerToken() };
}

export default function (data) {
  const opts = authHeaders(data.token);
  const roll = Math.random();

  if (roll < 0.40) {
    // 40% — most expensive path: 5-table JOIN view all employees see first
    const res = http.get(`${BASE_URL}/views/employee-shift-overview`, opts);
    overviewDuration.add(res.timings.duration);
    check(res, { 'overview 200': (r) => r.status === 200 });
  } else if (roll < 0.65) {
    // 25% — managers scanning the full shift board
    const res = http.get(`${BASE_URL}/shifts`, opts);
    shiftsDuration.add(res.timings.duration);
    check(res, { 'shifts 200': (r) => r.status === 200 });
  } else if (roll < 0.85) {
    // 20% — managers checking who is assigned where
    const res = http.get(`${BASE_URL}/shiftassignments`, opts);
    check(res, { 'assignments 200': (r) => r.status === 200 });
  } else {
    // 15% — HR checking pending leave
    const res = http.get(`${BASE_URL}/leaverequests`, opts);
    check(res, { 'leave 200': (r) => r.status === 200 });
  }

  sleep(1);
}

export function handleSummary(data) {
  return {
    'performance/reports/01-load-test.html': htmlReport(data),
    'performance/reports/01-load-test.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: '  ', enableColors: true }),
  };
}

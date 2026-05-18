/**
 * STRESS TEST — "The Hospital Expands: Too Many Staff, One System"
 *
 * Scenario: The hospital onboards an entire new floor of 200 nurses and
 * reassigns all existing shifts. All 200 staff check their new assignments
 * simultaneously until the system reaches — and exceeds — its limits.
 *
 * This test deliberately pushes past the HikariCP connection pool limit
 * (maximum-pool-size=2 in application.properties). Visible degradation is
 * expected at ~50 VUs; error rates climb at ~100–150 VUs as connection
 * timeout (30s) is hit.
 *
 * VU ramp: 0→20 (30s) → hold 20 (1m) → 20→50 (30s) → hold 50 (1m)
 *          → 50→100 (30s) → hold 100 (2m) → 100→150 (30s) → hold 150 (2m) → 150→0 (3m)
 * Total duration: ~12 minutes
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { BASE_URL, getManagerToken, authHeaders } from '../helpers/auth.js';

export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '1m', target: 20 },
    { duration: '30s', target: 50 },
    { duration: '1m', target: 50 },
    { duration: '30s', target: 100 },
    { duration: '2m', target: 100 },
    { duration: '30s', target: 150 },
    { duration: '2m', target: 150 },
    { duration: '3m', target: 0 },
  ],
  // Intentionally loose thresholds — this test is designed to find the breaking point
  thresholds: {
    http_req_duration: ['p(50)<2000'],
    http_req_failed: ['rate<0.30'],
  },
};

export function setup() {
  return { token: getManagerToken() };
}

export default function (data) {
  const opts = authHeaders(data.token);

  // Three sequential DB-touching requests per iteration.
  // At 150 VUs × 3 requests each, the pool of 2 connections saturates visibly.
  const overview = http.get(`${BASE_URL}/views/employee-shift-overview`, opts);
  check(overview, { 'overview ok': (r) => r.status === 200 });

  const shifts = http.get(`${BASE_URL}/shifts`, opts);
  check(shifts, { 'shifts ok': (r) => r.status === 200 });

  const assignments = http.get(`${BASE_URL}/shiftassignments`, opts);
  check(assignments, { 'assignments ok': (r) => r.status === 200 });

  sleep(0.5);
}

export function handleSummary(data) {
  return {
    'performance/reports/02-stress-test.html': htmlReport(data),
    'performance/reports/02-stress-test.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: '  ', enableColors: true }),
  };
}

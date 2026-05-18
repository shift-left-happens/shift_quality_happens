/**
 * SOAK TEST — "The Night Shift: Running Unattended for 30 Minutes"
 *
 * Scenario: The scheduling system runs overnight while automated reports
 * are generated and security staff check their schedules. No one is watching.
 * Does performance degrade as memory accumulates or connections leak?
 *
 * Each VU simulates a realistic employee session with reading pauses between
 * requests. If response times drift upward from minute 5 to minute 25 without
 * a VU count increase, it indicates a resource leak (memory or DB connection).
 *
 * Watch: HikariCP leak-detection-threshold=2000ms in application.properties
 * will log warnings in the app if connections are held longer than 2 seconds.
 * Compare those logs against this test's timeline.
 *
 * VU ramp: 0→15 (1m) → hold 15 (28m) → 15→0 (1m)
 * Total duration: 30 minutes
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { BASE_URL, getManagerToken, authHeaders } from '../helpers/auth.js';
import { SHIFT_IDS, LEAVE_REQUEST_IDS, randomFrom } from '../helpers/data.js';

const overviewDuration = new Trend('req_overview_duration', true);
const leavesDuration = new Trend('req_leaves_duration', true);

export const options = {
  stages: [
    { duration: '1m', target: 15 },
    { duration: '28m', target: 15 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<800'],
    http_req_failed: ['rate<0.02'],
    checks: ['rate>0.98'],
  },
};

export function setup() {
  return { token: getManagerToken() };
}

export default function (data) {
  const opts = authHeaders(data.token);

  // Step 1: Check own schedule (what every employee does first)
  const overview = http.get(`${BASE_URL}/views/employee-shift-overview`, opts);
  overviewDuration.add(overview.timings.duration);
  check(overview, { 'overview 200': (r) => r.status === 200 });
  sleep(Math.random() * 2 + 1); // 1–3s reading time

  // Step 2: Check leave balance/status
  const leaves = http.get(`${BASE_URL}/leaverequests`, opts);
  leavesDuration.add(leaves.timings.duration);
  check(leaves, { 'leave 200': (r) => r.status === 200 });
  sleep(Math.random() * 2 + 1);

  // Step 3: Look up a specific shift detail
  const shiftId = randomFrom(SHIFT_IDS);
  const shift = http.get(`${BASE_URL}/shifts/${shiftId}`, opts);
  check(shift, { 'shift detail 200': (r) => r.status === 200 });
  sleep(Math.random() * 2 + 1);

  // Step 4: Check assignments
  const assignments = http.get(`${BASE_URL}/shiftassignments`, opts);
  check(assignments, { 'assignments 200': (r) => r.status === 200 });
  sleep(Math.random() * 3 + 2); // 2–5s before next session loop
}

export function handleSummary(data) {
  return {
    'performance/reports/04-soak-test.html': htmlReport(data),
    'performance/reports/04-soak-test.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: '  ', enableColors: true }),
  };
}

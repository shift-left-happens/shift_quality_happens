/**
 * SPIKE TEST — "The Pilot Simulator: All Emergency Protocols at Once"
 *
 * Scenario: The manager publishes the weekend schedule at 14:00 on Friday.
 * 80 employees instantly open the app to check their assignments — all within
 * a 5-second window. The system must absorb the instant surge and then recover
 * to baseline once the rush subsides.
 *
 * Unlike the stress test (gradual ramp), this test spikes from 1 to 80 VUs
 * in 5 seconds to simulate the real-world event of a schedule publication.
 * The recovery phase (last 1 minute at 1 VU) verifies the system returns
 * to normal response times after the spike.
 *
 * VU ramp: 0→1 (1s) → hold 1 (30s) → 1→80 (5s) ← THE SPIKE
 *          → hold 80 (2m) → 80→1 (10s) → hold 1 (1m) [recovery] → 0 (5s)
 * Total duration: ~5 minutes
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { BASE_URL, getManagerToken, authHeaders } from '../helpers/auth.js';
import { SHIFT_IDS, randomFrom } from '../helpers/data.js';

const overviewDuration = new Trend('req_overview_duration', true);

export const options = {
  stages: [
    { duration: '1s', target: 1 },
    { duration: '30s', target: 1 },
    { duration: '5s', target: 80 }, // the spike
    { duration: '2m', target: 80 },
    { duration: '10s', target: 1 },
    { duration: '1m', target: 1 }, // recovery check
    { duration: '5s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],
    http_req_failed: ['rate<0.10'],
  },
};

export function setup() {
  return { token: getManagerToken() };
}

export default function (data) {
  const opts = authHeaders(data.token);
  const roll = Math.random();

  if (roll < 0.60) {
    // 60% — the schedule view every employee opens first
    const res = http.get(`${BASE_URL}/views/employee-shift-overview`, opts);
    overviewDuration.add(res.timings.duration);
    check(res, { 'overview 200': (r) => r.status === 200 });
  } else {
    // 40% — individual shift detail lookup
    const shiftId = randomFrom(SHIFT_IDS);
    const res = http.get(`${BASE_URL}/shifts/${shiftId}`, opts);
    check(res, { 'shift detail 200': (r) => r.status === 200 });
  }

  sleep(0.5);
}

export function handleSummary(data) {
  return {
    'performance/reports/03-spike-test.html': htmlReport(data),
    'performance/reports/03-spike-test.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: '  ', enableColors: true }),
  };
}

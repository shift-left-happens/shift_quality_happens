/**
 * SMOKE TEST — "Is Anyone Home?"
 *
 * 1 VU, 1 iteration. Zero failures allowed.
 * Run after deploy or `make reset` to confirm the app is up and the two
 * most-used endpoints respond correctly before running heavier tests.
 */

import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, getManagerToken, authHeaders } from '../helpers/auth.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_failed: ['rate==0'],
    checks: ['rate==1'],
  },
};

export function setup() {
  return { token: getManagerToken() };
}

export default function (data) {
  const opts = authHeaders(data.token);

  check(http.get(`${BASE_URL}/views/employee-shift-overview`, opts), {
    'overview 200': (r) => r.status === 200,
  });

  check(http.get(`${BASE_URL}/shifts`, opts), {
    'shifts 200': (r) => r.status === 200,
  });
}

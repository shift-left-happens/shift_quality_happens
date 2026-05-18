import http from 'k6/http';
import { check } from 'k6';

export const BASE_URL = 'http://localhost:8081';

function login(email, password) {
  const res = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email, password }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(res, { 'login 200': (r) => r.status === 200 });
  return res.json('token');
}

// Call these in setup() only — BCrypt cost factor 10 makes per-VU login prohibitively expensive
export function getEmployeeToken() {
  return login('sofie.jensen@hospital.dk', 'password123');
}

export function getManagerToken() {
  return login('malthe.enevoldsen@hospital.dk', 'password123');
}

export function getAdminToken() {
  return login('admin@shift.dk', 'password123');
}

export function authHeaders(token) {
  return {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  };
}

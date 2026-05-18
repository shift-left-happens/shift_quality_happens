/**
 * Shift — API integration tests
 *
 * These tests talk directly to the Spring Boot backend using Playwright's
 * request fixture. They validate the HTTP contract for /shifts.
 *
 * The validation cases are blackbox-derived from "Software Quality exam -
 * Shift Happens" §"Shift Duration & Timing" and §"Shift Status Transitions",
 * mirrored from ShiftServiceTest on the backend.
 *
 * Fully self-contained: every shift (and the one test employee) is created
 * and deleted by the suite itself. The only seed assumption is a running
 * admin account (admin@shift.dk).
 */

import { test, expect, type APIRequestContext } from '@playwright/test';
import { API_URL, loginAndGetToken, authHeaders, fmt, shiftWindow } from '../pages/helper/api-helpers';

const ADMIN_EMAIL = process.env.TEST_ADMIN_EMAIL || 'admin@shift.dk';
const TEST_EMPLOYEE_PASSWORD = process.env.TEST_EMPLOYEE_PASSWORD || 'TestPass123';

test.describe.serial('Shift API', () => {
  let adminToken: string;
  let employeeToken: string;
  let departmentId: number;
  let workLocationId: number;
  let employeeId: number;

  const shiftIds: number[] = [];

  // Builds a valid shift payload; spread an override object to make it invalid.
  function validShiftPayload(overrides: Record<string, unknown> = {}) {
    return {
      departmentId,
      workLocationId,
      shiftName: `API Test Shift ${Date.now().toString(36)}`,
      ...shiftWindow(7, 8 * 60),
      shiftStatus: 'Open',
      ...overrides,
    };
  }

  async function createShift(
    request: APIRequestContext,
    token: string,
    overrides: Record<string, unknown> = {},
  ) {
    const res = await request.post(`${API_URL}/shifts`, {
      headers: authHeaders(token),
      data: validShiftPayload(overrides),
    });
    expect(res.status(), 'Create shift should return 201').toBe(201);
    const body = await res.json();
    shiftIds.push(body.shiftId as number);
    return body;
  }

  test.beforeAll(async ({ request }) => {
    const adminLogin = await loginAndGetToken(request, ADMIN_EMAIL);
    adminToken = adminLogin.token;
    expect(
      ['Administrator', 'Manager'],
      `${ADMIN_EMAIL} must be Administrator or Manager`,
    ).toContain(adminLogin.roleName ?? '');

    // Pick real department + work location so reference validation passes.
    const deptRes = await request.get(`${API_URL}/departments`, {
      headers: authHeaders(adminToken),
    });
    expect(deptRes.status()).toBe(200);
    departmentId = (await deptRes.json())[0].departmentId as number;

    const locRes = await request.get(`${API_URL}/worklocations`, {
      headers: authHeaders(adminToken),
    });
    expect(locRes.status()).toBe(200);
    workLocationId = (await locRes.json())[0].workLocationId as number;

    // Create one regular employee to exercise role-based authorization.
    const suffix = Date.now().toString(36);
    const empRes = await request.post(`${API_URL}/employees`, {
      headers: authHeaders(adminToken),
      data: {
        employeeNumber: `EMP-SH-${suffix}`,
        firstName: 'Shift',
        lastName: 'Tester',
        userRole: 'Employee',
        email: `shift.tester.${suffix}@shifthappens.dk`,
        loginPassword: TEST_EMPLOYEE_PASSWORD,
        phoneNumber: '12345678',
        hireDate: '2026-01-15',
        birthDate: '2000-01-01',
        employmentStatus: 'ACTIVE',
        primaryWorkLocationId: workLocationId,
      },
    });
    expect(empRes.status(), 'Create test employee').toBe(201);
    const emp = await empRes.json();
    employeeId = emp.employeeId as number;
    employeeToken = (
      await loginAndGetToken(request, emp.email, TEST_EMPLOYEE_PASSWORD)
    ).token;
  });

  test.afterAll(async ({ request }) => {
    if (!adminToken) return;
    const deleteIfExists = async (url: string) => {
      const res = await request.delete(url, { headers: authHeaders(adminToken) });
      // 204 = deleted, 404 = already gone, 400 = dependency during teardown.
      expect([204, 400, 404]).toContain(res.status());
    };
    for (const id of [...shiftIds].reverse()) {
      await deleteIfExists(`${API_URL}/shifts/${id}`);
    }
    if (employeeId) {
      await deleteIfExists(`${API_URL}/employees/${employeeId}`);
    }
  });

  // ── SH-API-01 — Authentication ────────────────────────────────────────────

  test('SH-API-01 — unauthenticated GET /shifts returns 401', async ({ request }) => {
    const res = await request.get(`${API_URL}/shifts`);
    expect(res.status()).toBe(401);
  });

  test('SH-API-01 — unauthenticated POST /shifts returns 401', async ({ request }) => {
    const res = await request.post(`${API_URL}/shifts`, { data: validShiftPayload() });
    expect(res.status()).toBe(401);
  });

  // ── SH-API-02 — Authorization (role) ──────────────────────────────────────

  test('SH-API-02 — authenticated employee can list shifts', async ({ request }) => {
    const res = await request.get(`${API_URL}/shifts`, {
      headers: authHeaders(employeeToken),
    });
    expect(res.status()).toBe(200);
    expect(Array.isArray(await res.json())).toBe(true);
  });

  test('SH-API-02 — employee cannot create a shift (403)', async ({ request }) => {
    const res = await request.post(`${API_URL}/shifts`, {
      headers: authHeaders(employeeToken),
      data: validShiftPayload(),
    });
    expect(res.status()).toBe(403);
  });

  // ── SH-API-03 — Create + read ─────────────────────────────────────────────

  test('SH-API-03 — admin creates a valid shift (FR-SH-01)', async ({ request }) => {
    const shift = await createShift(request, adminToken);
    expect(shift).toHaveProperty('shiftId');
    expect(shift.shiftStatus).toBe('Open');
  });

  test('SH-API-03 — created shift can be fetched by id', async ({ request }) => {
    const shift = await createShift(request, adminToken, { shiftName: 'Fetch Me' });
    const res = await request.get(`${API_URL}/shifts/${shift.shiftId}`, {
      headers: authHeaders(adminToken),
    });
    expect(res.status()).toBe(200);
    expect((await res.json()).shiftName).toBe('Fetch Me');
  });

  test('SH-API-03 — GET unknown shift id returns 404', async ({ request }) => {
    const res = await request.get(`${API_URL}/shifts/99999999`, {
      headers: authHeaders(adminToken),
    });
    expect(res.status()).toBe(404);
  });

  // ── SH-API-04 — Update + cancel + delete lifecycle ────────────────────────

  test('SH-API-04 — shift can be updated (FR-SH-02)', async ({ request }) => {
    const shift = await createShift(request, adminToken);
    const res = await request.put(`${API_URL}/shifts/${shift.shiftId}`, {
      headers: authHeaders(adminToken),
      data: { ...validShiftPayload(), shiftName: 'Updated Name' },
    });
    expect(res.status()).toBe(200);
    expect((await res.json()).shiftName).toBe('Updated Name');
  });

  test('SH-API-04 — shift can be cancelled (FR-SH-06)', async ({ request }) => {
    const shift = await createShift(request, adminToken);
    const res = await request.post(`${API_URL}/shifts/${shift.shiftId}/cancel`, {
      headers: authHeaders(adminToken),
    });
    expect(res.status()).toBe(200);
    expect((await res.json()).shiftStatus).toBe('Cancelled');
  });

  test('SH-API-04 — future shift can be deleted (FR-SH-03)', async ({ request }) => {
    const shift = await createShift(request, adminToken);
    const res = await request.delete(`${API_URL}/shifts/${shift.shiftId}`, {
      headers: authHeaders(adminToken),
    });
    expect(res.status()).toBe(204);
    // already gone — drop from the cleanup list
    shiftIds.splice(shiftIds.indexOf(shift.shiftId), 1);
  });

  // ── SH-API-05 — Validation rejections (400) ───────────────────────────────
  // Each case maps to a branch of ShiftService.validate(); see ShiftServiceTest.

  const w = shiftWindow(7, 8 * 60);
  const invalidCases: { name: string; overrides: Record<string, unknown> }[] = [
    {
      name: 'end time equal to start time',
      overrides: { startDatetime: w.startDatetime, endDatetime: w.startDatetime },
    },
    {
      name: 'end time before start time',
      overrides: { startDatetime: w.endDatetime, endDatetime: w.startDatetime },
    },
    { name: 'duration below 60 minutes', overrides: shiftWindow(7, 30) },
    { name: 'duration above 12 hours', overrides: shiftWindow(7, 13 * 60) },
    { name: 'blank shift name', overrides: { shiftName: '   ' } },
    { name: 'shift name over 100 characters', overrides: { shiftName: 'a'.repeat(101) } },
    { name: 'shift name with unsafe content', overrides: { shiftName: 'Late <script>' } },
    { name: 'unknown shift status', overrides: { shiftStatus: 'SCHEDULED' } },
    { name: 'missing department', overrides: { departmentId: null } },
    { name: 'non-existent department', overrides: { departmentId: 99999999 } },
  ];

  for (const { name, overrides } of invalidCases) {
    test(`SH-API-05 — rejects ${name} with 400`, async ({ request }) => {
      const res = await request.post(`${API_URL}/shifts`, {
        headers: authHeaders(adminToken),
        data: validShiftPayload(overrides),
      });
      expect(res.status(), `${name} should be rejected`).toBe(400);
    });
  }
});

/**
 * Shift — API integration tests
 *
 * These tests talk directly to the Spring Boot backend using Playwright's
 * request fixture. They validate the HTTP contract for /shifts.
 *
 * SH-API-03 walks the full create → read → update → cancel → delete lifecycle
 * as one flow. The remaining tests are independent guards: authentication,
 * role authorization, a not-found read, and the blackbox-derived validation
 * rejections.
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
import { API_URL, loginAndGetToken, authHeaders, shiftWindow } from '../pages/helper/api-helpers';

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
    expect(adminLogin.roleName).toBe('Administrator');

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
      expect(res.status()).toBe(204);
    };
    //Delete all shifts in reverse order, so that the first shift is deleted first.
    for (const id of [...shiftIds].reverse()) {
      await deleteIfExists(`${API_URL}/shifts/${id}`);
    }
    //Delete the test employee, if it exists.
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

  // ── SH-API-03 — Create → read → update → cancel → delete lifecycle ────────

  test('SH-API-03 — admin walks a shift through its full lifecycle (FR-SH-01, 02, 03, 06)', async ({
    request,
  }) => {
    // 1. Create a valid shift
    const created = await createShift(request, adminToken);
    expect(created).toHaveProperty('shiftId');
    expect(created.shiftStatus).toBe('Open');
    const shiftId = created.shiftId as number;

    // 2. Read it back by id
    const getRes = await request.get(`${API_URL}/shifts/${shiftId}`, {
      headers: authHeaders(adminToken),
    });
    expect(getRes.status()).toBe(200);
    expect((await getRes.json()).shiftId).toBe(shiftId);

    // 3. Update it (FR-SH-02)
    const updateRes = await request.put(`${API_URL}/shifts/${shiftId}`, {
      headers: authHeaders(adminToken),
      data: { ...validShiftPayload(), shiftName: 'Updated Name' },
    });
    expect(updateRes.status()).toBe(200);
    expect((await updateRes.json()).shiftName).toBe('Updated Name');

    // 4. Cancel it (FR-SH-06)
    const cancelRes = await request.post(`${API_URL}/shifts/${shiftId}/cancel`, {
      headers: authHeaders(adminToken),
    });
    expect(cancelRes.status()).toBe(200);
    expect((await cancelRes.json()).shiftStatus).toBe('Cancelled');

    // 5. Delete it — still a future shift, so deletion is allowed (FR-SH-03)
    const deleteRes = await request.delete(`${API_URL}/shifts/${shiftId}`, {
      headers: authHeaders(adminToken),
    });
    expect(deleteRes.status()).toBe(204);
    shiftIds.splice(shiftIds.indexOf(shiftId), 1);

    // 6. It is gone
    const goneRes = await request.get(`${API_URL}/shifts/${shiftId}`, {
      headers: authHeaders(adminToken),
    });
    expect(goneRes.status()).toBe(404);
  });

  test('SH-API-03 — GET unknown shift id returns 404', async ({ request }) => {
    const res = await request.get(`${API_URL}/shifts/99999999`, {
      headers: authHeaders(adminToken),
    });
    expect(res.status()).toBe(404);
  });

  // ── SH-API-04 — Validation rejections (400) ───────────────────────────────
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
    test(`SH-API-04 — rejects ${name} with 400`, async ({ request }) => {
      const res = await request.post(`${API_URL}/shifts`, {
        headers: authHeaders(adminToken),
        data: validShiftPayload(overrides),
      });
      expect(res.status(), `${name} should be rejected`).toBe(400);
    });
  }
});

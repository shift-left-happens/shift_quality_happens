/**
 * Shift Swap — API integration tests
 *
 * These tests talk directly to the Spring Boot backend using Playwright's
 * request fixture. They validate the HTTP contract for /shiftswaps.
 *
 * Fully self-contained: all employees, shifts and assignments are created
 * and deleted by the suite itself. The only seed assumption is a running
 * admin account (admin@shift.dk).
 */

import { test, expect, type APIRequestContext } from '@playwright/test';

const API_URL = process.env.API_URL || 'http://localhost:8080';
const ADMIN_EMAIL = process.env.TEST_ADMIN_EMAIL || 'admin@shift.dk';
const ADMIN_PASSWORD = process.env.TEST_USER_PASSWORD || 'password123';
const TEST_EMPLOYEE_PASSWORD = process.env.TEST_EMPLOYEE_PASSWORD || 'TestPass123';

async function loginAndGetToken(
  request: APIRequestContext,
  email: string,
  password: string = ADMIN_PASSWORD,
): Promise<{ token: string; roleName?: string }> {
  const response = await request.post(`${API_URL}/auth/login`, {
    data: { email, password },
  });
  expect(response.status(), `Expected successful login for ${email}`).toBe(200);
  const body = await response.json();
  expect(body).toHaveProperty('token');
  expect(typeof body.token).toBe('string');
  return { token: body.token as string, roleName: body.roleName as string | undefined };
}

function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}` };
}

function futureShiftWindow(daysFromNow: number = 7) {
  const start = new Date(Date.now() + daysFromNow * 24 * 60 * 60 * 1000);
  const end = new Date(start.getTime() + 8 * 60 * 60 * 1000);
  const fmt = (d: Date) => d.toISOString().slice(0, 19);
  return { startDatetime: fmt(start), endDatetime: fmt(end) };
}

// The outer describe is serial so the nested lifecycle group and SS-06 group
// do not race over the same shared assignment.
test.describe.serial('Shift Swap API', () => {
  let adminToken: string;
  let empOwnerToken: string;
  let empNonOwnerToken: string;
  let empOwnerId: number;
  let empTargetId: number;
  // Three separate assignments so each group never shares one.
  let assignmentLifecycleId: number;
  let assignmentSS06Id: number;
  let assignmentSS11Id: number;
  const swapsToCleanup = new Set<number>();

  // Resource IDs to delete in afterAll
  const shiftIds: number[] = [];
  const assignmentIds: number[] = [];
  const employeeIds: number[] = [];

  test.beforeAll(async ({ request }) => {
    const adminLogin = await loginAndGetToken(request, ADMIN_EMAIL);
    adminToken = adminLogin.token;
    expect(
      ['Administrator', 'Manager'],
      `User ${ADMIN_EMAIL} must be Administrator or Manager to create employees`,
    ).toContain(adminLogin.roleName ?? '');

    const suffix = Date.now().toString(36);
    const fmt = (d: Date) => d.toISOString().slice(0, 19);
    const pickDepartmentId = (offset: number) => 1 + ((Date.now() + offset) % 20);
    const pickWorkLocationId = (offset: number) => 1 + ((Date.now() + offset) % 10);

    // --- helpers ---
    const createEmployeeFromPayload = async (
      label: string,
      payload: {
        employeeNumber: string;
        firstName: string;
        lastName: string;
        userRole: 'Employee';
        email: string;
        loginPassword: string;
        phoneNumber: string;
        hireDate: string;
        birthDate: string;
        employmentStatus: 'ACTIVE';
        primaryWorkLocationId: number;
      },
    ) => {
      const res = await request.post(`${API_URL}/employees`, {
        headers: authHeaders(adminToken),
        data: payload,
      });
      expect(res.status(), `Create employee ${label}`).toBe(201);
      const id = (await res.json()).employeeId as number;
      employeeIds.push(id);
      return { id, email: payload.email };
    };

    const createShift = async (label: string, daysFromNow: number) => {
      const { startDatetime, endDatetime } = futureShiftWindow(daysFromNow);
      const idOffset = label.length;
      const res = await request.post(`${API_URL}/shifts`, {
        headers: authHeaders(adminToken),
        data: {
          departmentId: pickDepartmentId(idOffset),
          workLocationId: pickWorkLocationId(idOffset),
          shiftName: `Vagt ${label} ${suffix}`,
          startDatetime,
          endDatetime,
          shiftStatus: 'Open',
        },
      });
      expect(res.status(), `Create shift ${label}`).toBe(201);
      const id = (await res.json()).shiftId as number;
      shiftIds.push(id);
      return id;
    };

    const createAssignmentFromPayload = async (
      label: string,
      payload: {
        shiftId: number;
        employeeId: number;
        assignmentStatus: 'Assigned';
        assignedDatetime: string;
      },
    ) => {
      const res = await request.post(`${API_URL}/shiftassignments`, {
        headers: authHeaders(adminToken),
        data: payload,
      });
      expect(res.status(), `Create assignment ${label}`).toBe(201);
      const id = (await res.json()).shiftAssignmentId as number;
      assignmentIds.push(id);
      return id;
    };

    // --- Create 3 manual employees (same structure as validated payload) ---
    const empOwner = await createEmployeeFromPayload('owner', {
      employeeNumber: `EMP-1004-${suffix}`,
      firstName: 'Johnny',
      lastName: 'Doe',
      userRole: 'Employee',
      email: `johnny.doe.${suffix}@shifthappens.dk`,
      loginPassword: TEST_EMPLOYEE_PASSWORD,
      phoneNumber: '+45 12345678',
      hireDate: '2026-01-15',
      birthDate: '2000-01-01',
      employmentStatus: 'ACTIVE',
      primaryWorkLocationId: 1,
    });

    const empNonOwner = await createEmployeeFromPayload('nonowner', {
      employeeNumber: `EMP-1005-${suffix}`,
      firstName: 'Mia',
      lastName: 'Hansen',
      userRole: 'Employee',
      email: `mia.hansen.${suffix}@shifthappens.dk`,
      loginPassword: TEST_EMPLOYEE_PASSWORD,
      phoneNumber: '+45 23456789',
      hireDate: '2026-01-15',
      birthDate: '1999-05-11',
      employmentStatus: 'ACTIVE',
      primaryWorkLocationId: 1,
    });

    const empTarget = await createEmployeeFromPayload('target', {
      employeeNumber: `EMP-1006-${suffix}`,
      firstName: 'Ali',
      lastName: 'Nielsen',
      userRole: 'Employee',
      email: `ali.nielsen.${suffix}@shifthappens.dk`,
      loginPassword: TEST_EMPLOYEE_PASSWORD,
      phoneNumber: '+45 34567890',
      hireDate: '2026-01-15',
      birthDate: '1998-08-20',
      employmentStatus: 'ACTIVE',
      primaryWorkLocationId: 1,
    });
    empOwnerId = empOwner.id;
    empTargetId = empTarget.id;

    // --- Create shifts and assignments ---
    const shiftLifecycle = await createShift('Lifecycle', 7);
    const shiftSS06 = await createShift('SS06', 9);
    const shiftSS11 = await createShift('SS11', 11);

    assignmentLifecycleId = await createAssignmentFromPayload('lifecycle', {
      shiftId: shiftLifecycle,
      employeeId: empOwnerId,
      assignmentStatus: 'Assigned',
      assignedDatetime: fmt(new Date()),
    });
    assignmentSS06Id = await createAssignmentFromPayload('ss06', {
      shiftId: shiftSS06,
      employeeId: empOwnerId,
      assignmentStatus: 'Assigned',
      assignedDatetime: fmt(new Date()),
    });
    assignmentSS11Id = await createAssignmentFromPayload('ss11', {
      shiftId: shiftSS11,
      employeeId: empOwnerId,
      assignmentStatus: 'Assigned',
      assignedDatetime: fmt(new Date()),
    });

    // --- Log in as test employees ---
    empOwnerToken = (await loginAndGetToken(request, empOwner.email, TEST_EMPLOYEE_PASSWORD)).token;
    empNonOwnerToken = (await loginAndGetToken(request, empNonOwner.email, TEST_EMPLOYEE_PASSWORD)).token;
  });

  test.afterAll(async ({ request }) => {
    if (!adminToken) {
      return;
    }
    // Re-authenticate right before teardown to avoid token-expiry cleanup failures.
    const cleanupAdminToken = (await loginAndGetToken(request, ADMIN_EMAIL)).token;

    const deleteSwapIfExists = async (swapId: number) => {
      // Pending swaps often must be cancelled before delete. Non-pending may return 400 here.
      const cancelRes = await request.post(`${API_URL}/shiftswaps/${swapId}/cancel`, {
        headers: authHeaders(cleanupAdminToken),
      });
      expect([200, 400, 404]).toContain(cancelRes.status());

      const deleteRes = await request.delete(`${API_URL}/shiftswaps/${swapId}`, {
        headers: authHeaders(cleanupAdminToken),
      });
      // 204 = deleted, 404 = already gone.
      expect([204, 404]).toContain(deleteRes.status());
    };

    const deleteIfExists = async (url: string) => {
      const res = await request.delete(url, { headers: authHeaders(cleanupAdminToken) });
      // 204 = deleted, 404 = already gone. Any other status means cleanup did not complete.
      expect([204, 404]).toContain(res.status());
    };

    // Delete swaps first to release FK references to assignments and employees.
    for (const swapId of Array.from(swapsToCleanup).reverse()) {
      await deleteSwapIfExists(swapId);
    }

    // Delete in reverse dependency order: assignments → shifts → employees
    for (const id of [...assignmentIds].reverse()) {
      await deleteIfExists(`${API_URL}/shiftassignments/${id}`);
    }
    for (const id of [...shiftIds].reverse()) {
      await deleteIfExists(`${API_URL}/shifts/${id}`);
    }
    for (const id of [...employeeIds].reverse()) {
      await deleteIfExists(`${API_URL}/employees/${id}`);
    }
  });

  // ── BR-API-SS-01 ──────────────────────────────────────────────────────────

  test('BR-API-SS-01 — unauthenticated GET /shiftswaps returns 401', async ({ request }) => {
    const response = await request.get(`${API_URL}/shiftswaps`);
    expect(response.status()).toBe(401);
  });

  test('BR-API-SS-01 — unauthenticated POST /shiftswaps returns 401', async ({ request }) => {
    const response = await request.post(`${API_URL}/shiftswaps`, {
      data: {
        originalShiftAssignmentId: assignmentLifecycleId,
        employeeFromId: empOwnerId,
        employeeToId: empTargetId,
        requestDatetime: new Date(Date.now() - 60_000).toISOString().slice(0, 19),
        reason: 'Test swap',
      },
    });
    expect(response.status()).toBe(401);
  });

  // ── BR-API-SS-02 ──────────────────────────────────────────────────────────

  test('BR-API-SS-02 — authenticated employee can list swaps', async ({ request }) => {
    const response = await request.get(`${API_URL}/shiftswaps`, {
      headers: authHeaders(empOwnerToken),
    });
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(Array.isArray(body)).toBe(true);
  });

  // ── BR-API-SS-03 / 04 / 05 — Swap lifecycle ───────────────────────────────

  test.describe.serial('Swap lifecycle (create → duplicate → cancel)', () => {
    let createdSwapId: number;

    test('BR-API-SS-03 — valid swap is created with status Pending (FR-SW-02)', async ({ request }) => {
      const response = await request.post(`${API_URL}/shiftswaps`, {
        headers: authHeaders(empOwnerToken),
        data: {
          originalShiftAssignmentId: assignmentLifecycleId,
          employeeFromId: empOwnerId,
          employeeToId: empTargetId,
          requestDatetime: new Date(Date.now() - 60_000).toISOString().slice(0, 19),
          reason: 'Playwright API test swap',
        },
      });

      expect(response.status()).toBe(201);
      const body = await response.json();
      expect(body).toHaveProperty('shiftSwapId');
      expect(body.swapStatus).toMatch(/pending/i);
      expect(body.employeeFromId).toBe(empOwnerId);
      expect(body.employeeToId).toBe(empTargetId);

      createdSwapId = body.shiftSwapId as number;
      swapsToCleanup.add(createdSwapId);
    });

    test('BR-API-SS-04 — duplicate pending swap for same assignment returns 400 (FR-SW-07)', async ({ request }) => {
      const response = await request.post(`${API_URL}/shiftswaps`, {
        headers: authHeaders(empOwnerToken),
        data: {
          originalShiftAssignmentId: assignmentLifecycleId,
          employeeFromId: empOwnerId,
          employeeToId: empTargetId,
          requestDatetime: new Date(Date.now() - 60_000).toISOString().slice(0, 19),
          reason: 'Duplicate swap attempt',
        },
      });
      expect([400]).toContain(response.status());
    });

    test('BR-API-SS-05 — requester can cancel their own pending swap', async ({ request }) => {
      expect(createdSwapId).toBeDefined();

      const response = await request.post(`${API_URL}/shiftswaps/${createdSwapId}/cancel`, {
        headers: authHeaders(empOwnerToken),
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.swapStatus).toMatch(/cancelled/i);
      // Keep swap id for teardown: cancelled swaps still hold FK references until deleted.
    });
  });

  // ── BR-API-SS-06 — non-owner cannot cancel ────────────────────────────────

  test.describe.serial('BR-API-SS-06 — non-owner cannot cancel another employee\'s swap', () => {
    let swapId: number;

    test.beforeAll(async ({ request }) => {
      // Owner creates a swap on the dedicated SS-06 assignment
      const swapRes = await request.post(`${API_URL}/shiftswaps`, {
        headers: authHeaders(empOwnerToken),
        data: {
          originalShiftAssignmentId: assignmentSS06Id,
          employeeFromId: empOwnerId,
          employeeToId: empTargetId,
          requestDatetime: new Date(Date.now() - 60_000).toISOString().slice(0, 19),
          reason: 'Non-owner cancel test',
        },
      });
      expect(swapRes.status()).toBe(201);
      swapId = (await swapRes.json()).shiftSwapId as number;
      swapsToCleanup.add(swapId);
    });

    test('non-owner cannot cancel another employee\'s swap — returns 400', async ({ request }) => {
      const response = await request.post(`${API_URL}/shiftswaps/${swapId}/cancel`, {
        headers: authHeaders(empNonOwnerToken),
      });
      expect(response.status()).toBe(400);
    });
  });
  test('BR-API-SS-11 — cancelling already cancelled swap returns 400', async ({ request }) => {
    const createResponse = await request.post(`${API_URL}/shiftswaps`, {
      headers: authHeaders(empOwnerToken),
      data: {
        originalShiftAssignmentId: assignmentSS11Id,
        employeeFromId: empOwnerId,
        employeeToId: empTargetId,
        requestDatetime: new Date(Date.now() - 60_000).toISOString().slice(0, 19),
        reason: 'Cancel twice test',
      },
    });

    expect(createResponse.status()).toBe(201);
    const swapId = (await createResponse.json()).shiftSwapId as number;
    swapsToCleanup.add(swapId);

    const firstCancel = await request.post(`${API_URL}/shiftswaps/${swapId}/cancel`, {
      headers: authHeaders(empOwnerToken),
    });
    expect(firstCancel.status()).toBe(200);

    const secondCancel = await request.post(`${API_URL}/shiftswaps/${swapId}/cancel`, {
      headers: authHeaders(empOwnerToken),
    });
    expect(secondCancel.status()).toBe(400);
  });

  test('BR-API-SS-07 — non-owner cannot create swap for another employee assignment', async ({ request }) => {
    const response = await request.post(`${API_URL}/shiftswaps`, {
      headers: authHeaders(empNonOwnerToken),
      data: {
        originalShiftAssignmentId: assignmentLifecycleId,
        employeeFromId: empOwnerId,
        employeeToId: empTargetId,
        requestDatetime: new Date(Date.now() - 60_000).toISOString().slice(0, 19),
        reason: 'Non-owner create attempt',
      },
    });
    expect([403]).toContain(response.status());
    
  });
  test('BR-API-SS-08 — creating swap with non-existing assignment returns 400 or 404', async ({ request }) => {
    const response = await request.post(`${API_URL}/shiftswaps`, {
      headers: authHeaders(empOwnerToken),
      data: {
        originalShiftAssignmentId: 999999999,
        employeeFromId: empOwnerId,
        employeeToId: empTargetId,
        requestDatetime: new Date(Date.now() - 60_000).toISOString().slice(0, 19),
        reason: 'Invalid assignment test',
      },
    });

    expect([400, 404]).toContain(response.status());
  });
  test('BR-API-SS-09 — missing originalShiftAssignmentId returns 400', async ({ request }) => {
    const response = await request.post(`${API_URL}/shiftswaps`, {
      headers: authHeaders(empOwnerToken),
      data: {
        employeeFromId: empOwnerId,
        employeeToId: empTargetId,
        requestDatetime: new Date(Date.now() - 60_000).toISOString().slice(0, 19),
        reason: 'Missing assignment id',
      },
    });

    expect(response.status()).toBe(400);
  });
  test('BR-API-SS-10 — employee cannot request swap with themselves', async ({ request }) => {
    const response = await request.post(`${API_URL}/shiftswaps`, {
      headers: authHeaders(empOwnerToken),
      data: {
        originalShiftAssignmentId: assignmentLifecycleId,
        employeeFromId: empOwnerId,
        employeeToId: empOwnerId,
        requestDatetime: new Date(Date.now() - 60_000).toISOString().slice(0, 19),
        reason: 'Swap with myself',
      },
    });

    expect(response.status()).toBe(400);
  });
});

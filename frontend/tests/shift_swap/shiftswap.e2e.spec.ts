/**
 * Shift Swap — End-to-End tests
 *
 * The frontend does not yet have a dedicated shift swap form page, so this
 * spec validates browser-authenticated API calls from a real app session.
 *
 * This suite is self-contained: it creates and cleans up its own employees,
 * shifts, assignments and swaps.
 */

import { test, expect } from '@playwright/test';
import type { APIRequestContext, Page } from '@playwright/test';

const API_URL = process.env.API_URL || 'http://localhost:8080';
const ADMIN_EMAIL = process.env.TEST_ADMIN_EMAIL || 'admin@shift.dk';
const ADMIN_PASSWORD = process.env.TEST_USER_PASSWORD || 'password123';
const TEST_EMPLOYEE_PASSWORD = process.env.TEST_EMPLOYEE_PASSWORD || 'TestPass123';

type LoginResponse = {
  token: string;
  employeeId: number;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  roleName: string;
};

function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}` };
}

async function login(
  request: APIRequestContext,
  email: string,
  password: string = ADMIN_PASSWORD,
): Promise<LoginResponse> {
  const res = await request.post(`${API_URL}/auth/login`, {
    data: { email, password },
  });
  expect(res.status(), `Expected successful login for ${email}`).toBe(200);
  return (await res.json()) as LoginResponse;
}

async function seedAuthState(page: Page, session: LoginResponse) {
  // Seed on an actual app-origin page to keep behavior deterministic in CI browsers.
  await page.goto('/login');
  await page.evaluate(
    ({ token, user }) => {
      localStorage.setItem('shift_happens_token', token);
      localStorage.setItem('shift_happens_user', JSON.stringify(user));
    },
    {
      token: session.token,
      user: {
        employeeId: session.employeeId,
        employeeNumber: session.employeeNumber,
        firstName: session.firstName,
        lastName: session.lastName,
        email: session.email,
        role: session.roleName,
      },
    },
  );
}

function futureShiftWindow(daysFromNow: number) {
  const start = new Date(Date.now() + daysFromNow * 24 * 60 * 60 * 1000);
  const end = new Date(start.getTime() + 8 * 60 * 60 * 1000);
  const fmt = (d: Date) => d.toISOString().slice(0, 19);
  return { startDatetime: fmt(start), endDatetime: fmt(end) };
}

// ── Tests ──────────────────────────────────────────────────────────────────────

test.describe('Shift Swap E2E', () => {
  let adminToken = '';
  let ownerSession: LoginResponse;
  let nonOwnerSession: LoginResponse;
  let targetEmployeeId = 0;
  let assignmentId = 0;

  const employeeIds: number[] = [];
  const shiftIds: number[] = [];
  const assignmentIds: number[] = [];
  const swapsToCleanup = new Set<number>();

  test.beforeAll(async ({ request }) => {
    const adminLogin = await login(request, ADMIN_EMAIL);
    adminToken = adminLogin.token;
    expect(['Administrator', 'Manager']).toContain(adminLogin.roleName);

    const suffix = Date.now().toString(36);
    const fmt = (d: Date) => d.toISOString().slice(0, 19);

    const createEmployee = async (
      label: string,
      firstName: string,
      lastName: string,
    ): Promise<{ id: number; email: string }> => {
      const payload = {
        employeeNumber: `EMP-E2E-${label}-${suffix}`,
        firstName,
        lastName,
        userRole: 'Employee' as const,
        email: `e2e.${label}.${suffix}@shifthappens.dk`,
        loginPassword: TEST_EMPLOYEE_PASSWORD,
        phoneNumber: '+4512345678',
        hireDate: '2026-01-15',
        birthDate: '2000-01-01',
        employmentStatus: 'ACTIVE' as const,
        primaryWorkLocationId: 1,
      };
      const res = await request.post(`${API_URL}/employees`, {
        headers: authHeaders(adminToken),
        data: payload,
      });
      expect(res.status(), `Create employee ${label}`).toBe(201);
      const body = await res.json();
      employeeIds.push(body.employeeId as number);
      return { id: body.employeeId as number, email: payload.email };
    };

    const createShift = async (label: string, daysFromNow: number): Promise<number> => {
      const { startDatetime, endDatetime } = futureShiftWindow(daysFromNow);
      const res = await request.post(`${API_URL}/shifts`, {
        headers: authHeaders(adminToken),
        data: {
          departmentId: 1,
          workLocationId: 1,
          shiftName: `E2E ${label} ${suffix}`,
          startDatetime,
          endDatetime,
          shiftStatus: 'Open',
        },
      });
      expect(res.status(), `Create shift ${label}`).toBe(201);
      const body = await res.json();
      shiftIds.push(body.shiftId as number);
      return body.shiftId as number;
    };

    const createAssignment = async (shiftId: number, employeeId: number): Promise<number> => {
      const res = await request.post(`${API_URL}/shiftassignments`, {
        headers: authHeaders(adminToken),
        data: {
          shiftId,
          employeeId,
          assignmentStatus: 'Assigned',
          assignedDatetime: fmt(new Date()),
        },
      });
      expect(res.status(), `Create assignment for shift ${shiftId}`).toBe(201);
      const body = await res.json();
      assignmentIds.push(body.shiftAssignmentId as number);
      return body.shiftAssignmentId as number;
    };

    const owner = await createEmployee('owner', 'Sofie', 'Jensen');
    await createEmployee('nonowner', 'Mads', 'Nielsen');
    const target = await createEmployee('target', 'Malthe', 'Enevoldsen');
    targetEmployeeId = target.id;

    const shiftId = await createShift('Swap', 7);
    assignmentId = await createAssignment(shiftId, owner.id);

    ownerSession = await login(request, owner.email, TEST_EMPLOYEE_PASSWORD);
    nonOwnerSession = await login(request, `e2e.nonowner.${suffix}@shifthappens.dk`, TEST_EMPLOYEE_PASSWORD);
  });

  test.afterAll(async ({ request }) => {
    // Re-authenticate right before teardown to avoid token-expiry cleanup failures.
    let cleanupAdminToken = (await login(request, ADMIN_EMAIL)).token;

    const adminDeleteWithRetry = async (url: string) => {
      let res = await request.delete(url, {
        headers: authHeaders(cleanupAdminToken),
      });

      if (res.status() === 401 || res.status() === 403) {
        cleanupAdminToken = (await login(request, ADMIN_EMAIL)).token;
        res = await request.delete(url, {
          headers: authHeaders(cleanupAdminToken),
        });
      }

      return res;
    };

    const adminPostWithRetry = async (url: string) => {
      let res = await request.post(url, {
        headers: authHeaders(cleanupAdminToken),
      });

      if (res.status() === 401 || res.status() === 403) {
        cleanupAdminToken = (await login(request, ADMIN_EMAIL)).token;
        res = await request.post(url, {
          headers: authHeaders(cleanupAdminToken),
        });
      }

      return res;
    };

    const deleteSwapIfExists = async (swapId: number) => {
      // Pending swaps often must be cancelled before delete. Non-pending may return 400 here.
      const cancelRes = await adminPostWithRetry(`${API_URL}/shiftswaps/${swapId}/cancel`);
      expect([200, 400, 404], `Cancel swap ${swapId} during teardown`).toContain(cancelRes.status());

      const deleteRes = await adminDeleteWithRetry(`${API_URL}/shiftswaps/${swapId}`);
      expect([204, 404], `Delete swap ${swapId} during teardown`).toContain(deleteRes.status());
    };

    const deleteIfExists = async (url: string) => {
      const res = await adminDeleteWithRetry(url);
      expect([204, 404], `Delete ${url} during teardown`).toContain(res.status());
    };

    for (const swapId of Array.from(swapsToCleanup).reverse()) {
      await deleteSwapIfExists(swapId);
    }
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

  test('E2E-SS-01 — browser auth session can create and cancel a swap', async ({ page }) => {
    await seedAuthState(page, ownerSession);

    await page.goto('/');
    await expect(page).not.toHaveURL(/\/login/);

    const createResponse = await page.request.post(`${API_URL}/shiftswaps`, {
      headers: authHeaders(ownerSession.token),
      data: {
        originalShiftAssignmentId: assignmentId,
        employeeFromId: ownerSession.employeeId,
        employeeToId: targetEmployeeId,
        requestDatetime: new Date(Date.now() - 60_000).toISOString().slice(0, 19),
        reason: 'E2E Playwright test — login UI → JWT → swap API',
      },
    });

    expect(createResponse.status()).toBe(201);
    const swap = await createResponse.json();
    expect(swap.swapStatus).toMatch(/pending/i);

    const swapId: number = swap.shiftSwapId;
    swapsToCleanup.add(swapId);

    // Cancel the swap to leave the database clean and verify the cancel path.
    const cancelResponse = await page.request.post(`${API_URL}/shiftswaps/${swapId}/cancel`, {
      headers: authHeaders(ownerSession.token),
    });
    expect(cancelResponse.status()).toBe(200);
    const cancelled = await cancelResponse.json();
    expect(cancelled.swapStatus).toMatch(/cancelled/i);
    // Keep swap id for teardown: cancelled swaps still hold FK references until deleted.
  });

  test('E2E-SS-02 — non-owner cannot cancel owner swap (400)', async ({ page }) => {
    await seedAuthState(page, ownerSession);
    await page.goto('/');

    const createRes = await page.request.post(`${API_URL}/shiftswaps`, {
      headers: authHeaders(ownerSession.token),
      data: {
        originalShiftAssignmentId: assignmentId,
        employeeFromId: ownerSession.employeeId,
        employeeToId: targetEmployeeId,
        requestDatetime: new Date(Date.now() - 60_000).toISOString().slice(0, 19),
        reason: 'E2E non-owner cancel test',
      },
    });
    expect(createRes.status()).toBe(201);
    const swap = await createRes.json();
    const swapId = swap.shiftSwapId as number;
    swapsToCleanup.add(swapId);

    const nonOwnerCancelRes = await page.request.post(`${API_URL}/shiftswaps/${swapId}/cancel`, {
      headers: authHeaders(nonOwnerSession.token),
    });
    expect(nonOwnerCancelRes.status()).toBe(400);
  });

});

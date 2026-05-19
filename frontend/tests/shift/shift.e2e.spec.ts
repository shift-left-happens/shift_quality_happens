/**
 * Shift — End-to-End tests
 *
 * Drives the Shift planner and create/edit form in a real browser session,
 * through the ShiftPage page object.
 *
 * The suite is self-contained: it seeds an authenticated admin session into
 * localStorage, creates shifts via the UI / API, and deletes every shift it
 * created in afterAll.
 *
 * E2E-SH-01 walks the full planner → create → edit workflow as one flow.
 * The remaining tests are independent guards: a validation rejection, the
 * frontend↔backend status regression check (ShiftFormPage previously
 * defaulted shiftStatus to 'SCHEDULED', which the backend rejects), and the
 * protected-route redirect.
 */

import { test, expect } from '@playwright/test';
import type { APIRequestContext, Page } from '@playwright/test';
import { ShiftPage } from '../pages/ShiftPage';

const API_URL = process.env.API_URL || 'http://localhost:8080';
const ADMIN_EMAIL = process.env.TEST_ADMIN_EMAIL || 'admin@shift.dk';
const ADMIN_PASSWORD = process.env.TEST_USER_PASSWORD || 'password123';

const BACKEND_STATUSES = ['Open', 'Assigned', 'Pending Swap', 'Cancelled', 'Completed'];

type LoginResponse = {
  token: string;
  employeeId: number;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  roleId: number;
  roleName: string;
};

function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}` };
}

async function login(request: APIRequestContext, email: string, password: string): Promise<LoginResponse> {
  const res = await request.post(`${API_URL}/auth/login`, { data: { email, password } });
  expect(res.status(), `Expected successful login for ${email}`).toBe(200);
  return (await res.json()) as LoginResponse;
}

/** Seed an authenticated session the way AuthContext stores it (token + user). */
async function seedAuthState(page: Page, session: LoginResponse) {
  const { token, ...user } = session;
  await page.addInitScript(
    ({ token, user }) => {
      localStorage.setItem('shift_happens_token', token);
      localStorage.setItem('shift_happens_user', JSON.stringify(user));
    },
    { token, user },
  );
}

/** A `YYYY-MM-DDTHH:MM` string for a <input type="datetime-local">. */
function futureDateTimeLocal(daysFromNow: number, hour: number): string {
  const d = new Date(Date.now() + daysFromNow * 24 * 60 * 60 * 1000);
  d.setHours(hour, 0, 0, 0);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

test.describe.serial('Shift E2E', () => {
  let adminSession: LoginResponse;
  const createdShiftIds: number[] = [];

  test.beforeAll(async ({ request }) => {
    adminSession = await login(request, ADMIN_EMAIL, ADMIN_PASSWORD);
    expect(adminSession.roleName).toBe('Administrator');
  });

  test.afterAll(async ({ request }) => {
    for (const id of [...createdShiftIds].reverse()) {
      const res = await request.delete(`${API_URL}/shifts/${id}`, {
        headers: authHeaders(adminSession.token),
      });
      expect(res.status()).toBe(204);
    }
  });

  /** Find the id of a shift by its unique name via the API. */
  async function findShiftIdByName(request: APIRequestContext, name: string): Promise<number> {
    const res = await request.get(`${API_URL}/shifts`, {
      headers: authHeaders(adminSession.token),
    });
    expect(res.status()).toBe(200);
    const match = (await res.json()).find((s: { shiftName: string }) => s.shiftName === name);
    expect(match, `Shift "${name}" should exist`).toBeTruthy();
    return match.shiftId as number;
  }

  // ── E2E-SH-01 — Planner → create → edit workflow ──────────────────────────

  test('E2E-SH-01 — admin loads the planner, creates a shift and edits it (FR-SH-01, FR-SH-02)', async ({
    page,
    request,
  }) => {
    await seedAuthState(page, adminSession);
    const shiftPage = new ShiftPage(page);

    // 1. The planner loads with a New shift action
    await shiftPage.gotoPlanner();
    await expect(page).not.toHaveURL(/\/login/);
    await expect(shiftPage.plannerHeading).toBeVisible();
    await expect(shiftPage.newShiftLink).toBeVisible();

    // 2. Create a shift through the form — status is left untouched on
    //    purpose, so its default must be backend-valid.
    await shiftPage.gotoNew();
    const shiftName = `E2E Created Shift ${Date.now().toString(36)}`;
    await shiftPage.fillForm({
      name: shiftName,
      start: futureDateTimeLocal(10, 9),
      end: futureDateTimeLocal(10, 17),
      pickReferences: true,
    });
    await shiftPage.submit();

    // 3. On success the form navigates back to the planner; track the
    //    created shift for cleanup.
    await expect(page).toHaveURL(/\/shifts$/);
    const shiftId = await findShiftIdByName(request, shiftName);
    createdShiftIds.push(shiftId);

    // 4. Edit the shift just created via the form
    await shiftPage.gotoEdit(shiftId);
    const renamed = `${shiftName} (edited)`;
    await expect(shiftPage.nameInput).toHaveValue(shiftName);
    await shiftPage.fillForm({ name: renamed });
    await shiftPage.submit();
    await expect(page).toHaveURL(/\/shifts$/);

    // 5. The rename persisted on the backend
    const verifyRes = await request.get(`${API_URL}/shifts/${shiftId}`, {
      headers: authHeaders(adminSession.token),
    });
    expect(verifyRes.status()).toBe(200);
    expect((await verifyRes.json()).shiftName).toBe(renamed);
  });

  // ── E2E-SH-02 — Invalid shift surfaces an error ───────────────────────────

  test('E2E-SH-02 — end time before start time shows a validation error', async ({ page }) => {
    await seedAuthState(page, adminSession);
    const shiftPage = new ShiftPage(page);
    await shiftPage.gotoNew();

    await shiftPage.fillForm({
      name: `E2E Invalid Shift ${Date.now().toString(36)}`,
      start: futureDateTimeLocal(10, 17),
      end: futureDateTimeLocal(10, 9),
      pickReferences: true,
    });
    await shiftPage.submit();

    // The backend rejects it (400) and the form stays put showing the error.
    await expect(shiftPage.errorAlert).toBeVisible();
    await expect(page).toHaveURL(/\/shifts\/new/);
  });

  // ── E2E-SH-03 — Status options match the backend (regression) ─────────────

  test('E2E-SH-03 — form status options match backend VALID_STATUSES', async ({ page }) => {
    await seedAuthState(page, adminSession);
    const shiftPage = new ShiftPage(page);
    await shiftPage.gotoNew();

    const options = await shiftPage.statusSelect.locator('option').allTextContents();
    expect(options.map((o) => o.trim())).toEqual(BACKEND_STATUSES);
  });

  // ── E2E-SH-04 — Protected route ───────────────────────────────────────────

  test('E2E-SH-04 — unauthenticated user is redirected to login', async ({ page }) => {
    await page.goto('/shifts');
    await expect(page).toHaveURL(/\/login/);
  });
});

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
 * It also guards the frontend↔backend status fix: ShiftFormPage previously
 * defaulted shiftStatus to 'SCHEDULED', which the backend rejects. E2E-SH-05
 * pins the form's status options to ShiftService.VALID_STATUSES.
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
    expect(
      ['Administrator', 'Manager'],
      `${ADMIN_EMAIL} must be Administrator or Manager`,
    ).toContain(adminSession.roleName);
  });

  test.afterAll(async ({ request }) => {
    for (const id of [...createdShiftIds].reverse()) {
      const res = await request.delete(`${API_URL}/shifts/${id}`, {
        headers: authHeaders(adminSession.token),
      });
      expect([204, 400, 404]).toContain(res.status());
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

  // ── E2E-SH-01 — Planner loads ─────────────────────────────────────────────

  test('E2E-SH-01 — admin sees the shift planner with a New shift action', async ({ page }) => {
    await seedAuthState(page, adminSession);
    const shiftPage = new ShiftPage(page);
    await shiftPage.gotoPlanner();

    await expect(page).not.toHaveURL(/\/login/);
    await expect(shiftPage.plannerHeading).toBeVisible();
    await expect(shiftPage.newShiftLink).toBeVisible();
  });

  // ── E2E-SH-02 — Create a shift through the form ───────────────────────────

  test('E2E-SH-02 — admin creates a shift via the form (FR-SH-01)', async ({ page, request }) => {
    await seedAuthState(page, adminSession);
    const shiftPage = new ShiftPage(page);
    await shiftPage.gotoNew();

    const shiftName = `E2E Created Shift ${Date.now().toString(36)}`;
    // Status is left untouched on purpose — its default must be backend-valid.
    await shiftPage.fillForm({
      name: shiftName,
      start: futureDateTimeLocal(10, 9),
      end: futureDateTimeLocal(10, 17),
      pickReferences: true,
    });
    await shiftPage.submit();

    // On success the form navigates back to the planner.
    await expect(page).toHaveURL(/\/shifts$/);

    const id = await findShiftIdByName(request, shiftName);
    createdShiftIds.push(id);
  });

  // ── E2E-SH-03 — Invalid shift surfaces an error ───────────────────────────

  test('E2E-SH-03 — end time before start time shows a validation error', async ({ page }) => {
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

  // ── E2E-SH-04 — Edit an existing shift ────────────────────────────────────

  test('E2E-SH-04 — admin edits a shift via the form (FR-SH-02)', async ({ page, request }) => {
    // Seed a shift through the API so the edit form has something to load.
    const original = `E2E Edit Source ${Date.now().toString(36)}`;
    const createRes = await request.post(`${API_URL}/shifts`, {
      headers: authHeaders(adminSession.token),
      data: {
        departmentId: (await (await request.get(`${API_URL}/departments`, { headers: authHeaders(adminSession.token) })).json())[0].departmentId,
        workLocationId: (await (await request.get(`${API_URL}/worklocations`, { headers: authHeaders(adminSession.token) })).json())[0].workLocationId,
        shiftName: original,
        startDatetime: `${futureDateTimeLocal(12, 9)}:00`,
        endDatetime: `${futureDateTimeLocal(12, 17)}:00`,
        shiftStatus: 'Open',
      },
    });
    expect(createRes.status()).toBe(201);
    const shiftId = (await createRes.json()).shiftId as number;
    createdShiftIds.push(shiftId);

    await seedAuthState(page, adminSession);
    const shiftPage = new ShiftPage(page);
    await shiftPage.gotoEdit(shiftId);

    const renamed = `${original} (edited)`;
    await expect(shiftPage.nameInput).toHaveValue(original);
    await shiftPage.fillForm({ name: renamed });
    await shiftPage.submit();

    await expect(page).toHaveURL(/\/shifts$/);

    const verifyRes = await request.get(`${API_URL}/shifts/${shiftId}`, {
      headers: authHeaders(adminSession.token),
    });
    expect(verifyRes.status()).toBe(200);
    expect((await verifyRes.json()).shiftName).toBe(renamed);
  });

  // ── E2E-SH-05 — Status options match the backend (regression) ─────────────

  test('E2E-SH-05 — form status options match backend VALID_STATUSES', async ({ page }) => {
    await seedAuthState(page, adminSession);
    const shiftPage = new ShiftPage(page);
    await shiftPage.gotoNew();

    const options = await shiftPage.statusSelect.locator('option').allTextContents();
    expect(options.map((o) => o.trim())).toEqual(BACKEND_STATUSES);
  });

  // ── E2E-SH-06 — Protected route ───────────────────────────────────────────

  test('E2E-SH-06 — unauthenticated user is redirected to login', async ({ page }) => {
    await page.goto('/shifts');
    await expect(page).toHaveURL(/\/login/);
  });
});

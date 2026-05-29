/**
 * Shift Approval — End-to-End tests
 *
 * There is no dedicated shift-approval page in the frontend, so (following the same convention as
 * shiftswap.e2e.spec.ts) this spec validates browser-authenticated API calls made from a real app
 * session: an admin/manager loads the app in the browser, then records an approval decision through
 * the authenticated session, and an employee session is blocked from doing the same.
 *
 * The suite is self-contained: it creates and cleans up its own employee, shift, assignment and
 * approval (FK-safe teardown order).
 */

import { test, expect } from '@playwright/test';
import { API_URL, login, authHeaders, futureShiftWindow, fmt, type LoginResponse } from '../pages/helper/api-helpers';
import { seedAuthState, ensureBrowserAuthenticated } from '../pages/helper/browser-auth';

const ADMIN_EMAIL = process.env.TEST_ADMIN_EMAIL || 'admin@shift.dk';
const TEST_EMPLOYEE_PASSWORD = process.env.TEST_EMPLOYEE_PASSWORD || 'TestPass123';

test.describe('Shift Approval E2E', () => {
  let adminSession: LoginResponse;
  let employeeSession: LoginResponse;
  let approverEmployeeId = 0;
  let assignmentId = 0;

  const employeeIds: number[] = [];
  const shiftIds: number[] = [];
  const assignmentIds: number[] = [];
  const approvalIds: number[] = [];

  test.beforeAll(async ({ request }) => {
    adminSession = await login(request, ADMIN_EMAIL);
    expect(adminSession.roleName).toBe('Administrator');

    const suffix = Date.now().toString(36);
    const email = `sa.apprE2E.${suffix}@shifthappens.dk`;

    const empRes = await request.post(`${API_URL}/employees`, {
      headers: authHeaders(adminSession.token),
      data: {
        employeeNumber: `EMP-APPRE2E-${suffix}`,
        firstName: 'Erik',
        lastName: 'Approver',
        userRole: 'Employee',
        email,
        loginPassword: TEST_EMPLOYEE_PASSWORD,
        phoneNumber: '+45 12345678',
        hireDate: '2026-01-15',
        birthDate: '1999-01-01',
        employmentStatus: 'ACTIVE',
        primaryWorkLocationId: 1,
      },
    });
    expect(empRes.status()).toBe(201);
    approverEmployeeId = (await empRes.json()).employeeId as number;
    employeeIds.push(approverEmployeeId);

    const { startDatetime, endDatetime } = futureShiftWindow(9);
    const shiftRes = await request.post(`${API_URL}/shifts`, {
      headers: authHeaders(adminSession.token),
      data: {
        departmentId: 1,
        workLocationId: 1,
        shiftName: `Approval E2E ${suffix}`,
        startDatetime,
        endDatetime,
        shiftStatus: 'Open',
      },
    });
    expect(shiftRes.status()).toBe(201);
    const shiftId = (await shiftRes.json()).shiftId as number;
    shiftIds.push(shiftId);

    const assignRes = await request.post(`${API_URL}/shiftassignments`, {
      headers: authHeaders(adminSession.token),
      data: { shiftId, employeeId: approverEmployeeId, assignmentStatus: 'Assigned', assignedDatetime: fmt(new Date(Date.now() - 10000)) },
    });
    expect(assignRes.status()).toBe(201);
    assignmentId = (await assignRes.json()).shiftAssignmentId as number;
    assignmentIds.push(assignmentId);

    employeeSession = await login(request, email, TEST_EMPLOYEE_PASSWORD);
  });

  test.afterAll(async ({ request }) => {
    let token = (await login(request, ADMIN_EMAIL)).token;
    const del = async (path: string) => {
      let res = await request.delete(`${API_URL}${path}`, { headers: authHeaders(token) });
      if (res.status() === 401 || res.status() === 403) {
        token = (await login(request, ADMIN_EMAIL)).token;
        res = await request.delete(`${API_URL}${path}`, { headers: authHeaders(token) });
      }
      return res.status();
    };
    for (const id of [...approvalIds].reverse()) await del(`/shiftapprovals/${id}`);
    for (const id of [...assignmentIds].reverse()) await del(`/shiftassignments/${id}`);
    for (const id of [...shiftIds].reverse()) await del(`/shifts/${id}`);
    for (const id of [...employeeIds].reverse()) await del(`/employees/${id}`);
  });

  // ── E2E-SA-01 — admin records an approval from a browser session; employee is blocked ──

  test('E2E-SA-01 — admin approves from a browser session; an employee session cannot', async ({ page }) => {
    await seedAuthState(page, adminSession);
    await ensureBrowserAuthenticated(page, adminSession);

    // 1. The admin, authenticated in the browser, records an approval decision via the API.
    const createRes = await page.request.post(`${API_URL}/shiftapprovals`, {
      headers: authHeaders(adminSession.token),
      data: {
        shiftAssignmentId: assignmentId,
        approverEmployeeId,
        decision: 'Approved',
        approvalComment: 'E2E Playwright — login UI → JWT → approval API',
        decisionDatetime: fmt(new Date(Date.now() - 5000)),
      },
    });
    expect(createRes.status()).toBe(200);
    const approval = await createRes.json();
    const approvalId: number = approval.shiftApprovalId;
    approvalIds.push(approvalId);
    expect(approval.decision).toBe('Approved');

    // 2. The decision is retrievable through the same authenticated session.
    const getRes = await page.request.get(`${API_URL}/shiftapprovals/${approvalId}`, {
      headers: authHeaders(adminSession.token),
    });
    expect(getRes.status()).toBe(200);
    expect((await getRes.json()).shiftApprovalId).toBe(approvalId);

    // 3. An employee session cannot record an approval (read/write are ADMIN/MANAGER-only).
    const employeeAttempt = await page.request.post(`${API_URL}/shiftapprovals`, {
      headers: authHeaders(employeeSession.token),
      data: { shiftAssignmentId: assignmentId, approverEmployeeId, decision: 'Approved' },
    });
    expect(employeeAttempt.status()).toBe(403);
  });
});

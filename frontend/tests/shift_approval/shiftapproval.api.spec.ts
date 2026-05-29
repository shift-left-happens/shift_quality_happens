/**
 * Shift Approval — API tests (/shiftapprovals)
 *
 * Exercises the full CRUD surface of ShiftApprovalController against the running backend:
 * status codes, JSON content, response time, and positive + negative paths.
 *
 * shift_approval has FK constraints to shift_assignment and employee (01-schema.sql), so the suite
 * creates a real employee → shift → assignment to reference, and tears everything down in
 * FK-safe order (approvals → assignments → shifts → employees).
 *
 * Authorization note: ShiftApprovalController restricts every endpoint — including reads — to
 * ADMINISTRATOR/MANAGER via @PreAuthorize, on top of the global write rules in SecurityConfig.
 * The negative tests assert that an EMPLOYEE token is rejected and that anonymous access is 401.
 *
 * Two current backend quirks are pinned by tests (see comments): create returns 200 (no
 * @ResponseStatus → not 201), and updating an unknown id surfaces as 500 (unhandled
 * NoSuchElementException) rather than 404.
 */

import { test, expect, type APIRequestContext } from '@playwright/test';
import { API_URL, login, loginAndGetToken, authHeaders, futureShiftWindow, fmt } from '../pages/helper/api-helpers';

const ADMIN_EMAIL = process.env.TEST_ADMIN_EMAIL || 'admin@shift.dk';
const TEST_EMPLOYEE_PASSWORD = process.env.TEST_EMPLOYEE_PASSWORD || 'TestPass123';

test.describe.serial('Shift Approval API', () => {
  let adminToken = '';
  let employeeToken = '';
  let approverEmployeeId = 0;
  let assignmentId = 0;

  const employeeIds: number[] = [];
  const shiftIds: number[] = [];
  const assignmentIds: number[] = [];
  const approvalIds: number[] = [];

  const createApproval = async (
    request: APIRequestContext,
    decision = 'Approved',
  ): Promise<{ status: number; id?: number; body?: Record<string, unknown> }> => {
    const res = await request.post(`${API_URL}/shiftapprovals`, {
      headers: authHeaders(adminToken),
      data: {
        shiftAssignmentId: assignmentId,
        approverEmployeeId,
        decision,
        approvalComment: 'API test approval',
        decisionDatetime: fmt(new Date(Date.now() - 5000)),
      },
    });
    if (res.status() === 200) {
      const body = await res.json();
      const id = body.shiftApprovalId as number;
      approvalIds.push(id);
      return { status: 200, id, body };
    }
    return { status: res.status() };
  };

  test.beforeAll(async ({ request }) => {
    adminToken = (await loginAndGetToken(request, ADMIN_EMAIL)).token;

    const suffix = Date.now().toString(36);
    const employeeEmail = `sa.approval.${suffix}@shifthappens.dk`;

    // Approver employee (also used to obtain an EMPLOYEE-role token for the negative tests).
    const empRes = await request.post(`${API_URL}/employees`, {
      headers: authHeaders(adminToken),
      data: {
        employeeNumber: `EMP-APPR-${suffix}`,
        firstName: 'Appr',
        lastName: 'Over',
        userRole: 'Employee',
        email: employeeEmail,
        loginPassword: TEST_EMPLOYEE_PASSWORD,
        phoneNumber: '+45 12345678',
        hireDate: '2026-01-15',
        birthDate: '1999-01-01',
        employmentStatus: 'ACTIVE',
        primaryWorkLocationId: 1,
      },
    });
    expect(empRes.status(), 'create approver employee').toBe(201);
    approverEmployeeId = (await empRes.json()).employeeId as number;
    employeeIds.push(approverEmployeeId);

    const { startDatetime, endDatetime } = futureShiftWindow(9);
    const shiftRes = await request.post(`${API_URL}/shifts`, {
      headers: authHeaders(adminToken),
      data: {
        departmentId: 1,
        workLocationId: 1,
        shiftName: `Approval API ${suffix}`,
        startDatetime,
        endDatetime,
        shiftStatus: 'Open',
      },
    });
    expect(shiftRes.status(), 'create shift').toBe(201);
    const shiftId = (await shiftRes.json()).shiftId as number;
    shiftIds.push(shiftId);

    const assignRes = await request.post(`${API_URL}/shiftassignments`, {
      headers: authHeaders(adminToken),
      data: { shiftId, employeeId: approverEmployeeId, assignmentStatus: 'Assigned', assignedDatetime: fmt(new Date(Date.now() - 10000)) },
    });
    expect(assignRes.status(), 'create assignment').toBe(201);
    assignmentId = (await assignRes.json()).shiftAssignmentId as number;
    assignmentIds.push(assignmentId);

    employeeToken = (await login(request, employeeEmail, TEST_EMPLOYEE_PASSWORD)).token;
  });

  test.afterAll(async ({ request }) => {
    let token = (await loginAndGetToken(request, ADMIN_EMAIL)).token;
    const del = async (path: string) => {
      let res = await request.delete(`${API_URL}${path}`, { headers: authHeaders(token) });
      if (res.status() === 401 || res.status() === 403) {
        token = (await loginAndGetToken(request, ADMIN_EMAIL)).token;
        res = await request.delete(`${API_URL}${path}`, { headers: authHeaders(token) });
      }
      return res.status();
    };

    for (const id of [...approvalIds].reverse()) await del(`/shiftapprovals/${id}`);
    for (const id of [...assignmentIds].reverse()) await del(`/shiftassignments/${id}`);
    for (const id of [...shiftIds].reverse()) await del(`/shifts/${id}`);
    for (const id of [...employeeIds].reverse()) await del(`/employees/${id}`);
  });

  // ── Positive ────────────────────────────────────────────────────────────────

  test('POST creates an approval and returns 200 with the persisted body', async ({ request }) => {
    const result = await createApproval(request, 'Approved');
    // No @ResponseStatus(CREATED) on the controller, so a successful create is 200, not 201.
    expect(result.status).toBe(200);
    expect(result.id).toBeGreaterThan(0);
    expect(result.body).toMatchObject({
      shiftAssignmentId: assignmentId,
      approverEmployeeId,
      decision: 'Approved',
    });
  });

  test('GET list returns 200, a JSON array including the created approval, within 2s (NFR-01)', async ({ request }) => {
    const started = Date.now();
    const res = await request.get(`${API_URL}/shiftapprovals`, { headers: authHeaders(adminToken) });
    const elapsedMs = Date.now() - started;

    expect(res.status()).toBe(200);
    expect(res.headers()['content-type']).toContain('application/json');
    const body = await res.json();
    expect(Array.isArray(body)).toBe(true);
    expect(body.some((a: { shiftApprovalId: number }) => approvalIds.includes(a.shiftApprovalId))).toBe(true);
    expect(elapsedMs).toBeLessThan(2000);
  });

  test('GET by id returns 200 with the matching approval', async ({ request }) => {
    const { id } = await createApproval(request, 'Declined');
    const res = await request.get(`${API_URL}/shiftapprovals/${id}`, { headers: authHeaders(adminToken) });

    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.shiftApprovalId).toBe(id);
    expect(body.decision).toBe('Declined');
    expect(body.approverEmployeeId).toBe(approverEmployeeId);
  });

  test('PUT updates an existing approval and returns 200 with the new values', async ({ request }) => {
    const { id } = await createApproval(request, 'Approved');
    const res = await request.put(`${API_URL}/shiftapprovals/${id}`, {
      headers: authHeaders(adminToken),
      data: {
        shiftAssignmentId: assignmentId,
        approverEmployeeId,
        decision: 'Declined',
        approvalComment: 'changed via PUT',
        decisionDatetime: fmt(new Date(Date.now() - 5000)),
      },
    });

    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.decision).toBe('Declined');
    expect(body.approvalComment).toBe('changed via PUT');
  });

  test('DELETE removes an approval and returns 200', async ({ request }) => {
    const { id } = await createApproval(request, 'Approved');
    const res = await request.delete(`${API_URL}/shiftapprovals/${id}`, { headers: authHeaders(adminToken) });
    // void handler with no @ResponseStatus → 200.
    expect(res.status()).toBe(200);

    // Track removal so afterAll does not try to delete it again.
    const idx = approvalIds.indexOf(id!);
    if (idx >= 0) approvalIds.splice(idx, 1);
  });

  // ── Negative ────────────────────────────────────────────────────────────────

  test('PUT on an unknown id returns 404', async ({ request }) => {
    // updateShiftApproval maps a missing id to a 404 (previously this leaked as a masked 401/500
    // from an unhandled NoSuchElementException).
    const res = await request.put(`${API_URL}/shiftapprovals/999999999`, {
      headers: authHeaders(adminToken),
      data: { decision: 'Approved' },
    });
    expect(res.status()).toBe(404);
  });

  test('GET is forbidden for an EMPLOYEE token (403)', async ({ request }) => {
    const res = await request.get(`${API_URL}/shiftapprovals`, { headers: authHeaders(employeeToken) });
    expect(res.status()).toBe(403);
  });

  test('POST is forbidden for an EMPLOYEE token (403)', async ({ request }) => {
    const res = await request.post(`${API_URL}/shiftapprovals`, {
      headers: authHeaders(employeeToken),
      data: { shiftAssignmentId: assignmentId, approverEmployeeId, decision: 'Approved' },
    });
    expect(res.status()).toBe(403);
  });

  test('DELETE is forbidden for an EMPLOYEE token (403)', async ({ request }) => {
    const res = await request.delete(`${API_URL}/shiftapprovals/1`, { headers: authHeaders(employeeToken) });
    expect(res.status()).toBe(403);
  });

  test('GET without a token is unauthorized (401)', async ({ request }) => {
    const res = await request.get(`${API_URL}/shiftapprovals`);
    expect(res.status()).toBe(401);
  });
});

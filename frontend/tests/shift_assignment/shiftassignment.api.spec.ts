/**
 * Shift Assignment — API tests (/shiftassignments)
 *
 * Full CRUD coverage of ShiftAssignmentController plus the validation rules from the black-box
 * "Shift Creation" decision table, tested against the running backend: status codes, JSON content,
 * positive + negative paths, role authorization, and the EMPLOYEE row-scoping the controller adds.
 *
 * The role-matching cases live in the existing shiftassignment-role-link.api.spec.ts; this spec is
 * complementary and does not duplicate them.
 *
 * Self-contained: creates its own employees/shifts/assignments and tears them down FK-safely
 * (assignments → shifts → employees).
 */

import { test, expect, type APIRequestContext } from '@playwright/test';
import { API_URL, login, loginAndGetToken, authHeaders, futureShiftWindow, fmt } from '../pages/helper/api-helpers';

const ADMIN_EMAIL = process.env.TEST_ADMIN_EMAIL || 'admin@shift.dk';
const TEST_EMPLOYEE_PASSWORD = process.env.TEST_EMPLOYEE_PASSWORD || 'TestPass123';

test.describe.serial('Shift Assignment API (CRUD + validation)', () => {
  let adminToken = '';

  const employeeIds: number[] = [];
  const shiftIds: number[] = [];
  const assignmentIds: number[] = [];

  const createEmployee = async (request: APIRequestContext, label: string): Promise<{ id: number; email: string }> => {
    const suffix = `${Date.now().toString(36)}-${Math.floor(Math.random() * 1e6)}`;
    const email = `sa.crud.${label}.${suffix}@shifthappens.dk`;
    const res = await request.post(`${API_URL}/employees`, {
      headers: authHeaders(adminToken),
      data: {
        employeeNumber: `EMP-SACRUD-${label}-${suffix}`,
        firstName: `First${label}`,
        lastName: `Last${label}`,
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
    expect(res.status(), `create employee ${label}`).toBe(201);
    const id = (await res.json()).employeeId as number;
    employeeIds.push(id);
    return { id, email };
  };

  const createShift = async (
    request: APIRequestContext,
    daysFromNow: number,
    status = 'Open',
    hours = 8,
  ): Promise<number> => {
    const { startDatetime, endDatetime } = futureShiftWindow(daysFromNow, hours);
    const res = await request.post(`${API_URL}/shifts`, {
      headers: authHeaders(adminToken),
      data: {
        departmentId: 1,
        workLocationId: 1,
        shiftName: `SA CRUD ${status} ${daysFromNow}-${Math.floor(Math.random() * 1e6)}`,
        startDatetime,
        endDatetime,
        shiftStatus: status,
      },
    });
    expect(res.status(), `create shift (${status})`).toBe(201);
    const id = (await res.json()).shiftId as number;
    shiftIds.push(id);
    return id;
  };

  const assign = async (
    request: APIRequestContext,
    body: Record<string, unknown>,
    token = adminToken,
  ): Promise<{ status: number; id?: number; text?: string }> => {
    const res = await request.post(`${API_URL}/shiftassignments`, { headers: authHeaders(token), data: body });
    if (res.status() === 201) {
      const id = (await res.json()).shiftAssignmentId as number;
      assignmentIds.push(id);
      return { status: 201, id };
    }
    return { status: res.status(), text: await res.text() };
  };

  test.beforeAll(async ({ request }) => {
    adminToken = (await loginAndGetToken(request, ADMIN_EMAIL)).token;
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
    for (const id of [...assignmentIds].reverse()) await del(`/shiftassignments/${id}`);
    for (const id of [...shiftIds].reverse()) await del(`/shifts/${id}`);
    for (const id of [...employeeIds].reverse()) await del(`/employees/${id}`);
  });

  // ── Positive CRUD ────────────────────────────────────────────────────────────

  test('POST assigns an active employee to an open shift → 201 with JSON body', async ({ request }) => {
    const { id: employeeId } = await createEmployee(request, 'happy');
    const shiftId = await createShift(request, 10);

    const res = await request.post(`${API_URL}/shiftassignments`, {
      headers: authHeaders(adminToken),
      data: { shiftId, employeeId, assignmentStatus: 'Assigned', assignedDatetime: fmt(new Date(Date.now() - 10000)) },
    });
    expect(res.status()).toBe(201);
    expect(res.headers()['content-type']).toContain('application/json');
    const body = await res.json();
    expect(body.shiftAssignmentId).toBeGreaterThan(0);
    expect(body).toMatchObject({ shiftId, employeeId, assignmentStatus: 'Assigned' });
    assignmentIds.push(body.shiftAssignmentId as number);
  });

  test('POST defaults assignmentStatus to "Assigned" when omitted', async ({ request }) => {
    const { id: employeeId } = await createEmployee(request, 'default');
    const shiftId = await createShift(request, 12);
    const result = await assign(request, { shiftId, employeeId });
    expect(result.status).toBe(201);
  });

  test('GET list returns 200 and a JSON array, within 2s (NFR-01)', async ({ request }) => {
    const started = Date.now();
    const res = await request.get(`${API_URL}/shiftassignments`, { headers: authHeaders(adminToken) });
    const elapsedMs = Date.now() - started;
    expect(res.status()).toBe(200);
    expect(Array.isArray(await res.json())).toBe(true);
    expect(elapsedMs).toBeLessThan(2000);
  });

  test('GET by id returns 200 with the matching assignment', async ({ request }) => {
    const { id: employeeId } = await createEmployee(request, 'getbyid');
    const shiftId = await createShift(request, 14);
    const { id } = await assign(request, { shiftId, employeeId });

    const res = await request.get(`${API_URL}/shiftassignments/${id}`, { headers: authHeaders(adminToken) });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.shiftAssignmentId).toBe(id);
    expect(body.employeeId).toBe(employeeId);
  });

  test('PUT updates assignment status → 200 with new status', async ({ request }) => {
    const { id: employeeId } = await createEmployee(request, 'put');
    const shiftId = await createShift(request, 16);
    const { id } = await assign(request, { shiftId, employeeId });

    const res = await request.put(`${API_URL}/shiftassignments/${id}`, {
      headers: authHeaders(adminToken),
      data: { assignmentStatus: 'Completed' },
    });
    expect(res.status()).toBe(200);
    expect((await res.json()).assignmentStatus).toBe('Completed');
  });

  test('DELETE removes an assignment → 204', async ({ request }) => {
    const { id: employeeId } = await createEmployee(request, 'del');
    const shiftId = await createShift(request, 18);
    const { id } = await assign(request, { shiftId, employeeId });

    const res = await request.delete(`${API_URL}/shiftassignments/${id}`, { headers: authHeaders(adminToken) });
    expect(res.status()).toBe(204);
    const idx = assignmentIds.indexOf(id!);
    if (idx >= 0) assignmentIds.splice(idx, 1);
  });

  // ── Negative: validation (maps to the black-box "Shift Creation" decision table) ──

  test('POST without shiftId → 400', async ({ request }) => {
    const { id: employeeId } = await createEmployee(request, 'noshift');
    const result = await assign(request, { employeeId, assignmentStatus: 'Assigned' });
    expect(result.status).toBe(400);
  });

  test('POST with a non-existent shiftId → 400', async ({ request }) => {
    const { id: employeeId } = await createEmployee(request, 'badshift');
    const result = await assign(request, { shiftId: 999999999, employeeId });
    expect(result.status).toBe(400);
  });

  test('POST to a shift that is not Open → 400', async ({ request }) => {
    const { id: employeeId } = await createEmployee(request, 'closed');
    const shiftId = await createShift(request, 20, 'Completed');
    const result = await assign(request, { shiftId, employeeId });
    expect(result.status).toBe(400);
    expect(result.text ?? '').toMatch(/not open/i);
  });

  test('POST with an invalid assignmentStatus → 400', async ({ request }) => {
    const { id: employeeId } = await createEmployee(request, 'badstatus');
    const shiftId = await createShift(request, 22);
    const result = await assign(request, { shiftId, employeeId, assignmentStatus: 'Bogus' });
    expect(result.status).toBe(400);
  });

  test('POST that overlaps an existing assignment (rest-period rule) → 400', async ({ request }) => {
    // BR-SH-02 / BR-WT-02: two shifts in the same window for one employee must clash.
    const { id: employeeId } = await createEmployee(request, 'overlap');
    const shiftA = await createShift(request, 24);
    const shiftB = await createShift(request, 24); // identical window → overlap

    const first = await assign(request, { shiftId: shiftA, employeeId });
    expect(first.status).toBe(201);

    const second = await assign(request, { shiftId: shiftB, employeeId });
    expect(second.status).toBe(400);
    expect(second.text ?? '').toMatch(/overlap|rest period/i);
  });

  test('PUT with an invalid assignmentStatus → 400', async ({ request }) => {
    const { id: employeeId } = await createEmployee(request, 'putbad');
    const shiftId = await createShift(request, 26);
    const { id } = await assign(request, { shiftId, employeeId });

    const res = await request.put(`${API_URL}/shiftassignments/${id}`, {
      headers: authHeaders(adminToken),
      data: { assignmentStatus: 'Nope' },
    });
    expect(res.status()).toBe(400);
  });

  // ── Negative: not found ──────────────────────────────────────────────────────

  test('GET by unknown id → 404', async ({ request }) => {
    const res = await request.get(`${API_URL}/shiftassignments/999999999`, { headers: authHeaders(adminToken) });
    expect(res.status()).toBe(404);
  });

  test('DELETE by unknown id → 404', async ({ request }) => {
    const res = await request.delete(`${API_URL}/shiftassignments/999999999`, { headers: authHeaders(adminToken) });
    expect(res.status()).toBe(404);
  });

  // ── Negative: authorization ──────────────────────────────────────────────────

  test('GET without a token → 401', async ({ request }) => {
    const res = await request.get(`${API_URL}/shiftassignments`);
    expect(res.status()).toBe(401);
  });

  test('POST with an EMPLOYEE token → 403', async ({ request }) => {
    const { id: employeeId, email } = await createEmployee(request, 'emppost');
    const shiftId = await createShift(request, 28);
    const employeeToken = (await login(request, email, TEST_EMPLOYEE_PASSWORD)).token;

    const res = await request.post(`${API_URL}/shiftassignments`, {
      headers: authHeaders(employeeToken),
      data: { shiftId, employeeId, assignmentStatus: 'Assigned' },
    });
    expect(res.status()).toBe(403);
  });

  // ── Employee row-scoping (complements the controller slice test at integration level) ──

  test('an EMPLOYEE only sees their own assignments and is forbidden from reading others', async ({ request }) => {
    const ownerEmp = await createEmployee(request, 'owner');
    const otherEmp = await createEmployee(request, 'other');
    const ownerShift = await createShift(request, 30);
    const otherShift = await createShift(request, 32);

    const ownerAssign = await assign(request, { shiftId: ownerShift, employeeId: ownerEmp.id });
    const otherAssign = await assign(request, { shiftId: otherShift, employeeId: otherEmp.id });
    expect(ownerAssign.status).toBe(201);
    expect(otherAssign.status).toBe(201);

    const ownerToken = (await login(request, ownerEmp.email, TEST_EMPLOYEE_PASSWORD)).token;

    // List is scoped to the caller.
    const listRes = await request.get(`${API_URL}/shiftassignments`, { headers: authHeaders(ownerToken) });
    expect(listRes.status()).toBe(200);
    const list = (await listRes.json()) as { employeeId: number }[];
    expect(list.every((a) => a.employeeId === ownerEmp.id)).toBe(true);

    // Reading another employee's assignment by id is forbidden.
    const forbidden = await request.get(`${API_URL}/shiftassignments/${otherAssign.id}`, {
      headers: authHeaders(ownerToken),
    });
    expect(forbidden.status()).toBe(403);
  });
});

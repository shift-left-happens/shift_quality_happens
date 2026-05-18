import { test, expect, type APIRequestContext } from '@playwright/test';
import { API_URL, loginAndGetToken, authHeaders, futureShiftWindow, randomLetters } from '../pages/helper/api-helpers';

const ADMIN_EMAIL = process.env.TEST_ADMIN_EMAIL || 'admin@shift.dk';
const TEST_EMPLOYEE_PASSWORD = process.env.TEST_EMPLOYEE_PASSWORD || 'TestPass123';
const TEST_EMAIL_DOMAIN = '@shifthappens.dk';
const TEST_EMAIL_PREFIX = 'sa.';
const TEST_SHIFT_PREFIX = 'SA role-link';
const TEST_JOBROLE_DESC_MARKER = 'assignment-role relation tests';

async function cleanupRoleLinkTestData(request: APIRequestContext, token: string): Promise<void> {
  let activeToken = token;

  const refreshToken = async () => {
    activeToken = (await loginAndGetToken(request, ADMIN_EMAIL)).token;
  };

  const getArray = async (path: string): Promise<Record<string, unknown>[]> => {
    let res = await request.get(`${API_URL}${path}`, { headers: authHeaders(activeToken) });
    if (res.status() === 401 || res.status() === 403) {
      await refreshToken();
      res = await request.get(`${API_URL}${path}`, { headers: authHeaders(activeToken) });
    }
    expect(res.status(), `GET ${path} should return 200 during cleanup`).toBe(200);
    const body = await res.json();
    expect(Array.isArray(body), `GET ${path} should return an array`).toBe(true);
    return body as Record<string, unknown>[];
  };

  const deleteByPath = async (path: string) => {
    let res = await request.delete(`${API_URL}${path}`, { headers: authHeaders(activeToken) });
    if (res.status() === 401 || res.status() === 403) {
      await refreshToken();
      res = await request.delete(`${API_URL}${path}`, { headers: authHeaders(activeToken) });
    }
    if (res.status() === 401 || res.status() === 403) {
      return;
    }
    if (path.includes('employeejobroles')) {
      expect(res.status(), `DELETE ${path} during cleanup`).toBe(200);
    } else {
      expect(res.status(), `DELETE ${path} during cleanup`).toBe(204);
    }
  };

  const cancelSwapIfPossible = async (swapId: number) => {
    let res = await request.post(`${API_URL}/shiftswaps/${swapId}/cancel`, {
      headers: authHeaders(activeToken),
    });
    if (res.status() === 401 || res.status() === 403) {
      await refreshToken();
      res = await request.post(`${API_URL}/shiftswaps/${swapId}/cancel`, {
        headers: authHeaders(activeToken),
      });
    }
    if (res.status() === 401 || res.status() === 403) {
      return;
    }
    // expect(res.status(), `Cancel swap ${swapId} during cleanup`).toBe(200);
  };

  const employees = await getArray('/employees');
  const testEmployeeIds = new Set<number>(
    employees
      .filter((e) => {
        const email = String(e.email ?? '').toLowerCase();
        return email.startsWith(TEST_EMAIL_PREFIX) && email.endsWith(TEST_EMAIL_DOMAIN);
      })
      .map((e) => Number(e.employeeId))
      .filter((id) => Number.isInteger(id) && id > 0),
  );

  const jobRoles = await getArray('/jobroles');
  const testJobRoleIds = new Set<number>(
    jobRoles
      .filter((jr) => String(jr.jobRoleDescription ?? '').includes(TEST_JOBROLE_DESC_MARKER))
      .map((jr) => Number(jr.jobRoleId))
      .filter((id) => Number.isInteger(id) && id > 0),
  );

  const shifts = await getArray('/shifts');
  const testShiftIds = new Set<number>(
    shifts
      .filter((s) => String(s.shiftName ?? '').startsWith(TEST_SHIFT_PREFIX))
      .map((s) => Number(s.shiftId))
      .filter((id) => Number.isInteger(id) && id > 0),
  );

  const employeeJobRoles = await getArray('/employeejobroles');
  const employeeJobRoleIds = employeeJobRoles
    .filter(
      (ejr) => testEmployeeIds.has(Number(ejr.employeeId)) || testJobRoleIds.has(Number(ejr.jobRoleId)),
    )
    .map((ejr) => Number(ejr.employeeJobRoleId))
    .filter((id) => Number.isInteger(id) && id > 0);

  const shiftRequiredJobRoles = await getArray('/shiftrequiredjobroles');
  const shiftRequiredJobRoleIds = shiftRequiredJobRoles
    .filter(
      (srjr) => testShiftIds.has(Number(srjr.shiftId)) || testJobRoleIds.has(Number(srjr.jobRoleId)),
    )
    .map((srjr) => Number(srjr.shiftRequiredJobRoleId))
    .filter((id) => Number.isInteger(id) && id > 0);

  const shiftAssignments = await getArray('/shiftassignments');
  const shiftAssignmentIds = shiftAssignments
    .filter(
      (sa) => testShiftIds.has(Number(sa.shiftId)) || testEmployeeIds.has(Number(sa.employeeId)),
    )
    .map((sa) => Number(sa.shiftAssignmentId))
    .filter((id) => Number.isInteger(id) && id > 0);

  const shiftswaps = await getArray('/shiftswaps');
  const shiftSwapIds = shiftswaps
    .filter(
      (sw) =>
        shiftAssignmentIds.includes(Number(sw.originalShiftAssignmentId)) ||
        testEmployeeIds.has(Number(sw.employeeFromId)) ||
        testEmployeeIds.has(Number(sw.employeeToId)),
    )
    .map((sw) => Number(sw.shiftSwapId))
    .filter((id) => Number.isInteger(id) && id > 0);

  for (const id of [...shiftSwapIds].reverse()) {
    await cancelSwapIfPossible(id);
    await deleteByPath(`/shiftswaps/${id}`);
  }

  for (const id of [...shiftAssignmentIds].reverse()) {
    await deleteByPath(`/shiftassignments/${id}`);
  }
  for (const id of [...shiftRequiredJobRoleIds].reverse()) {
    await deleteByPath(`/shiftrequiredjobroles/${id}`);
  }
  for (const id of [...employeeJobRoleIds].reverse()) {
    await deleteByPath(`/employeejobroles/${id}`);
  }
  for (const id of Array.from(testShiftIds).reverse()) {
    await deleteByPath(`/shifts/${id}`);
  }
  for (const id of Array.from(testJobRoleIds).reverse()) {
    await deleteByPath(`/jobroles/${id}`);
  }
  for (const id of Array.from(testEmployeeIds).reverse()) {
    await deleteByPath(`/employees/${id}`);
  }
}

test.describe.serial('Shift Assignment Role Link API', () => {
  let adminToken: string;

  const employeeIds: number[] = [];
  const jobRoleIds: number[] = [];
  const shiftIds: number[] = [];
  const employeeJobRoleIds: number[] = [];
  const shiftRequiredJobRoleIds: number[] = [];
  const shiftAssignmentIds: number[] = [];

  test.beforeAll(async ({ request }) => {
    adminToken = (await loginAndGetToken(request, ADMIN_EMAIL)).token;
    await cleanupRoleLinkTestData(request, adminToken);
  });

  test.afterAll(async ({ request }) => {
    if (!adminToken) return;
    const cleanupToken = (await loginAndGetToken(request, ADMIN_EMAIL)).token;

    const deleteIfExists = async (url: string, http_code: number = 204) => {
      const res = await request.delete(url, { headers: authHeaders(cleanupToken) });
      expect(res.status()).toBe(http_code);
    };

    for (const id of [...shiftAssignmentIds].reverse()) {
      await deleteIfExists(`${API_URL}/shiftassignments/${id}`);
    }
    for (const id of [...shiftRequiredJobRoleIds].reverse()) {
      await deleteIfExists(`${API_URL}/shiftrequiredjobroles/${id}`);
    }
    for (const id of [...employeeJobRoleIds].reverse()) {
      await deleteIfExists(`${API_URL}/employeejobroles/${id}`, 200);
    }
    for (const id of [...shiftIds].reverse()) {
      await deleteIfExists(`${API_URL}/shifts/${id}`);
    }
    for (const id of [...jobRoleIds].reverse()) {
      await deleteIfExists(`${API_URL}/jobroles/${id}`);
    }
    for (const id of [...employeeIds].reverse()) {
      await deleteIfExists(`${API_URL}/employees/${id}`);
    }

    // Final sweep catches leftovers from aborted/failed previous runs.
    await cleanupRoleLinkTestData(request, cleanupToken);
  });

  const createEmployee = async (
    request: APIRequestContext,
    suffix: string,
    label: string,
  ): Promise<number> => {
    const res = await request.post(`${API_URL}/employees`, {
      headers: authHeaders(adminToken),
      data: {
        employeeNumber: `EMP-SA-${label}-${suffix}`,
        firstName: `First${label}`,
        lastName: `Last${label}`,
        userRole: 'Employee',
        email: `sa.${label}.${suffix}@shifthappens.dk`,
        loginPassword: TEST_EMPLOYEE_PASSWORD,
        phoneNumber: '+45 12345678',
        hireDate: '2026-01-15',
        birthDate: '1999-01-01',
        employmentStatus: 'ACTIVE',
        primaryWorkLocationId: 1,
      },
    });
    expect(res.status()).toBe(201);
    const id = (await res.json()).employeeId as number;
    employeeIds.push(id);
    return id;
  };

  const createJobRole = async (request: APIRequestContext, label: string): Promise<number> => {
    const res = await request.post(`${API_URL}/jobroles`, {
      headers: authHeaders(adminToken),
      data: {
        roleName: `Role ${label} ${randomLetters(6)}`,
        jobRoleDescription: `Role ${label} for assignment-role relation tests`,
        isCertificationRequired: false,
      },
    });
    expect(res.status()).toBe(201);
    const id = (await res.json()).jobRoleId as number;
    jobRoleIds.push(id);
    return id;
  };

  const createShift = async (request: APIRequestContext, suffix: string, daysFromNow: number): Promise<number> => {
    const { startDatetime, endDatetime } = futureShiftWindow(daysFromNow);
    const res = await request.post(`${API_URL}/shifts`, {
      headers: authHeaders(adminToken),
      data: {
        departmentId: 1,
        workLocationId: 1,
        shiftName: `SA role-link ${suffix}-${daysFromNow}`,
        startDatetime,
        endDatetime,
        shiftStatus: 'Open',
      },
    });
    expect(res.status()).toBe(201);
    const id = (await res.json()).shiftId as number;
    shiftIds.push(id);
    return id;
  };

  const createShiftRequiredJobRole = async (
    request: APIRequestContext,
    shiftId: number,
    jobRoleId: number,
  ): Promise<number> => {
    const res = await request.post(`${API_URL}/shiftrequiredjobroles`, {
      headers: authHeaders(adminToken),
      data: {
        shiftId,
        jobRoleId,
        requiredEmployeeCount: 1,
      },
    });
    expect(res.status()).toBe(201);
    const id = (await res.json()).shiftRequiredJobRoleId as number;
    shiftRequiredJobRoleIds.push(id);
    return id;
  };

  const createEmployeeJobRole = async (
    request: APIRequestContext,
    employeeId: number,
    jobRoleId: number,
  ): Promise<number> => {
    const res = await request.post(`${API_URL}/employeejobroles`, {
      headers: authHeaders(adminToken),
      data: {
        employeeId,
        jobRoleId,
        assignedDate: new Date().toISOString().slice(0, 10),
        proficiencyLevel: 'Junior',
      },
    });
    expect(res.status()).toBe(200);
    const id = (await res.json()).employeeJobRoleId as number;
    employeeJobRoleIds.push(id);
    return id;
  };

  const createShiftAssignment = async (
    request: APIRequestContext,
    shiftId: number,
    employeeId: number,
  ): Promise<{ status: number; shiftAssignmentId?: number }> => {
    const res = await request.post(`${API_URL}/shiftassignments`, {
      headers: authHeaders(adminToken),
      data: {
        shiftId,
        employeeId,
        assignmentStatus: 'Assigned',
        assignedDatetime: new Date().toISOString().slice(0, 19),
      },
    });

    if (res.status() === 201) {
      const id = (await res.json()).shiftAssignmentId as number;
      shiftAssignmentIds.push(id);
      return { status: 201, shiftAssignmentId: id };
    }

    return { status: res.status() };
  };

  test('assign succeeds when employee holds one of shift required roles', async ({ request }) => {
    const suffix = Date.now().toString(36);
    const employeeId = await createEmployee(request, suffix, 'rolematch');
    const jobRoleA = await createJobRole(request, 'A');
    const jobRoleB = await createJobRole(request, 'B');
    const shiftId = await createShift(request, suffix, 8);

    await createShiftRequiredJobRole(request, shiftId, jobRoleA);
    await createShiftRequiredJobRole(request, shiftId, jobRoleB);
    await createEmployeeJobRole(request, employeeId, jobRoleB);

    const result = await createShiftAssignment(request, shiftId, employeeId);
    expect(result.status).toBe(201);
  });

  test('assign fails when shift requires roles employee does not hold', async ({ request }) => {
    const suffix = (Date.now() + 1).toString(36);
    const employeeId = await createEmployee(request, suffix, 'norole');
    const requiredRoleId = await createJobRole(request, 'REQ');
    const shiftId = await createShift(request, suffix, 10);

    await createShiftRequiredJobRole(request, shiftId, requiredRoleId);

    const result = await createShiftAssignment(request, shiftId, employeeId);
    expect(result.status).toBe(400);
  });

  test('assign succeeds when shift has no required roles', async ({ request }) => {
    const suffix = (Date.now() + 2).toString(36);
    const employeeId = await createEmployee(request, suffix, 'noreq');
    const shiftId = await createShift(request, suffix, 12);

    const result = await createShiftAssignment(request, shiftId, employeeId);
    expect(result.status).toBe(201);
  });
});

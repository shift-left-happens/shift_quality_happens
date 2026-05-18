/**
 * Job Role — API integration tests
 *
 * These tests talk directly to the Spring Boot backend using Playwright's
 * request fixture. They validate the HTTP contract for /jobroles.
 *
 * Fully self-contained: all job roles are created and deleted by the suite itself.
 * The only seed assumption is a running admin account (admin@shift.dk).
 */

import { test, expect, type APIRequestContext } from '@playwright/test';

const API_URL = process.env.API_URL || 'http://localhost:8080';
const ADMIN_EMAIL = process.env.TEST_ADMIN_EMAIL || 'admin@shift.dk';
const ADMIN_PASSWORD = process.env.TEST_USER_PASSWORD || 'password123';
const TEST_MANAGER_PASSWORD = process.env.TEST_MANAGER_PASSWORD || 'TestPass123';
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

function randomLetters(length: number = 8): string {
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';
  return Array.from({ length }, () => alphabet[Math.floor(Math.random() * alphabet.length)]).join('');
}

test.describe.serial('Job Role API', () => {
  let adminToken: string;
  let managerToken: string;
  let employeeToken: string;
  let managerEmployeeId: number;
  let employeeEmployeeId: number;
  const jobRolesToCleanup = new Set<number>();
  const shiftsToCleanup = new Set<number>();
  const employeeJobRolesToCleanup = new Set<number>();
  const shiftRequiredJobRolesToCleanup = new Set<number>();

  test.beforeAll(async ({ request }) => {
    const adminLogin = await loginAndGetToken(request, ADMIN_EMAIL);
    adminToken = adminLogin.token;
    expect(
      ['Administrator', 'Manager'],
      `User ${ADMIN_EMAIL} must be Administrator or Manager to manage job roles`,
    ).toContain(adminLogin.roleName ?? '');

    const suffix = Date.now().toString(36);
    const createManagerResponse = await request.post(`${API_URL}/employees`, {
      headers: authHeaders(adminToken),
      data: {
        employeeNumber: `MGR-JR-${suffix}`,
        firstName: 'JobRole',
        lastName: 'Manager',
        userRole: 'Manager',
        email: `jobrole.manager.${suffix}@shifthappens.dk`,
        loginPassword: TEST_MANAGER_PASSWORD,
        phoneNumber: '+45 11122233',
        hireDate: '2026-01-15',
        birthDate: '1995-01-01',
        employmentStatus: 'ACTIVE',
        primaryWorkLocationId: 1,
      },
    });
    expect(createManagerResponse.status(), 'Create manager user for auth coverage').toBe(201);
    const createdManager = await createManagerResponse.json();
    managerEmployeeId = createdManager.employeeId as number;

    const managerLogin = await loginAndGetToken(
      request,
      `jobrole.manager.${suffix}@shifthappens.dk`,
      TEST_MANAGER_PASSWORD,
    );
    managerToken = managerLogin.token;
    expect(managerLogin.roleName, 'Temporary manager test user must have Manager role').toBe('Manager');

    const createEmployeeResponse = await request.post(`${API_URL}/employees`, {
      headers: authHeaders(adminToken),
      data: {
        employeeNumber: `EMP-JR-${suffix}`,
        firstName: 'JobRole',
        lastName: 'Employee',
        userRole: 'Employee',
        email: `jobrole.employee.${suffix}@shifthappens.dk`,
        loginPassword: TEST_EMPLOYEE_PASSWORD,
        phoneNumber: '+45 11122244',
        hireDate: '2026-01-15',
        birthDate: '1996-01-01',
        employmentStatus: 'ACTIVE',
        primaryWorkLocationId: 1,
      },
    });
    expect(createEmployeeResponse.status(), 'Create employee user for auth coverage').toBe(201);
    const createdEmployee = await createEmployeeResponse.json();
    employeeEmployeeId = createdEmployee.employeeId as number;

    const employeeLogin = await loginAndGetToken(
      request,
      `jobrole.employee.${suffix}@shifthappens.dk`,
      TEST_EMPLOYEE_PASSWORD,
    );
    employeeToken = employeeLogin.token;
    expect(employeeLogin.roleName, 'Temporary employee test user must have Employee role').toBe('Employee');
  });

  test.afterAll(async ({ request }) => {
    if (!adminToken) {
      return;
    }

    const deleteIfExists = async (url: string) => {
      const res = await request.delete(url, { headers: authHeaders(adminToken) });
      // 200/204 = deleted, 404 = already gone, 400 = dependency/permission during teardown.
      expect([200, 204, 400, 404]).toContain(res.status());
    };

    // Delete any job roles that weren't cleaned up by individual tests
    for (const linkId of Array.from(shiftRequiredJobRolesToCleanup).reverse()) {
      await deleteIfExists(`${API_URL}/shiftrequiredjobroles/${linkId}`);
    }
    for (const linkId of Array.from(employeeJobRolesToCleanup).reverse()) {
      await deleteIfExists(`${API_URL}/employeejobroles/${linkId}`);
    }
    for (const shiftId of Array.from(shiftsToCleanup).reverse()) {
      await deleteIfExists(`${API_URL}/shifts/${shiftId}`);
    }
    for (const jobRoleId of Array.from(jobRolesToCleanup).reverse()) {
      await deleteIfExists(`${API_URL}/jobroles/${jobRoleId}`);
    }

    if (managerEmployeeId) {
      const deleteManagerRes = await request.delete(`${API_URL}/employees/${managerEmployeeId}`, {
        headers: authHeaders(adminToken),
      });
      expect([204, 404]).toContain(deleteManagerRes.status());
    }

    if (employeeEmployeeId) {
      const deleteEmployeeRes = await request.delete(`${API_URL}/employees/${employeeEmployeeId}`, {
        headers: authHeaders(adminToken),
      });
      expect([204, 404]).toContain(deleteEmployeeRes.status());
    }
  });

  // ── BR-API-JR-01 ──────────────────────────────────────────────────────────

  test('BR-API-JR-01 — unauthenticated GET /jobroles returns 401', async ({ request }) => {
    const response = await request.get(`${API_URL}/jobroles`);
    expect(response.status()).toBe(401);
  });

  test('BR-API-JR-01 — unauthenticated POST /jobroles returns 401', async ({ request }) => {
    const response = await request.post(`${API_URL}/jobroles`, {
      data: {
        roleName: 'TestRole',
        jobRoleDescription: 'Test Description',
        isCertificationRequired: false,
      },
    });
    expect(response.status()).toBe(401);
  });

  // ── BR-API-JR-02 ──────────────────────────────────────────────────────────

  test('BR-API-JR-02 — authenticated admin can list job roles', async ({ request }) => {
    const response = await request.get(`${API_URL}/jobroles`, {
      headers: authHeaders(adminToken),
    });
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(Array.isArray(body)).toBe(true);
  });

  // ── BR-API-JR-03 / 04 / 05 / 06 — CRUD operations ─────────────────────────

  test.describe.serial('Job Role CRUD operations', () => {
    let createdJobRoleId: number;

    test('BR-API-JR-03 — admin can create a new job role', async ({ request }) => {
      const jobRoleName = `TestRole${randomLetters(8)}`;
      const response = await request.post(`${API_URL}/jobroles`, {
        headers: authHeaders(adminToken),
        data: {
          roleName: jobRoleName,
          jobRoleDescription: 'Test job role for API testing',
          isCertificationRequired: true,
        },
      });

      expect(response.status(), 'Create job role should return 201').toBe(201);
      const body = await response.json();
      expect(body).toHaveProperty('jobRoleId');
      expect(body.roleName).toBe(jobRoleName);
      expect(body.jobRoleDescription).toBe('Test job role for API testing');
      expect(body.isCertificationRequired).toBe(true);

      createdJobRoleId = body.jobRoleId as number;
      jobRolesToCleanup.add(createdJobRoleId);
    });

    test('BR-API-JR-04 — can retrieve a created job role by ID', async ({ request }) => {
      expect(createdJobRoleId).toBeDefined();

      const response = await request.get(`${API_URL}/jobroles/${createdJobRoleId}`, {
        headers: authHeaders(adminToken),
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.jobRoleId).toBe(createdJobRoleId);
      expect(body.isCertificationRequired).toBe(true);
    });

    test('BR-API-JR-05 — admin can update a job role', async ({ request }) => {
      expect(createdJobRoleId).toBeDefined();

      const response = await request.put(`${API_URL}/jobroles/${createdJobRoleId}`, {
        headers: authHeaders(adminToken),
        data: {
          roleName: `UpdatedRole${randomLetters(8)}`,
          jobRoleDescription: 'Updated description after creation',
          isCertificationRequired: false,
        },
      });

      expect(response.status()).toBe(200);
      const body = await response.json();
      expect(body.jobRoleId).toBe(createdJobRoleId);
      expect(body.isCertificationRequired).toBe(false);
      expect(body.jobRoleDescription).toBe('Updated description after creation');
    });

    test('BR-API-JR-06 — admin can delete a job role', async ({ request }) => {
      expect(createdJobRoleId).toBeDefined();

      const response = await request.delete(`${API_URL}/jobroles/${createdJobRoleId}`, {
        headers: authHeaders(adminToken),
      });

      expect(response.status()).toBe(204);
      jobRolesToCleanup.delete(createdJobRoleId);

      // Verify it's deleted
      const getResponse = await request.get(`${API_URL}/jobroles/${createdJobRoleId}`, {
        headers: authHeaders(adminToken),
      });
      expect([404, 500]).toContain(getResponse.status());
    });

    test('BR-API-JR-06B — manager can create, update and delete a job role', async ({ request }) => {
      const createdName = `ManagerRole${randomLetters(8)}`;
      const createResponse = await request.post(`${API_URL}/jobroles`, {
        headers: authHeaders(managerToken),
        data: {
          roleName: createdName,
          jobRoleDescription: 'Created by manager for authorization coverage',
          isCertificationRequired: false,
        },
      });

      expect(createResponse.status()).toBe(201);
      const created = await createResponse.json();
      const managerCreatedId = created.jobRoleId as number;
      jobRolesToCleanup.add(managerCreatedId);

      const updateResponse = await request.put(`${API_URL}/jobroles/${managerCreatedId}`, {
        headers: authHeaders(managerToken),
        data: {
          roleName: `ManagerUpdated${randomLetters(8)}`,
          jobRoleDescription: 'Updated by manager',
          isCertificationRequired: true,
        },
      });

      expect(updateResponse.status()).toBe(200);
      const updated = await updateResponse.json();
      expect(updated.jobRoleId).toBe(managerCreatedId);
      expect(updated.isCertificationRequired).toBe(true);

      const deleteResponse = await request.delete(`${API_URL}/jobroles/${managerCreatedId}`, {
        headers: authHeaders(managerToken),
      });

      expect(deleteResponse.status()).toBe(204);
      jobRolesToCleanup.delete(managerCreatedId);
    });
  });

  // ── BR-API-JR-07 ──────────────────────────────────────────────────────────

  test('BR-API-JR-07 — get non-existent job role returns 404', async ({ request }) => {
    const response = await request.get(`${API_URL}/jobroles/999999`, {
      headers: authHeaders(adminToken),
    });

    expect([404]).toContain(response.status());
  });

  test('BR-API-JR-07 — delete non-existent job role returns 404', async ({ request }) => {
    const response = await request.delete(`${API_URL}/jobroles/999999`, {
      headers: authHeaders(adminToken),
    });

    expect([404]).toContain(response.status());
  });

  // ── BR-API-JR-08 — employee cannot write job roles ──────────────────────

  test('BR-API-JR-08 — employee cannot create/update/delete job roles (403)', async ({ request }) => {
    const roleName = `EmployeeBlocked${randomLetters(8)}`;

    const employeeCreate = await request.post(`${API_URL}/jobroles`, {
      headers: authHeaders(employeeToken),
      data: {
        roleName,
        jobRoleDescription: 'Should be forbidden for Employee role',
        isCertificationRequired: false,
      },
    });
    expect(employeeCreate.status()).toBe(403);

    const adminCreate = await request.post(`${API_URL}/jobroles`, {
      headers: authHeaders(adminToken),
      data: {
        roleName: `Writable${randomLetters(8)}`,
        jobRoleDescription: 'Temp role for employee forbidden update/delete checks',
        isCertificationRequired: false,
      },
    });
    expect(adminCreate.status()).toBe(201);
    const tempRoleId = (await adminCreate.json()).jobRoleId as number;
    jobRolesToCleanup.add(tempRoleId);

    const employeeUpdate = await request.put(`${API_URL}/jobroles/${tempRoleId}`, {
      headers: authHeaders(employeeToken),
      data: {
        roleName: `StillBlocked${randomLetters(8)}`,
        jobRoleDescription: 'Should stay forbidden',
        isCertificationRequired: true,
      },
    });
    expect(employeeUpdate.status()).toBe(403);

    const employeeDelete = await request.delete(`${API_URL}/jobroles/${tempRoleId}`, {
      headers: authHeaders(employeeToken),
    });
    expect(employeeDelete.status()).toBe(403);
  });

  // ── BR-API-JR-09 — roleName validation ──────────────────────────────────

  test('BR-API-JR-09 — roleName validation rejects short, long, invalid and blank values', async ({ request }) => {
    const invalidPayloads = [
      {
        roleName: 'A',
        jobRoleDescription: 'Too short roleName',
        isCertificationRequired: false,
      },
      {
        roleName: 'A'.repeat(51),
        jobRoleDescription: 'Too long roleName',
        isCertificationRequired: false,
      },
      {
        roleName: 'Role123',
        jobRoleDescription: 'Contains digits',
        isCertificationRequired: false,
      },
      {
        roleName: '   ',
        jobRoleDescription: 'Blank after trim',
        isCertificationRequired: false,
      },
    ];

    for (const payload of invalidPayloads) {
      const response = await request.post(`${API_URL}/jobroles`, {
        headers: authHeaders(adminToken),
        data: payload,
      });
      expect(response.status()).toBe(400);
    }
  });

  // ── BR-API-JR-10 — duplicate name (case-insensitive) ────────────────────

  test('BR-API-JR-10 — duplicate roleName is rejected case-insensitively', async ({ request }) => {
    const uniqueBase = `DupRole${randomLetters(8)}`;

    const createFirst = await request.post(`${API_URL}/jobroles`, {
      headers: authHeaders(adminToken),
      data: {
        roleName: uniqueBase,
        jobRoleDescription: 'First create for duplicate test',
        isCertificationRequired: false,
      },
    });
    expect(createFirst.status()).toBe(201);
    const firstId = (await createFirst.json()).jobRoleId as number;
    jobRolesToCleanup.add(firstId);

    const createDuplicate = await request.post(`${API_URL}/jobroles`, {
      headers: authHeaders(adminToken),
      data: {
        roleName: uniqueBase.toLowerCase(),
        jobRoleDescription: 'Duplicate with different casing',
        isCertificationRequired: false,
      },
    });
    expect(createDuplicate.status()).toBe(400);
  });

  // ── BR-API-JR-11 — required certification flag ──────────────────────────

  test('BR-API-JR-11 — missing isCertificationRequired returns 400', async ({ request }) => {
    const response = await request.post(`${API_URL}/jobroles`, {
      headers: authHeaders(adminToken),
      data: {
        roleName: `MissingCert${randomLetters(8)}`,
        jobRoleDescription: 'Missing required certification flag',
      },
    });
    expect(response.status()).toBe(400);
  });

  // ── BR-API-JR-12 — deletion blocked by dependencies ─────────────────────

  test('BR-API-JR-12 — delete is blocked when role is held by an employee', async ({ request }) => {
    const createRole = await request.post(`${API_URL}/jobroles`, {
      headers: authHeaders(adminToken),
      data: {
        roleName: `HeldByEmp${randomLetters(8)}`,
        jobRoleDescription: 'Delete blocked by employeejobrole dependency',
        isCertificationRequired: false,
      },
    });
    expect(createRole.status()).toBe(201);
    const jobRoleId = (await createRole.json()).jobRoleId as number;
    jobRolesToCleanup.add(jobRoleId);

    const createEmployeeJobRole = await request.post(`${API_URL}/employeejobroles`, {
      headers: authHeaders(adminToken),
      data: {
        employeeId: employeeEmployeeId,
        jobRoleId,
        assignedDate: new Date().toISOString().slice(0, 10),
        proficiencyLevel: 'Junior',
      },
    });
    expect(createEmployeeJobRole.status()).toBe(200);
    const linkId = (await createEmployeeJobRole.json()).employeeJobRoleId as number;
    employeeJobRolesToCleanup.add(linkId);

    const deleteResponse = await request.delete(`${API_URL}/jobroles/${jobRoleId}`, {
      headers: authHeaders(adminToken),
    });
    expect(deleteResponse.status()).toBe(400);
    const msg = await deleteResponse.text();
    expect(msg.toLowerCase()).toContain('employee');
  });

  test('BR-API-JR-12 — delete is blocked when role is required by a shift', async ({ request }) => {
    const createRole = await request.post(`${API_URL}/jobroles`, {
      headers: authHeaders(adminToken),
      data: {
        roleName: `ReqByShift${randomLetters(8)}`,
        jobRoleDescription: 'Delete blocked by shiftrequiredjobrole dependency',
        isCertificationRequired: false,
      },
    });
    expect(createRole.status()).toBe(201);
    const jobRoleId = (await createRole.json()).jobRoleId as number;
    jobRolesToCleanup.add(jobRoleId);

    const start = new Date(Date.now() + 9 * 24 * 60 * 60 * 1000);
    const end = new Date(start.getTime() + 8 * 60 * 60 * 1000);
    const fmt = (d: Date) => d.toISOString().slice(0, 19);
    const createShift = await request.post(`${API_URL}/shifts`, {
      headers: authHeaders(adminToken),
      data: {
        departmentId: 1,
        workLocationId: 1,
        shiftName: `JR dependency ${randomLetters(6)}`,
        startDatetime: fmt(start),
        endDatetime: fmt(end),
        shiftStatus: 'Open',
      },
    });
    expect(createShift.status()).toBe(201);
    const shiftId = (await createShift.json()).shiftId as number;
    shiftsToCleanup.add(shiftId);

    const createShiftRequiredRole = await request.post(`${API_URL}/shiftrequiredjobroles`, {
      headers: authHeaders(adminToken),
      data: {
        shiftId,
        jobRoleId,
        requiredEmployeeCount: 1,
      },
    });
    expect(createShiftRequiredRole.status()).toBe(201);
    const linkId = (await createShiftRequiredRole.json()).shiftRequiredJobRoleId as number;
    shiftRequiredJobRolesToCleanup.add(linkId);

    const deleteResponse = await request.delete(`${API_URL}/jobroles/${jobRoleId}`, {
      headers: authHeaders(adminToken),
    });
    expect(deleteResponse.status()).toBe(400);
    const msg = await deleteResponse.text();
    expect(msg.toLowerCase()).toContain('shift');
  });
});

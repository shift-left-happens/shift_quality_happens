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
  const jobRolesToCleanup = new Set<number>();

  test.beforeAll(async ({ request }) => {
    const adminLogin = await loginAndGetToken(request, ADMIN_EMAIL);
    adminToken = adminLogin.token;
    expect(
      ['Administrator', 'Manager'],
      `User ${ADMIN_EMAIL} must be Administrator or Manager to manage job roles`,
    ).toContain(adminLogin.roleName ?? '');
  });

  test.afterAll(async ({ request }) => {
    if (!adminToken) {
      return;
    }

    const deleteIfExists = async (url: string) => {
      const res = await request.delete(url, { headers: authHeaders(adminToken) });
      // 204 = deleted, 404 = already gone, 400/403 = dependency/permission during teardown.
      expect([204, 400, 403, 404]).toContain(res.status());
    };

    // Delete any job roles that weren't cleaned up by individual tests
    for (const jobRoleId of Array.from(jobRolesToCleanup).reverse()) {
      await deleteIfExists(`${API_URL}/jobroles/${jobRoleId}`);
    }
  });

  // ── BR-API-JR-01 ──────────────────────────────────────────────────────────

  test('BR-API-JR-01 — unauthenticated GET /jobroles returns 403', async ({ request }) => {
    const response = await request.get(`${API_URL}/jobroles`);
    expect(response.status()).toBe(403);
  });

  test('BR-API-JR-01 — unauthenticated POST /jobroles returns 403', async ({ request }) => {
    const response = await request.post(`${API_URL}/jobroles`, {
      data: {
        roleName: 'TestRole',
        jobRoleDescription: 'Test Description',
        isCertificationRequired: false,
      },
    });
    expect(response.status()).toBe(403);
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
});

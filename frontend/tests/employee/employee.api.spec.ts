import { test, expect } from '@playwright/test';

test.describe('Employee API', () => {
  test.describe.configure({ mode: 'serial' });

  const api_url = process.env.API_URL || 'http://localhost:8080';
  const email = process.env.TEST_ADMIN_EMAIL || 'admin@shift.dk';
  const password = process.env.TEST_USER_PASSWORD || 'password123';
  let adminToken: string;

  const getResponseDetails = async (response: { status: () => number; text: () => Promise<string> }) => {
    const text = await response.text();
    return `status=${response.status()} body=${text || '<empty>'}`;
  };

  const buildEmployeePayload = (overrides: Record<string, unknown> = {}) => {
    const now = Date.now();
    return {
      firstName: 'API',
      lastName: 'TestUser',
      employeeNumber: `EMP-${now}`,
      email: `api.test.${now}@hospital.dk`,
      loginPassword: 'Password123',
      birthDate: '1995-01-01',
      hireDate: '2024-01-01',
      employmentStatus: 'ACTIVE',
      userRole: 'Employee',
      primaryWorkLocationId: 1,
      phoneNumber: '12345678',
      ...overrides
    };
  };

  const authHeader = () => ({ 'Authorization': `Bearer ${adminToken}` });

  test.beforeAll(async ({ request }) => {
    const loginResponse = await request.post(`${api_url}/auth/login`, {
      data: {
        email: email,
        password: password
      }
    });
    expect(loginResponse.status(), `Expected successful login for ${email}. ${await getResponseDetails(loginResponse)}`).toBe(200);
    const body = await loginResponse.json();
    expect(['Administrator', 'Manager']).toContain(body.roleName);
    adminToken = body.token;
    expect(adminToken).toBeTruthy();
  });

  test('should perform full employee lifecycle via API', async ({ request }) => {
    // 1. Create Employee
    const createResponse = await request.post(`${api_url}/employees`, {
      headers: authHeader(),
      data: buildEmployeePayload()
    });
    expect(createResponse.status()).toBe(201);
    const employee = await createResponse.json();
    const employeeId = employee.employeeId;
    expect(employeeId).toBeDefined();

    // 2. Get Employee
    const getResponse = await request.get(`${api_url}/employees/${employeeId}`, {
      headers: authHeader()
    });
    expect(getResponse.status()).toBe(200);
    expect((await getResponse.json()).firstName).toBe('API');

    // 3. Patch Employee
    const patchResponse = await request.patch(`${api_url}/employees/${employeeId}`, {
      headers: authHeader(),
      data: {
        firstName: 'APIUpdated',
        phoneNumber: '+45 12345678'
      }
    });
    expect(patchResponse.status()).toBe(200);
    expect((await patchResponse.json()).firstName).toBe('APIUpdated');

    // 4. Validation: Invalid Phone Number
    const invalidPhoneResponse = await request.patch(`${api_url}/employees/${employeeId}`, {
      headers: authHeader(),
      data: { phoneNumber: 'invalid' }
    });
    expect(invalidPhoneResponse.status()).toBe(400);

    // 5. Delete Employee
    const deleteResponse = await request.delete(`${api_url}/employees/${employeeId}`, {
      headers: authHeader()
    });
    expect(deleteResponse.status()).toBe(204); // 204 No Content

    // 6. Verify Deletion
    const verifyDeleteResponse = await request.get(`${api_url}/employees/${employeeId}`, {
      headers: authHeader()
    });
    expect(verifyDeleteResponse.status()).toBe(404);
  });

  test('should enforce phone number decision table for create', async ({ request }) => {
    const validPhones = ['1234', '123456789012345', '+45 12345678', '+998 123456789012'];
    for (const phone of validPhones) {
      const response = await request.post(`${api_url}/employees`, {
        headers: authHeader(),
        data: buildEmployeePayload({ phoneNumber: phone })
      });
      expect(response.status(), `Expected valid phone '${phone}'`).toBe(201);
      const created = await response.json();
      await request.delete(`${api_url}/employees/${created.employeeId}`, { headers: authHeader() });
    }

    const invalidPhones = ['123', '++12 123456', '+12345678', '+ 123456', '123aaa', '+45  1234', ''];
    for (const phone of invalidPhones) {
      const response = await request.post(`${api_url}/employees`, {
        headers: authHeader(),
        data: buildEmployeePayload({ phoneNumber: phone })
      });
      expect(response.status(), `Expected invalid phone '${phone}'`).toBe(400);
    }
  });

  test('should enforce age boundaries from birth date', async ({ request }) => {
    const validBoundary = await request.post(`${api_url}/employees`, {
      headers: authHeader(),
      data: buildEmployeePayload({ birthDate: '2010-01-01' })
    });
    expect(validBoundary.status()).toBe(201);
    const created = await validBoundary.json();
    await request.delete(`${api_url}/employees/${created.employeeId}`, { headers: authHeader() });

    const tooYoung = await request.post(`${api_url}/employees`, {
      headers: authHeader(),
      data: buildEmployeePayload({ birthDate: '2011-01-01' })
    });
    expect(tooYoung.status()).toBe(400);

    const tooOld = await request.post(`${api_url}/employees`, {
      headers: authHeader(),
      data: buildEmployeePayload({ birthDate: '1925-01-01' })
    });
    expect(tooOld.status()).toBe(400);

    const futureBirthDate = await request.post(`${api_url}/employees`, {
      headers: authHeader(),
      data: buildEmployeePayload({ birthDate: '2030-01-01' })
    });
    expect(futureBirthDate.status()).toBe(400);
  });

  test('should enforce email format and uniqueness', async ({ request }) => {
    const sharedEmail = `employee.dup.${Date.now()}@hospital.dk`;

    const firstCreate = await request.post(`${api_url}/employees`, {
      headers: authHeader(),
      data: buildEmployeePayload({ email: sharedEmail })
    });
    expect(firstCreate.status(), await getResponseDetails(firstCreate)).toBe(201);
    const firstCreatedEmployee = await firstCreate.json();

    const duplicate = await request.post(`${api_url}/employees`, {
      headers: authHeader(),
      data: buildEmployeePayload({ email: sharedEmail })
    });
    expect(duplicate.status()).toBe(400);

    const invalidFormats = ['@@das', 'a@a', '@aaa.dk', 'aaa@a'];
    for (const invalidEmail of invalidFormats) {
      const response = await request.post(`${api_url}/employees`, {
        headers: authHeader(),
        data: buildEmployeePayload({ email: invalidEmail })
      });
      expect(response.status(), `Expected invalid email '${invalidEmail}'`).toBe(400);
    }

    await request.delete(`${api_url}/employees/${firstCreatedEmployee.employeeId}`, { headers: authHeader() });
  });

  test('should enforce password composition and length constraints', async ({ request }) => {
    const valid = await request.post(`${api_url}/employees`, {
      headers: authHeader(),
      data: buildEmployeePayload({ loginPassword: 'Passw0rd' })
    });
    expect(valid.status()).toBe(201);
    const created = await valid.json();
    await request.delete(`${api_url}/employees/${created.employeeId}`, { headers: authHeader() });

    const invalidPasswords = ['pass', 'password', 'PASSWORD', 'Password', 'P1a'];
    for (const candidate of invalidPasswords) {
      const response = await request.post(`${api_url}/employees`, {
        headers: authHeader(),
        data: buildEmployeePayload({ loginPassword: candidate })
      });
      expect(response.status(), `Expected invalid password '${candidate}'`).toBe(400);
    }
  });

  test('should validate employment status and hire date formats', async ({ request }) => {
    const invalidStatus = await request.post(`${api_url}/employees`, {
      headers: authHeader(),
      data: buildEmployeePayload({ employmentStatus: 'PENDING' })
    });
    expect(invalidStatus.status()).toBe(400);

    const invalidHireDateFormat = await request.post(`${api_url}/employees`, {
      headers: authHeader(),
      data: buildEmployeePayload({ hireDate: '12-05-2024' })
    });
    expect(403).toBe(invalidHireDateFormat.status());

    const nonExistentHireDate = await request.post(`${api_url}/employees`, {
      headers: authHeader(),
      data: buildEmployeePayload({ hireDate: '2023-02-29' })
    });
    expect(403).toBe(nonExistentHireDate.status());
  });
});

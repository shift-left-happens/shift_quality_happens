import { test, expect } from '@playwright/test';

test.describe('Auth API Integration', () => {
  const email = process.env.TEST_USER_EMAIL || 'sofie.jensen@hospital.dk';
  const password = process.env.TEST_USER_PASSWORD || 'password123';
  const api_url = process.env.API_URL || '';

  test('should issue a token for valid login and reject invalid login', async ({ request }) => {
    // 1. Valid credentials return 200 with a token
    const validResponse = await request.post(`${api_url}/auth/login`, {
      data: { email, password },
    });
    expect(validResponse.ok()).toBeTruthy();
    const validBody = await validResponse.json();
    expect(validBody).toHaveProperty('token');
    expect(validBody.email).toBe(email);

    // 2. Invalid credentials return 401 with an error message
    const invalidResponse = await request.post(`${api_url}/auth/login`, {
      data: { email: 'invalid@user.dk', password: 'wrongpassword' },
    });
    expect(invalidResponse.status()).toBe(401);
    const invalidBody = await invalidResponse.json();
    expect(invalidBody.error).toBe('Invalid email or password');
  });
});

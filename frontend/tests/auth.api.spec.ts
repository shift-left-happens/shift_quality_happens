import { test, expect } from '@playwright/test';

test.describe('Auth API Integration', () => {
  const email = process.env.TEST_USER_EMAIL || 'sofie.jensen@hospital.dk';
  const password = process.env.TEST_USER_PASSWORD || 'password123';
  const api_url = process.env.API_URL || '';

  test('should return 200 and token on valid login', async ({ request }) => {
    const response = await request.post(`${api_url}/auth/login`, {
      data: {
        email: email,
        password: password
      }
    });

    expect(response.ok()).toBeTruthy();
    const body = await response.json();
    expect(body).toHaveProperty('token');
    expect(body.email).toBe(email);
  });

  test('should return 401 on invalid login', async ({ request }) => {
    const response = await request.post(`${api_url}/auth/login`, {
      data: {
        email: 'invalid@user.dk',
        password: 'wrongpassword'
      }
    });

    expect(response.status()).toBe(401);
    const body = await response.json();
    expect(body.error).toBe('Invalid email or password');
  });
});

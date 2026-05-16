import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/LoginPage';

test.describe('Login E2E', () => {
  test('should redirect unauthenticated user from protected route to login', async ({ page, request, baseURL }) => {
    await expect
      .poll(
        async () => {
          const res = await request.get(`${baseURL}/login`);
          return res.status();
        },
        {
          timeout: 15000,
          intervals: [250, 500, 1000],
        },
      )
      .toBe(200);

    await page.goto('/');
    await expect(page).toHaveURL(/\/login/);
  });

  test('should login successfully with valid credentials', async ({ page }) => {
    const loginPage = new LoginPage(page);
    
    const email = process.env.TEST_USER_EMAIL || process.env.TEST_ADMIN_EMAIL || 'admin@shift.dk';
    const password = process.env.TEST_USER_PASSWORD || 'password123';

    await loginPage.goto();
    await loginPage.login(email, password);

    // App should leave /login after successful auth.
    await expect(page).not.toHaveURL(/\/login/);
  });

  test('should show error with invalid credentials', async ({ page }) => {
    const loginPage = new LoginPage(page);

    await loginPage.goto();
    await loginPage.login('wrong@email.dk', 'wrongpassword');

    await expect(loginPage.errorMessage).toBeVisible();
    await expect(loginPage.errorMessage).toContainText('Invalid email or password');
  });
});

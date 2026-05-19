import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/LoginPage';

test.describe('Login E2E', () => {
  const email = process.env.TEST_USER_EMAIL || 'sofie.jensen@hospital.dk';
  const password = process.env.TEST_USER_PASSWORD || 'password123';

  test('should accept valid credentials and reject invalid ones', async ({ page }) => {
    const loginPage = new LoginPage(page);

    // 1. Valid login lands on the dashboard
    await loginPage.goto();
    await loginPage.login(email, password);
    await expect(page).toHaveURL('/');

    // 2. Clear the session so the login page is reachable again
    await page.evaluate(() => localStorage.clear());

    // 3. Invalid login keeps the user on the login page with an error
    await loginPage.goto();
    await loginPage.login('wrong@email.dk', 'wrongpassword');
    await expect(loginPage.errorMessage).toBeVisible();
    await expect(loginPage.errorMessage).toContainText('Invalid email or password');
  });
});

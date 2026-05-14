import { test, expect } from '@playwright/test';
import { LoginPage } from './pages/LoginPage';

test.describe('Login E2E', () => {
  test('should login successfully with valid credentials', async ({ page }) => {
    const loginPage = new LoginPage(page);
    
    const email = process.env.TEST_USER_EMAIL || 'sofie.jensen@hospital.dk';
    const password = process.env.TEST_USER_PASSWORD || 'password123';

    await loginPage.goto();
    await loginPage.login(email, password);

    // Expect to be redirected to home or dashboard
    await expect(page).toHaveURL('/');
  });

  test('should show error with invalid credentials', async ({ page }) => {
    const loginPage = new LoginPage(page);

    await loginPage.goto();
    await loginPage.login('wrong@email.dk', 'wrongpassword');

    await expect(loginPage.errorMessage).toBeVisible();
    await expect(loginPage.errorMessage).toContainText('Invalid email or password');
  });
});

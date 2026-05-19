import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/LoginPage';
import { EmployeePage } from '../pages/EmployeePage';
import { ShiftPage } from '../pages/ShiftPage';
import { ShiftAssignmentPage } from '../pages/ShiftAssignmentPage';

function generateRandomString(length: number): string {
  const characters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';
  let result = '';
  for (let i = 0; i < length; i++) {
    result += characters[Math.floor(Math.random() * characters.length)];
  }
  return result;
}

test.describe('Employee E2E', () => {
  const email = 'admin@shift.dk';
  const password = 'password123';

  test('should block deletion of employee with future shifts and allow after deletion', async ({ page }) => {
    const loginPage = new LoginPage(page);
    const employeePage = new EmployeePage(page);
    const shiftPage = new ShiftPage(page);
    const assignmentPage = new ShiftAssignmentPage(page);

    // 1. Login
    await loginPage.goto();
    await loginPage.login(email, password);
    await expect(page).toHaveURL('/');

    // 2. Create employee
    const randomSuffix = generateRandomString(10);
    const employeeNumber = `EMP-${Date.now()}`;
    const firstName = `Employee${randomSuffix}`;
    const lastName = `User${randomSuffix}`;
    const testEmployeeEmail = `e2e.test.${Date.now()}@hospital.dk`;

    await employeePage.goto();
    await employeePage.createEmployee({
      employeeNumber,
      firstName,
      lastName,
      email: testEmployeeEmail,
      loginPassword: 'Password123',
      birthDate: '1990-01-01',
      hireDate: '2024-01-01',
      phoneNumber: '12345678'
    });

    await expect(page).toHaveURL('/employees');
    await expect(page.locator('table')).toContainText(testEmployeeEmail);

    // 3. Create shift
    const shiftName = `E2E Future Shift ${Date.now()}`;
    const tomorrow = new Date(Date.now() + 86400000);
    const startStr = tomorrow.toISOString().slice(0, 16);
    const endStr = new Date(tomorrow.getTime() + 3600000).toISOString().slice(0, 16);

    await shiftPage.goto();
    await shiftPage.createShift({
      shiftName,
      start: startStr,
      end: endStr
    });

    await expect(page).toHaveURL(/\/shifts(\/new)?$/);

    // 4. Assign shift
    await assignmentPage.goto();
    await assignmentPage.createAssignment(`${firstName} ${lastName}`, shiftName);

    await expect(page).toHaveURL('/shift-assignments');

    // =========================================================
    // 5. FIRST DELETE ATTEMPT (2 dialogs expected)
    // =========================================================

    await employeePage.goto();

    const dialogs: Array<{ type(): string; message(): string }> = [];

    page.on('dialog', async dialog => {
      dialogs.push(dialog);
      await dialog.accept();
    });

    const row = page.locator('tr', { hasText: testEmployeeEmail });

    await row.getByRole('button', { name: 'Delete' }).click();

    // ✅ Wait until both dialogs have actually happened
    await expect.poll(() => dialogs.length).toBe(2);

    // ASSERTIONS
    expect(dialogs[0].type()).toBe('confirm');

    expect(dialogs[1].message()).toContain(
        'Employee has future assigned shifts and cannot be deleted'
    );

    page.removeAllListeners('dialog');

    // =========================================================
    // 6. REMOVE SHIFT ASSIGNMENT
    // =========================================================

    await assignmentPage.deleteAssignment(`${firstName} ${lastName}`);

    // =========================================================
    // 7. SECOND DELETE ATTEMPT (success case)
    // =========================================================

    await employeePage.goto();

    const rowAfterFix = page.locator('tr', { hasText: testEmployeeEmail });

    page.once('dialog', dialog => dialog.accept());

    await rowAfterFix.getByRole('button', { name: 'Delete' }).click();

    await expect(page.locator('table')).not.toContainText(testEmployeeEmail);
  });
});
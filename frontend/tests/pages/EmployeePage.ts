import { Page, Locator } from '@playwright/test';

export class EmployeePage {
  readonly page: Page;
  readonly newButton: Locator;
  readonly firstNameInput: Locator;
  readonly lastNameInput: Locator;
  readonly emailInput: Locator;
  readonly passwordInput: Locator;
  readonly phoneNumberInput: Locator;
  readonly birthDateInput: Locator;
  readonly hireDateInput: Locator;
  readonly submitButton: Locator;
  readonly deleteButton: Locator;
  private primaryWorkLocation: Locator;
  readonly employeeNumber: Locator;

  constructor(page: Page) {
    this.page = page;
    this.newButton = page.getByRole('link', { name: 'New employee' });
    this.firstNameInput = page.locator('input[name="firstName"]');
    this.lastNameInput = page.locator('input[name="lastName"]');
    this.employeeNumber = page.locator('input[name="employeeNumber"]');
    this.emailInput = page.locator('input[name="email"]');
    this.passwordInput = page.locator('input[name="loginPassword"]');
    this.phoneNumberInput = page.locator('input[name="phoneNumber"]');
    this.birthDateInput = page.locator('input[name="birthDate"]');
    this.hireDateInput = page.locator('input[name="hireDate"]');
    this.primaryWorkLocation = page.locator('select[name="primaryWorkLocationId"]');
    this.submitButton = page.getByRole('button', { name: /Create|Save changes/ });
    this.deleteButton = page.getByRole('button', { name: 'Delete' });
  }

  async goto() {
    await this.page.goto('/employees');
  }

  async createEmployee(data: any) {
    // Wait until button is actually visible and clickable
    await this.newButton.waitFor({ state: 'visible' });

    // If clicking navigates to another page/view,
    // wait for the URL change together with the click
    await Promise.all([
      this.page.waitForURL('**/employees/new'), // adjust this URL pattern
      this.newButton.click(),
    ]);

    // Wait for form to be ready
    await this.firstNameInput.waitFor({ state: 'visible' });

    await this.firstNameInput.fill(data.firstName);
    await this.lastNameInput.fill(data.lastName);
    await this.emailInput.fill(data.email);

    await this.employeeNumber.fill(data.employeeNumber);

    if (data.loginPassword) {
      await this.passwordInput.fill(data.loginPassword);
    }

    if (data.phoneNumber) {
      await this.phoneNumberInput.fill(data.phoneNumber);
    }

    await this.birthDateInput.fill(data.birthDate);
    await this.hireDateInput.fill(data.hireDate);
    await this.primaryWorkLocation.selectOption({index: 1});
    await this.submitButton.click();
  }
}

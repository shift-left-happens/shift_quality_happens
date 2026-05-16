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

  constructor(page: Page) {
    this.page = page;
    this.newButton = page.getByRole('link', { name: 'New employee' });
    this.firstNameInput = page.locator('input[name="firstName"]');
    this.lastNameInput = page.locator('input[name="lastName"]');
    this.emailInput = page.locator('input[name="email"]');
    this.passwordInput = page.locator('input[name="loginPassword"]');
    this.phoneNumberInput = page.locator('input[name="phoneNumber"]');
    this.birthDateInput = page.locator('input[name="birthDate"]');
    this.hireDateInput = page.locator('input[name="hireDate"]');
    this.submitButton = page.getByRole('button', { name: /Create|Save changes/ });
    this.deleteButton = page.getByRole('button', { name: 'Delete' });
  }

  async goto() {
    await this.page.goto('/employees');
  }

  async createEmployee(data: any) {
    await this.newButton.click();
    await this.firstNameInput.fill(data.firstName);
    await this.lastNameInput.fill(data.lastName);
    await this.emailInput.fill(data.email);
    if (data.loginPassword) {
      await this.passwordInput.fill(data.loginPassword);
    }
    if (data.phoneNumber) {
      await this.phoneNumberInput.fill(data.phoneNumber);
    }
    await this.birthDateInput.fill(data.birthDate);
    await this.hireDateInput.fill(data.hireDate);
    await this.submitButton.click();
  }
}

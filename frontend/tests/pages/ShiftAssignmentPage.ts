import { Page, Locator } from '@playwright/test';

export class ShiftAssignmentPage {
  readonly page: Page;
  readonly newButton: Locator;
  readonly employeeSelect: Locator;
  readonly shiftSelect: Locator;
  readonly statusSelect: Locator;
  readonly submitButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.newButton = page.getByRole('link', { name: 'New assignment' });
    this.employeeSelect = page.locator('label:has-text("Employee") select');
    this.shiftSelect = page.locator('label:has-text("Shift") select');
    this.statusSelect = page.locator('label:has-text("Status") select');
    this.submitButton = page.getByRole('button', { name: 'Create' });
  }

  async goto() {
    await this.page.goto('/shift-assignments');
  }

  async createAssignment(employeeName: string, shiftName: string) {
    await this.newButton.click();
    await this.employeeSelect.selectOption({ label: new RegExp(employeeName) });
    await this.shiftSelect.selectOption({ label: new RegExp(shiftName) });
    await this.submitButton.click();
  }

  async deleteAssignment(employeeName: string) {
    await this.goto();
    const row = this.page.locator('tr', { hasText: employeeName });
    this.page.once('dialog', dialog => dialog.accept());
    await row.getByRole('button', { name: 'Delete' }).click();
  }
}

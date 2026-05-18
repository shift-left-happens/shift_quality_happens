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
    // this.employeeSelect = page.locator('label:has-text("Employee") select');
    // this.shiftSelect = page.locator('label:has-text("Shift") select');
    // this.statusSelect = page.locator('label:has-text("Status") select');
    this.employeeSelect = page.locator('select[name="employeeId"]');
    this.shiftSelect = page.locator('select[name="shiftId"]');
    this.statusSelect = page.locator('select[name="assignmentStatus"]');
    this.submitButton = page.getByRole('button', { name: 'Create' });
  }

  async goto() {
    await this.page.goto('/shift-assignments');
  }

  async createAssignment(employeeName: string, shiftName: string) {
    await this.newButton.click();
    await this.employeeSelect.waitFor({ state: 'visible' });
    await this.shiftSelect.waitFor({ state: 'visible' });
    await this.selectOptionByPartialLabel(this.employeeSelect, employeeName);
    await this.selectOptionByPartialLabel(this.shiftSelect, shiftName);
    await this.submitButton.click();
  }

  private async selectOptionByPartialLabel(select: Locator, text: string) {
    const normalized = text.trim().toLowerCase();
    const timeoutMs = 10000;
    const pollMs = 50;
    const deadline = Date.now() + timeoutMs;

    while (Date.now() < deadline) {
      const options = await select.locator('option').all();

      for (const option of options) {
        const label = (await option.innerText()).trim().toLowerCase();
        const value = (await option.getAttribute('value'))?.trim();

        if (value && label.includes(normalized)) {
          await select.selectOption(value);
          return;
        }
      }

      await this.page.waitForTimeout(pollMs);
    }

    throw new Error(`Option containing "${text}" was not found`);
  }

  async deleteAssignment(employeeName: string) {
    await this.goto();
    const row = this.page.locator('tr', { hasText: employeeName });
    this.page.once('dialog', dialog => dialog.accept());
    await row.getByRole('button', { name: 'Delete' }).click();
  }
}

import { Page, Locator } from '@playwright/test';

export class ShiftPage {
  readonly page: Page;
  readonly newButton: Locator;
  readonly shiftNameInput: Locator;
  readonly departmentSelect: Locator;
  readonly locationSelect: Locator;
  readonly startInput: Locator;
  readonly endInput: Locator;
  readonly submitButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.newButton = page.getByRole('link', { name: 'New shift' });
    this.shiftNameInput = page.locator('label:has-text("Shift name") input');
    this.departmentSelect = page.locator('label:has-text("Department") select');
    this.locationSelect = page.locator('label:has-text("Work location") select');
    this.startInput = page.locator('label:has-text("Start") input');
    this.endInput = page.locator('label:has-text("End") input');
    this.submitButton = page.getByRole('button', { name: 'Create' });
  }

  async goto() {
    await this.page.goto('/shifts');
  }

  async createShift(data: {
    shiftName: string;
    departmentId?: number;
    locationId?: number;
    start: string;
    end: string;
  }) {
    await this.newButton.click();
    await this.shiftNameInput.fill(data.shiftName);
    if (data.departmentId) {
        await this.departmentSelect.selectOption({ index: 1 }); // Just pick the first available for now if not specified or try to match
    }
    if (data.locationId) {
        await this.locationSelect.selectOption({ index: 1 });
    }
    await this.startInput.fill(data.start);
    await this.endInput.fill(data.end);
    await this.submitButton.click();
  }
}

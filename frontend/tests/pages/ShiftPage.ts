import { Page, Locator } from '@playwright/test';

/**
 * Page object for the Shift planner (/shifts) and the Shift create/edit
 * form (/shifts/new, /shifts/:id), backed by ShiftPlannerPage.tsx and
 * ShiftFormPage.tsx.
 *
 * Form inputs are matched by their wrapping <label> text — the labels in
 * ShiftFormPage wrap their controls, giving each input an accessible name.
 */
export class ShiftPage {
  readonly page: Page;

  // Planner (/shifts)
  readonly plannerHeading: Locator;
  readonly newShiftLink: Locator;
  readonly plannerBlocks: Locator;

  // Form (/shifts/new, /shifts/:id)
  readonly formHeading: Locator;
  readonly nameInput: Locator;
  readonly statusSelect: Locator;
  readonly departmentSelect: Locator;
  readonly workLocationSelect: Locator;
  readonly startInput: Locator;
  readonly endInput: Locator;
  readonly submitButton: Locator;
  readonly deleteButton: Locator;
  readonly errorAlert: Locator;

  constructor(page: Page) {
    this.page = page;

    this.plannerHeading = page.getByRole('heading', { name: 'Shifts' });
    this.newShiftLink = page.getByRole('link', { name: 'New shift' });
    this.plannerBlocks = page.locator('.planner-block');

    // ShiftFormPage wraps each control in a <label class="form-field"> whose
    // first <span> holds the field name. getByLabel is unreliable here: for a
    // <select> the label text gets polluted with the <option> texts, so an
    // exact match never lands. Resolve the control via its label's <span>.
    const field = (label: string) =>
      page
        .locator('label.form-field')
        .filter({ has: page.getByText(label, { exact: true }) });

    this.formHeading = page.locator('.page-title');
    this.nameInput = field('Shift name').locator('input');
    this.statusSelect = field('Status').locator('select');
    this.departmentSelect = field('Department').locator('select');
    this.workLocationSelect = field('Work location').locator('select');
    this.startInput = field('Start').locator('input');
    this.endInput = field('End').locator('input');
    this.submitButton = page.locator('button[type="submit"]');
    this.deleteButton = page.getByRole('button', { name: 'Delete' });
    this.errorAlert = page.locator('.alert-error');
  }

  async gotoPlanner() {
    await this.page.goto('/shifts');
  }

  async gotoNew() {
    await this.page.goto('/shifts/new');
  }

  async gotoEdit(id: number) {
    await this.page.goto(`/shifts/${id}`);
  }

  /**
   * Fill the shift form. Department / work location default to the first
   * loaded option; pass `pickReferences` to select them explicitly (needed
   * when creating a shift from scratch).
   */
  async fillForm(opts: {
    name?: string;
    status?: string;
    start?: string;
    end?: string;
    pickReferences?: boolean;
  }) {
    if (opts.pickReferences) {
      // index 0 is the disabled "Select…" placeholder
      await this.departmentSelect.selectOption({ index: 1 });
      await this.workLocationSelect.selectOption({ index: 1 });
    }
    if (opts.name !== undefined) await this.nameInput.fill(opts.name);
    if (opts.status !== undefined) await this.statusSelect.selectOption(opts.status);
    if (opts.start !== undefined) await this.startInput.fill(opts.start);
    if (opts.end !== undefined) await this.endInput.fill(opts.end);
  }

  async submit() {
    await this.submitButton.click();
  }

  async isErrorVisible(): Promise<boolean> {
    return this.errorAlert.isVisible();
  }
}

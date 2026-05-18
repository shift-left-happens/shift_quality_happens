/**
 * Shared browser authentication helpers
 *
 * Re-usable helpers for seeding auth state into the browser's localStorage
 * in Playwright E2E tests.
 */

import { expect, type Page } from '@playwright/test';
import type { LoginResponse } from './api-helpers';

/**
 * Seeds the browser's localStorage with the given session before the page loads.
 * Call this before `page.goto()` so the app picks up auth on first render.
 */
export async function seedAuthState(page: Page, session: LoginResponse): Promise<void> {
  await page.addInitScript(
    ({ token, user }) => {
      localStorage.removeItem('shift_happens_token');
      localStorage.removeItem('shift_happens_user');
      localStorage.setItem('shift_happens_token', token);
      localStorage.setItem('shift_happens_user', JSON.stringify(user));
    },
    {
      token: session.token,
      user: {
        employeeId: session.employeeId,
        employeeNumber: session.employeeNumber,
        firstName: session.firstName,
        lastName: session.lastName,
        email: session.email,
        roleId: session.roleId,
        roleName: session.roleName,
      },
    },
  );
}

/**
 * Navigates to the app root and ensures the browser is authenticated.
 * If the app redirects to /login (e.g. because early init-script storage writes
 * were missed), re-seeds localStorage on-origin and reloads.
 */
export async function ensureBrowserAuthenticated(page: Page, session: LoginResponse): Promise<void> {
  await page.goto('/');
  if (/\/login/.test(page.url())) {
    await page.evaluate(
      ({ token, user }) => {
        localStorage.setItem('shift_happens_token', token);
        localStorage.setItem('shift_happens_user', JSON.stringify(user));
      },
      {
        token: session.token,
        user: {
          employeeId: session.employeeId,
          employeeNumber: session.employeeNumber,
          firstName: session.firstName,
          lastName: session.lastName,
          email: session.email,
          roleId: session.roleId,
          roleName: session.roleName,
        },
      },
    );
    await page.goto('/');
  }
  await expect(page).not.toHaveURL(/\/login/);
}

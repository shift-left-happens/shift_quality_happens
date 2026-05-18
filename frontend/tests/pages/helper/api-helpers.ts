/**
 * Shared API test helpers
 *
 * Re-usable utilities for Playwright API integration tests.
 * Import from this module instead of duplicating these functions in each spec file.
 */

import { expect, type APIRequestContext } from '@playwright/test';

export const API_URL = process.env.API_URL || 'http://localhost:8080';
export const DEFAULT_ADMIN_EMAIL = process.env.TEST_ADMIN_EMAIL || 'admin@shift.dk';
export const DEFAULT_PASSWORD = process.env.TEST_USER_PASSWORD || 'password123';

export type LoginResponse = {
  token: string;
  employeeId: number;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  roleId: number;
  roleName: string;
};

/**
 * Login and return only the JWT token and role name.
 * Use this in API tests that only need a bearer token.
 */
export async function loginAndGetToken(
  request: APIRequestContext,
  email: string,
  password: string = DEFAULT_PASSWORD,
): Promise<{ token: string; roleName?: string }> {
  const response = await request.post(`${API_URL}/auth/login`, {
    data: { email, password },
  });
  expect(response.status(), `Expected successful login for ${email}`).toBe(200);
  const body = await response.json();
  expect(body).toHaveProperty('token');
  expect(typeof body.token).toBe('string');
  return { token: body.token as string, roleName: body.roleName as string | undefined };
}

/**
 * Login and return the full login response body.
 * Use this when you need employeeId or other fields beyond the token.
 */
export async function login(
  request: APIRequestContext,
  email: string,
  password: string = DEFAULT_PASSWORD,
): Promise<LoginResponse> {
  const res = await request.post(`${API_URL}/auth/login`, {
    data: { email, password },
  });
  expect(res.status(), `Expected successful login for ${email}`).toBe(200);
  return (await res.json()) as LoginResponse;
}

/** Returns an Authorization header object for the given bearer token. */
export function authHeaders(token: string): { Authorization: string } {
  return { Authorization: `Bearer ${token}` };
}

/** Formats a Date to an ISO-8601 datetime string without milliseconds. */
export const fmt = (d: Date): string => d.toISOString().slice(0, 19);

/**
 * Builds a future shift time window.
 * @param daysFromNow - How many days into the future the shift starts.
 * @param hours - Duration of the shift in hours (default: 8).
 */
export function futureShiftWindow(
  daysFromNow: number = 7,
  hours: number = 8,
): { startDatetime: string; endDatetime: string } {
  const start = new Date(Date.now() + daysFromNow * 24 * 60 * 60 * 1000);
  const end = new Date(start.getTime() + hours * 60 * 60 * 1000);
  return { startDatetime: fmt(start), endDatetime: fmt(end) };
}

/**
 * Builds a future shift time window with minute-level precision.
 * Seconds are zeroed for cleaner datetime values.
 * @param daysFromNow - How many days into the future the shift starts.
 * @param minutes - Duration of the shift in minutes.
 */
export function shiftWindow(
  daysFromNow: number,
  minutes: number,
): { startDatetime: string; endDatetime: string } {
  const start = new Date(Date.now() + daysFromNow * 24 * 60 * 60 * 1000);
  start.setSeconds(0, 0);
  const end = new Date(start.getTime() + minutes * 60 * 1000);
  return { startDatetime: fmt(start), endDatetime: fmt(end) };
}

/** Returns a string of random letters of the given length. */
export function randomLetters(length: number = 8): string {
  const alphabet = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz';
  return Array.from({ length }, () => alphabet[Math.floor(Math.random() * alphabet.length)]).join('');
}

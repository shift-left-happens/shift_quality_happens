/**
 * Shared employee test helpers
 *
 * Re-usable employee payload builder for Playwright tests.
 */

export type EmployeeCreatePayload = {
  firstName: string;
  lastName: string;
  employeeNumber: string;
  email: string;
  loginPassword: string;
  birthDate: string;
  hireDate: string;
  employmentStatus: string;
  userRole: string;
  primaryWorkLocationId: number;
  phoneNumber: string;
  [key: string]: unknown;
};

/**
 * Builds a valid employee creation payload with sensible defaults.
 * Pass an overrides object to change specific fields (including invalid values for
 * negative validation tests).
 */
export function buildEmployeePayload(
  overrides: Record<string, unknown> = {},
): EmployeeCreatePayload {
  const now = Date.now();
  return {
    firstName: 'API',
    lastName: 'TestUser',
    employeeNumber: `EMP-${now}`,
    email: `api.test.${now}@hospital.dk`,
    loginPassword: 'Password123',
    birthDate: '1995-01-01',
    hireDate: '2024-01-01',
    employmentStatus: 'ACTIVE',
    userRole: 'Employee',
    primaryWorkLocationId: 1,
    phoneNumber: '12345678',
    ...overrides,
  };
}

// Seed data ranges — see docker/init/02-seed-data.sql
// 100 employees, 100 shifts, 100 assignments, 100 leave requests seeded

export const SHIFT_IDS = Array.from({ length: 100 }, (_, i) => i + 1);
export const EMPLOYEE_IDS = Array.from({ length: 100 }, (_, i) => i + 1);
export const ASSIGNMENT_IDS = Array.from({ length: 100 }, (_, i) => i + 1);
export const LEAVE_REQUEST_IDS = Array.from({ length: 100 }, (_, i) => i + 1);

export function randomFrom(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

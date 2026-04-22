import type { RoleName } from '../api/types';

export function canWrite(role: RoleName | undefined): boolean {
  return role === 'Administrator' || role === 'Manager';
}

export function isAdmin(role: RoleName | undefined): boolean {
  return role === 'Administrator';
}

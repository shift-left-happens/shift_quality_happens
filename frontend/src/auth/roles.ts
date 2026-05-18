import type { RoleName } from '../api/types';

export function canWrite(role: RoleName | undefined): boolean {
  return role === 'Administrator' || role === 'Manager';
}

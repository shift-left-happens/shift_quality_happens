import { apiRequest } from './client';
import type { UserRole } from './types';

export function listUserRoles(): Promise<UserRole[]> {
  return apiRequest<UserRole[]>('/userroles');
}

import { apiRequest } from './client';
import type { LeaveType } from './types';

export function listLeaveTypes(): Promise<LeaveType[]> {
  return apiRequest<LeaveType[]>('/leavetypes');
}

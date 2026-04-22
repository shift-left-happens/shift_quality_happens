import { apiRequest } from './client';
import type { LeaveRequest, NewLeaveRequest } from './types';

export function listLeaveRequests(): Promise<LeaveRequest[]> {
  return apiRequest<LeaveRequest[]>('/leaverequests');
}

export function getLeaveRequest(id: number): Promise<LeaveRequest> {
  return apiRequest<LeaveRequest>(`/leaverequests/${id}`);
}

export function createLeaveRequest(data: NewLeaveRequest): Promise<LeaveRequest> {
  return apiRequest<LeaveRequest>('/leaverequests', {
    method: 'POST',
    body: data,
  });
}

export function updateLeaveRequest(
  id: number,
  data: Partial<NewLeaveRequest>
): Promise<LeaveRequest> {
  return apiRequest<LeaveRequest>(`/leaverequests/${id}`, {
    method: 'PATCH',
    body: data,
  });
}

export function deleteLeaveRequest(id: number): Promise<void> {
  return apiRequest<void>(`/leaverequests/${id}`, { method: 'DELETE' });
}

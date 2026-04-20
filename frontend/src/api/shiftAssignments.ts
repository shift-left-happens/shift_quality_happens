import { apiRequest } from './client';
import type { NewShiftAssignment, ShiftAssignment } from './types';

export function listShiftAssignments(): Promise<ShiftAssignment[]> {
  return apiRequest<ShiftAssignment[]>('/shiftassignments');
}

export function getShiftAssignment(id: number): Promise<ShiftAssignment> {
  return apiRequest<ShiftAssignment>(`/shiftassignments/${id}`);
}

export function createShiftAssignment(
  data: NewShiftAssignment
): Promise<ShiftAssignment> {
  return apiRequest<ShiftAssignment>('/shiftassignments', {
    method: 'POST',
    body: data,
  });
}

export function updateShiftAssignment(
  id: number,
  data: NewShiftAssignment
): Promise<ShiftAssignment> {
  return apiRequest<ShiftAssignment>(`/shiftassignments/${id}`, {
    method: 'PUT',
    body: data,
  });
}

export function deleteShiftAssignment(id: number): Promise<void> {
  return apiRequest<void>(`/shiftassignments/${id}`, { method: 'DELETE' });
}

import { apiRequest } from './client';
import type { NewShift, Shift } from './types';

export function listShifts(): Promise<Shift[]> {
  return apiRequest<Shift[]>('/shifts');
}

export function getShift(id: number): Promise<Shift> {
  return apiRequest<Shift>(`/shifts/${id}`);
}

export function createShift(data: NewShift): Promise<Shift> {
  return apiRequest<Shift>('/shifts', { method: 'POST', body: data });
}

export function updateShift(id: number, data: NewShift): Promise<Shift> {
  return apiRequest<Shift>(`/shifts/${id}`, { method: 'PUT', body: data });
}

export function deleteShift(id: number): Promise<void> {
  return apiRequest<void>(`/shifts/${id}`, { method: 'DELETE' });
}

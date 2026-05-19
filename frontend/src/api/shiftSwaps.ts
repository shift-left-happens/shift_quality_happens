import { apiRequest } from './client';
import type { NewShiftSwap, ShiftSwap } from './types';

export function listShiftSwaps(): Promise<ShiftSwap[]> {
  return apiRequest<ShiftSwap[]>('/shiftswaps');
}

export function getShiftSwap(id: number): Promise<ShiftSwap> {
  return apiRequest<ShiftSwap>(`/shiftswaps/${id}`);
}

export function createShiftSwap(data: NewShiftSwap): Promise<ShiftSwap> {
  return apiRequest<ShiftSwap>('/shiftswaps', { method: 'POST', body: data });
}

/** Cancel a swap request. Backend route: POST /shiftswaps/{id}/cancel */
export function cancelShiftSwap(id: number): Promise<ShiftSwap> {
  return apiRequest<ShiftSwap>(`/shiftswaps/${id}/cancel`, { method: 'POST' });
}

export function deleteShiftSwap(id: number): Promise<void> {
  return apiRequest<void>(`/shiftswaps/${id}`, { method: 'DELETE' });
}

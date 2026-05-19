import { apiRequest } from './client';
import type { NewShiftSwapApproval, ShiftSwapApproval } from './types';

/**
 * Record an approve/decline decision on a pending shift swap.
 * The backend mutates the swap's status (and, on approval, reassigns the
 * shift to the target employee). Admin/Manager only.
 */
export function createShiftSwapApproval(
  data: NewShiftSwapApproval
): Promise<ShiftSwapApproval> {
  return apiRequest<ShiftSwapApproval>('/shiftswapapprovals', {
    method: 'POST',
    body: data,
  });
}

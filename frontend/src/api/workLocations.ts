import { apiRequest } from './client';
import type { WorkLocation } from './types';

export function listWorkLocations(): Promise<WorkLocation[]> {
  return apiRequest<WorkLocation[]>('/worklocations');
}

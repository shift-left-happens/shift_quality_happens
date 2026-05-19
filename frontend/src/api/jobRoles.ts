import { apiRequest } from './client';
import type { JobRole, NewJobRole } from './types';

export function listJobRoles(): Promise<JobRole[]> {
  return apiRequest<JobRole[]>('/jobroles');
}

export function getJobRole(id: number): Promise<JobRole> {
  return apiRequest<JobRole>(`/jobroles/${id}`);
}

export function createJobRole(data: NewJobRole): Promise<JobRole> {
  return apiRequest<JobRole>('/jobroles', { method: 'POST', body: data });
}

export function updateJobRole(id: number, data: NewJobRole): Promise<JobRole> {
  return apiRequest<JobRole>(`/jobroles/${id}`, { method: 'PUT', body: data });
}

export function deleteJobRole(id: number): Promise<void> {
  return apiRequest<void>(`/jobroles/${id}`, { method: 'DELETE' });
}

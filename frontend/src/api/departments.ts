import { apiRequest } from './client';
import type { Department, NewDepartment } from './types';

export function listDepartments(): Promise<Department[]> {
  return apiRequest<Department[]>('/departments');
}

export function getDepartment(id: number): Promise<Department> {
  return apiRequest<Department>(`/departments/${id}`);
}

export function createDepartment(data: NewDepartment): Promise<Department> {
  return apiRequest<Department>('/departments', { method: 'POST', body: data });
}

export function updateDepartment(
  id: number,
  data: Partial<NewDepartment>
): Promise<Department> {
  return apiRequest<Department>(`/departments/${id}`, {
    method: 'PATCH',
    body: data,
  });
}

export function deleteDepartment(id: number): Promise<void> {
  return apiRequest<void>(`/departments/${id}`, { method: 'DELETE' });
}

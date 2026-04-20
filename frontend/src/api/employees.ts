import { apiRequest } from './client';
import type { Employee, NewEmployee } from './types';

export function listEmployees(): Promise<Employee[]> {
  return apiRequest<Employee[]>('/employees');
}

export function getEmployee(id: number): Promise<Employee> {
  return apiRequest<Employee>(`/employees/${id}`);
}

export function createEmployee(data: NewEmployee): Promise<Employee> {
  return apiRequest<Employee>('/employees', { method: 'POST', body: data });
}

export function updateEmployee(id: number, data: Partial<NewEmployee>): Promise<Employee> {
  return apiRequest<Employee>(`/employees/${id}`, { method: 'PATCH', body: data });
}

export function deleteEmployee(id: number): Promise<void> {
  return apiRequest<void>(`/employees/${id}`, { method: 'DELETE' });
}

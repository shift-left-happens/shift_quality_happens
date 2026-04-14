export interface User {
  employeeId: number;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  roleId: number;
  roleName: string;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export type RoleName = 'Administrator' | 'Manager' | 'Employee' | (string & {});

export interface LoginResponse {
  token: string;
  employeeId: number;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  roleId: number;
  roleName: RoleName;
}

export type AuthUser = Omit<LoginResponse, 'token'>;

export interface Employee {
  employeeId: number;
  employeeNumber: string | null;
  firstName: string | null;
  lastName: string | null;
  email: string;
  fkUserRoleId: number;
  phoneNumber: string | null;
  hireDate: string | null;
  employmentStatus: string | null;
  primaryWorkLocationId: number | null;
}

export type NewEmployee = Omit<Employee, 'employeeId'> & { loginPassword?: string };

export interface UserRole {
  userRoleId: number;
  userRoleName: string;
}

export interface Department {
  departmentId: number;
  departmentName: string | null;
  isActive: boolean | null;
}

export type NewDepartment = Omit<Department, 'departmentId'>;

export interface WorkLocation {
  workLocationId: number;
  locationName: string | null;
  addressLine1: string | null;
  addressLine2: string | null;
  city: string | null;
  country: string | null;
  timezone: string | null;
  isActive: boolean | null;
}

export interface Shift {
  shiftId: number;
  departmentId: number;
  workLocationId: number;
  shiftName: string | null;
  startDatetime: string | null;
  endDatetime: string | null;
  shiftStatus: string | null;
}

export type NewShift = Omit<Shift, 'shiftId'>;

export interface ShiftAssignment {
  shiftAssignmentId: number;
  shiftId: number;
  employeeId: number;
  assignmentStatus: string | null;
  assignedDatetime: string | null;
  checkInDatetime: string | null;
  checkOutDatetime: string | null;
}

export type NewShiftAssignment = Omit<ShiftAssignment, 'shiftAssignmentId'>;

export interface LeaveType {
  leaveTypeId: number;
  leaveTypeName: string | null;
  leaveTypeDescription: string | null;
  requiresApproval: boolean | null;
  isPaidLeave: boolean | null;
}

export interface LeaveRequest {
  leaveRequestId: number;
  employeeId: number;
  leaveTypeId: number;
  startDate: string | null;
  endDate: string | null;
  requestStatus: string | null;
  reason: string | null;
  requestedDatetime: string | null;
}

export type NewLeaveRequest = Omit<LeaveRequest, 'leaveRequestId'>;

export class ApiError extends Error {
  constructor(
    public status: number,
    public body: unknown,
    message: string
  ) {
    super(message);
  }
}

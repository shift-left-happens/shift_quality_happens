**Software Requirements Specification**

**Shift-Happens**

*A shift scheduling and swap-management system*

Software Quality Management — Exam Project

**Group G**

*Version 1.0 (initial draft, prior to formal review)*

Author: Group G

# **1\. Introduction**

## **1.1 Purpose**

This document specifies the software requirements for Shift-Happens, a web-based shift scheduling system intended for organisations that need to plan employee shifts, manage shift swaps between employees and enforce labour-related business rules. The document is the basis for the design, implementation, and acceptance testing of the application.

## **1.2 Scope**

Shift-Happens (henceforth SH) will allow managers and administrators to create shifts and assign them to employees, and will allow employees to view their schedule and request swaps with colleagues. SH will perform validation against a set of scheduling rules (such as overlap detection, rest periods and contract hours).

## **1.3 Definitions, Acronyms and Abbreviations**

| Term | Definition |
| :---- | :---- |
| SH | Shift-Happens. The application described in this document. |
| Shift | A scheduled work assignment with a date, start time, end time, work location, department and required job role. |
| Swap | A request from one employee to exchange one of their assigned shifts with a shift assigned to another employee. |
| Employee | A person registered in the system who can be assigned to shifts. |
| Manager / Admin | A user with elevated permissions who can create shifts, manage employees and approve swaps. |
| CRUD | Create, Read, Update, Delete. |

## **1.4 Technology Stack**

SH will be implemented as a web application with the following stack:

* Backend: Java 21 with Spring Boot.

* Database: MySQL.

* Frontend: React with TypeScript.

# **2\. Overall Description**

## **2.1 System Structure**

SH will consist of:

* A backend REST API (Spring Boot).

* A relational database (MySQL) for persistence.

* A web-based user interface (React \+ TypeScript).

The frontend will communicate with the backend exclusively through the REST API. The backend will be the only component that reads from or writes to the database.

## **2.2 User Roles**

The system will distinguish between the following user roles:

| Role | Description |
| :---- | :---- |
| Admin | Has full access. Manages employees, contracts, departments, work locations, job roles and shifts. |
| Manager | Creates and manages shifts, approves or declines swap requests. |
| Employee | Views own shifts, requests swaps with other employees, cancels own swap requests. |

## **2.3 Operating Environment**

The application will run on a server hosting the Spring Boot backend and the MySQL database. End users will access the application through a modern web browser.

## **2.4 Assumptions and Dependencies**

* All employees and managers have access to a modern web browser.

* The database server is available and reachable from the backend.

# **3\. Functional Requirements**

## **3.1 Shift Management**

| ID | Requirement | Priority | Description |
| :---- | :---- | :---- | :---- |
| FR-SH-01 | Create Shift | High | The system must allow managers and admins to create a shift with date, start time, end time, work location, department and required job role. |
| FR-SH-02 | Update Shift | High | The system must allow managers and admins to update shift details. |
| FR-SH-03 | Delete Shift | Medium | The system must allow managers and admins to delete a shift if it has not started and has no unresolved swap requests. |
| FR-SH-04 | View Shifts | High | The system must allow users to view shifts filtered by date, employee, department, location and status. |
| FR-SH-05 | Shift Status | High | Each shift must have a status: Open, Assigned, Pending Swap, Cancelled or Completed. |
| FR-SH-06 | Cancel Shift | High | The system must allow managers and admins to cancel a shift. Cancelled shifts must not count as active working time. |
| FR-SH-07 | Reject Invalid Shift Time | High | The system must reject shifts where the end time is before or equal to the start time. |
| FR-SH-08 | Overnight Shift Support | Medium | The system should support shifts that start on one day and end on the next day. |

## **3.2 Shift Swap Management**

| ID | Requirement | Priority | Description |
| :---- | :---- | :---- | :---- |
| FR-SW-01 | Request Shift Swap | High | An employee must be able to request a shift swap with another employee. |
| FR-SW-02 | Create Pending Swap | High | When a swap is requested, the system must create a swap request with status Pending. |
| FR-SW-03 | View Pending Swaps | High | Users must be able to view all pending swap requests. |
| FR-SW-04 | Approve Swap | High | Managers and admins must be able to approve a pending shift swap. |
| FR-SW-05 | Decline Swap | High | Managers and admins must be able to decline a pending shift swap. |
| FR-SW-06 | Cancel Swap Request | Medium | The employee who created a swap request must be able to cancel it before approval. |
| FR-SW-07 | Prevent Duplicate Pending Swaps | High | The system must prevent multiple pending swap requests for the same shift. |

## **3.3 Employee Management**

| ID | Requirement | Priority | Description |
| :---- | :---- | :---- | :---- |
| FR-EMP-01 | Create Employee | High | The system must allow admins to create employees with name, email, contract, department, job role and work location. |
| FR-EMP-02 | View Employees | High | The system must allow managers and admins to view all employees. |
| FR-EMP-03 | Update Employee | High | The system must allow admins/managers to update employee information. |
| FR-EMP-03.1 | Update own Employee | High | Update password, e-mail or phone number |
| FR-EMP-04 | Delete Employee | Medium | The system must allow admins to delete employees. |
| FR-EMP-05 | Prevent Deleting Active Employee | High | The system must prevent deletion of employees with future assigned shifts unless they are first reassigned or cancelled. |
| FR-EMP-06 | Unique Employee Email | High | Each employee must have a unique email address. |
| FR-EMP-07 | Employee Status | Low | Employees should have a status: Active or Inactive. |
| FR-EMP-08 | Employee Login | High | Employee Login |
| FR-EMP-09 | Employee Logout | High | Employee Logout |
| FR-EMP-10 | Hashed passwords | High | Passwords should be hashed prior to storing them in the DB. |

## **3.4 Department Management**

| ID | Requirement | Priority | Description |
| :---- | :---- | :---- | :---- |
| FR-DEP-01 | Create Department | High | The system must allow admins to create departments. |
| FR-DEP-02 | View Departments | High | The system must allow users to view available departments. |
| FR-DEP-03 | Update Department | Medium | The system must allow admins to update department names and descriptions. |
| FR-DEP-04 | Delete Department | Medium | The system must allow admins to delete departments if they are not linked to active employees or shifts. |
| FR-DEP-05 | Unique Department Name | Medium | Department names must be unique. |

## **3.5 Work Location Management**

| ID | Requirement | Priority | Description |
| :---- | :---- | :---- | :---- |
| FR-LOC-01 | Create Work Location | High | The system must allow admins to create work locations. |
| FR-LOC-02 | View Work Locations | High | The system must allow users to view available work locations. |
| FR-LOC-03 | Update Work Location | Medium | The system must allow admins to update work location information. |
| FR-LOC-04 | Delete Work Location | Medium | The system must prevent deleting locations linked to active shifts. |
| FR-LOC-05 | Location Address | Medium | A work location should include address, city and an optional description. |

## **3.6 Contract Management**

| ID | Requirement | Priority | Description |
| :---- | :---- | :---- | :---- |
| FR-CON-01 | Create Contract | High | The system must allow managers and admins to create employee contracts. |
| FR-CON-02 | View Contracts | High | The system must allow managers and admins to view contract details. |
| FR-CON-03 | Update Contract | High | The system must allow admins to update contract hours, type and validity period. |
| FR-CON-04 | Delete Contract | Medium | The system must prevent deleting contracts linked to active employees. |
| FR-CON-05 | Weekly Contract Hours | High | A contract must define expected weekly working hours. |
| FR-CON-06 | Contract Validity Period | Medium | A contract should have a start date and an optional end date. |

## **3.7 Job Role Management**

| ID | Requirement | Priority | Description |
| :---- | :---- | :---- | :---- |
| FR-JR-01 | Create Job Role | High | The system must allow admins to create job roles. |
| FR-JR-02 | View Job Roles | High | The system must allow users to view available job roles. |
| FR-JR-03 | Update Job Role | Medium | The system must allow admins to update job role information. |
| FR-JR-04 | Delete Job Role | Medium | The system must prevent deleting job roles linked to active employees or shifts. |
| FR-JR-05 | Role-Based Shift Matching | High | Employees should only be assigned shifts that match their job role or qualifications. |

# **4\. Business Rules**

## **4.1 Employee Rules**

| ID | Rule |
| :---- | :---- |
| BR-EM-01 | An employee must be at least 16 years old. |
| BR-EM-02 | An employee cannot be older than 100 years old. |
| BR-EM-03 | Birth dates cannot be in the future. |
| BR-EM-04 | Birth dates must follow ISO 8601 format (YYYY-MM-DD). |
| BR-EM-05 | The birth date must be a real calendar date. |

## **4.2 Shift Rules**

| ID | Rule |
| :---- | :---- |
| BR-SH-01 | A shift must have a start time before its end time. |
| BR-SH-02 | An employee cannot have two active shifts that overlap. |
| BR-SH-03 | Cancelled shifts must not count when checking overlap or working hours. |
| BR-SH-04 | A pending shift swap must not change the assigned employees until it is approved. |
| BR-SH-05 | When a swap is approved, the assigned employees must be updated. |
| BR-SH-06 | If a swap is declined, the original assignments must remain unchanged. |
| BR-SH-07 | A shift can only have one active assigned employee. |
| BR-SH-08 | A swap must not be approved if it would cause overlapping shifts. |

## **4.3 Working Time Rules**

| ID | Rule |
| :---- | :---- |
| BR-WT-01 | An employee must not work more than 37 hours per week on average. |
| BR-WT-02 | Employees must have at least 11 hours of rest between shifts. |
| BR-WT-03 | Overtime worked in one week must be compensated in later scheduling periods. |
| BR-WT-04 | Employees must not work more than 6 consecutive days. |
| BR-WT-05 | Employees must not work more than 3 consecutive night shifts. |
| BR-WT-06 | Shift assignments must respect the employee's contract hours. |
| BR-WT-07 | An employee cannot be assigned to overlapping shifts. |

## **4.4 Swap Approval Rules**

| ID | Rule |
| :---- | :---- |
| BR-AP-01 | Shift swaps must require manager or admin approval before they become active. |
| BR-AP-02 | An employee should not be able to approve their own swap request. |
| BR-AP-03 | A cancelled swap request cannot be approved. |
| BR-AP-04 | A declined swap request cannot be approved later unless a new request is created. |
| BR-AP-05 | A swap involving a cancelled shift cannot be approved. |
| BR-AP-06 | When approving a swap, all related updates must succeed together, or no changes should be saved. |

# **5\. User Interface**

The web interface will offer the following main views:

* Login page.

* Schedule view (shifts displayed by week).

* Shift creation and edit forms (managers and admins).

* Swap request page (employees).

* Swap approval page (managers and admins).

* Employee, department, work location, contract and job role administration pages (admins).

## **5.1 Input Validation**

The system will perform input validation on key fields:

| Field | Format / Constraint |
| :---- | :---- |
| First name | Letters only. An initial may follow, formatted as a single uppercase letter followed by a dot (e.g. "Peter A."). |
| Last name | Letters only. No numbers, symbols or spaces. |
| Email | Standard email format. Must be unique across employees. |
| Birth date | ISO 8601 (YYYY-MM-DD). Year between 1926 and 2010\. |
| Shift start / end time | Start time must be before end time. |

# **6\. Non-Functional Requirements**

| ID | Requirement | Description |
| :---- | :---- | :---- |
| NFR-01 | Performance | The system must respond within 2 seconds under normal load. |
| NFR-02 | Scalability | The system must support at least 100 employees. |
| NFR-03 | Availability | The system must be available 99.999% of the time. |
| NFR-04 | Security | User passwords must be encrypted before storage. |
| NFR-05 | Usability | The user interface should be usable on a desktop browser. |

# **8\. Out of Scope**

The following items will not be addressed by SH version 1.0:

* Payroll calculations.

* Native mobile applications (the web interface may be used on mobile browsers but is not optimised for them).

* Integration with external HR systems.

* Time-tracking (clock-in / clock-out).
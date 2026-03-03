-- Create all tables for shift-happens
CREATE DATABASE IF NOT EXISTS shift_happens;
USE shift_happens;
CREATE TABLE department (
                            department_id INT AUTO_INCREMENT PRIMARY KEY,
                            department_name VARCHAR(255) NOT NULL,
                            is_active BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE work_location (
                               work_location_id INT AUTO_INCREMENT PRIMARY KEY,
                               location_name VARCHAR(255) NOT NULL,
                               address_line_1 VARCHAR(255) NOT NULL,
                               address_line_2 VARCHAR(255),
                               city VARCHAR(100) NOT NULL,
                               country VARCHAR(100) NOT NULL,
                               timezone VARCHAR(50),
                               is_active BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB;
CREATE TABLE employee (
                          employee_id INT AUTO_INCREMENT PRIMARY KEY,
                          employee_number VARCHAR(50) UNIQUE,
                          first_name VARCHAR(100) NOT NULL,
                          last_name VARCHAR(100) NOT NULL,
                          email VARCHAR(255) NOT NULL,
                          phone_number VARCHAR(50) NOT NULL,
                          hire_date DATE NOT NULL,
                          employment_status VARCHAR(50) NOT NULL,
                          primary_work_location_id INT NOT NULL,
                          CONSTRAINT fk_employee_location
                              FOREIGN KEY (primary_work_location_id)
                                  REFERENCES work_location(work_location_id)
) ENGINE=InnoDB;

CREATE TABLE employee_contract (
                                   contract_id INT AUTO_INCREMENT PRIMARY KEY,
                                   employee_id INT NOT NULL,
                                   department_id INT NOT NULL,
                                   contract_type VARCHAR(50) NOT NULL,
                                   start_date DATE NOT NULL,
                                   end_date DATE,
                                   weekly_hours INT NOT NULL,
                                   salary_amount DECIMAL(12,2) NOT NULL,
                                   is_active BOOLEAN DEFAULT TRUE NOT NULL,
                                   INDEX idx_dep_type_ac (department_id, contract_type, is_active),
                                   CONSTRAINT fk_contract_employee
                                       FOREIGN KEY (employee_id) REFERENCES employee(employee_id),
                                   CONSTRAINT fk_contract_department
                                       FOREIGN KEY (department_id) REFERENCES department(department_id)
) ENGINE=InnoDB;
CREATE TABLE job_role (
                          job_role_id INT AUTO_INCREMENT PRIMARY KEY,
                          role_name VARCHAR(255) NOT NULL,
                          job_role_description TEXT,
                          is_certification_required BOOLEAN DEFAULT FALSE
) ENGINE=InnoDB;

CREATE TABLE employee_job_role (
                                   employee_job_role_id INT AUTO_INCREMENT PRIMARY KEY,
                                   employee_id INT NOT NULL,
                                   job_role_id INT NOT NULL,
                                   assigned_date DATE NOT NULL,
                                   expiry_date DATE,
                                   proficiency_level VARCHAR(50),
                                   UNIQUE KEY unq_emp_job (employee_id, job_role_id),
                                   CONSTRAINT fk_ejr_employee
                                       FOREIGN KEY (employee_id) REFERENCES employee(employee_id),
                                   CONSTRAINT fk_ejr_role
                                       FOREIGN KEY (job_role_id) REFERENCES job_role(job_role_id)
) ENGINE=InnoDB;
CREATE TABLE shift (
                       shift_id INT AUTO_INCREMENT PRIMARY KEY,
                       department_id INT NOT NULL,
                       work_location_id INT NOT NULL,
                       shift_name VARCHAR(255),
                       start_datetime DATETIME NOT NULL,
                       end_datetime DATETIME NOT NULL,
                       shift_status VARCHAR(50) NOT NULL,
                       INDEX idx_dep_loc(department_id, work_location_id),
                       CONSTRAINT fk_shift_department
                           FOREIGN KEY (department_id) REFERENCES department(department_id),
                       CONSTRAINT fk_shift_location
                           FOREIGN KEY (work_location_id) REFERENCES work_location(work_location_id)
) ENGINE=InnoDB;

CREATE TABLE shift_required_job_role (
                                         shift_required_job_role_id INT AUTO_INCREMENT PRIMARY KEY,
                                         shift_id INT NOT NULL,
                                         job_role_id INT NOT NULL,
                                         required_employee_count INT DEFAULT 1,
                                         UNIQUE KEY unq_shift_jr (shift_id, job_role_id),
                                         CONSTRAINT fk_srjr_shift
                                             FOREIGN KEY (shift_id) REFERENCES shift(shift_id),
                                         CONSTRAINT fk_srjr_role
                                             FOREIGN KEY (job_role_id) REFERENCES job_role(job_role_id)
) ENGINE=InnoDB;
CREATE TABLE shift_assignment (
                                  shift_assignment_id INT AUTO_INCREMENT PRIMARY KEY,
                                  shift_id INT NOT NULL,
                                  employee_id INT NOT NULL,
                                  assignment_status VARCHAR(50),
                                  assigned_datetime DATETIME,
                                  check_in_datetime DATETIME,
                                  check_out_datetime DATETIME,
                                  CONSTRAINT fk_sa_shift
                                      FOREIGN KEY (shift_id) REFERENCES shift(shift_id),
                                  CONSTRAINT fk_sa_employee
                                      FOREIGN KEY (employee_id) REFERENCES employee(employee_id)
) ENGINE=InnoDB;

CREATE TABLE shift_approval (
                                shift_approval_id INT AUTO_INCREMENT PRIMARY KEY,
                                shift_assignment_id INT NOT NULL,
                                approver_employee_id INT NOT NULL,
                                decision VARCHAR(50),
                                approval_comment TEXT,
                                decision_datetime DATETIME,
                                CONSTRAINT fk_shift_approval_assignment
                                    FOREIGN KEY (shift_assignment_id)
                                        REFERENCES shift_assignment(shift_assignment_id),
                                CONSTRAINT fk_shift_approval_employee
                                    FOREIGN KEY (approver_employee_id)
                                        REFERENCES employee(employee_id)
) ENGINE=InnoDB;
CREATE TABLE shift_swap (
                            shift_swap_id INT AUTO_INCREMENT PRIMARY KEY,
                            original_shift_assignment_id INT NOT NULL,
                            employee_from_id INT NOT NULL,
                            employee_to_id INT NOT NULL,
                            swap_status VARCHAR(50),
                            request_datetime DATETIME,
                            reason TEXT,
                            CONSTRAINT fk_shift_swap_assignment
                                FOREIGN KEY (original_shift_assignment_id)
                                    REFERENCES shift_assignment(shift_assignment_id),
                            CONSTRAINT fk_swap_employee_from
                                FOREIGN KEY (employee_from_id) REFERENCES employee(employee_id),

                            CONSTRAINT fk_swap_employee_to
                                FOREIGN KEY (employee_to_id) REFERENCES employee(employee_id)
) ENGINE=InnoDB;

CREATE TABLE shift_swap_approval (
                                     shift_swap_approval_id INT AUTO_INCREMENT PRIMARY KEY,
                                     shift_swap_id INT NOT NULL,
                                     approver_employee_id INT NOT NULL,
                                     decision VARCHAR(50),
                                     shift_swap_comment TEXT,
                                     decision_datetime DATETIME,
                                     CONSTRAINT fk_shift_swap_approval_swap
                                         FOREIGN KEY (shift_swap_id) REFERENCES shift_swap(shift_swap_id),
                                     CONSTRAINT fk_shift_swap_approval_employee
                                         FOREIGN KEY (approver_employee_id)
                                             REFERENCES employee(employee_id)
) ENGINE=InnoDB;

CREATE TABLE leave_type (
                            leave_type_id INT AUTO_INCREMENT PRIMARY KEY,
                            leave_type_name VARCHAR(255),
                            leave_type_description TEXT,
                            requires_approval BOOLEAN DEFAULT TRUE,
                            is_paid_leave BOOLEAN DEFAULT TRUE,
                            INDEX idx_paid_approval (leave_type_id, requires_approval, is_paid_leave)
) ENGINE=InnoDB;

CREATE TABLE leave_request (
                               leave_request_id INT AUTO_INCREMENT PRIMARY KEY,
                               employee_id INT NOT NULL,
                               leave_type_id INT NOT NULL,
                               start_date DATE,
                               end_date DATE,
                               request_status VARCHAR(50),
                               reason TEXT,
                               requested_datetime DATETIME,
                               INDEX idx_leave_ledger_emp_type (employee_id, leave_type_id),
                               CONSTRAINT fk_leave_request_employee
                                   FOREIGN KEY (employee_id) REFERENCES employee(employee_id),
                               CONSTRAINT fk_leave_request_type
                                   FOREIGN KEY (leave_type_id) REFERENCES leave_type(leave_type_id)
) ENGINE=InnoDB;

CREATE TABLE leave_approval (
                                leave_approval_id INT AUTO_INCREMENT PRIMARY KEY,
                                leave_request_id INT NOT NULL,
                                approver_employee_id INT NOT NULL,
                                decision VARCHAR(50),
                                leave_comment TEXT,
                                decision_datetime DATETIME,
                                CONSTRAINT fk_leave_approval_request
                                    FOREIGN KEY (leave_request_id)
                                        REFERENCES leave_request(leave_request_id),
                                CONSTRAINT fk_leave_approval_employee
                                    FOREIGN KEY (approver_employee_id)
                                        REFERENCES employee(employee_id)
) ENGINE=InnoDB;

CREATE TABLE leave_ledger (
                              leave_ledger_id INT AUTO_INCREMENT PRIMARY KEY,
                              employee_id INT NOT NULL,
                              leave_type_id INT NOT NULL,
                              change_amount_days DECIMAL(6,2),
                              transaction_type VARCHAR(50),
                              reference_entity_type VARCHAR(100),
                              reference_entity_id INT,
                              transaction_datetime DATETIME,
                              INDEX idx_leave_ledger_emp_type (employee_id, leave_type_id),
                              CONSTRAINT fk_leave_ledger_employee
                                  FOREIGN KEY (employee_id) REFERENCES employee(employee_id),
                              CONSTRAINT fk_leave_ledger_type
                                  FOREIGN KEY (leave_type_id) REFERENCES leave_type(leave_type_id)
) ENGINE=InnoDB;
CREATE TABLE audit_log (
                           audit_log_id INT AUTO_INCREMENT PRIMARY KEY,
                           entity_type VARCHAR(100),
                           entity_id INT,
                           action_type VARCHAR(100),
                           performed_by_employee_id INT,
                           action_datetime DATETIME,
                           old_value_snapshot TEXT,
                           new_value_snapshot TEXT,
                           CONSTRAINT fk_audit_employee
                               FOREIGN KEY (performed_by_employee_id)
                                   REFERENCES employee(employee_id)
) ENGINE=InnoDB;

CREATE INDEX idx_employee_email ON employee(email);
CREATE INDEX idx_shift_datetime ON shift(start_datetime, end_datetime);
CREATE INDEX idx_leave_employee ON leave_request(employee_id);

ALTER TABLE shift_assignment
    ADD CONSTRAINT uq_shift_employee UNIQUE (shift_id, employee_id);
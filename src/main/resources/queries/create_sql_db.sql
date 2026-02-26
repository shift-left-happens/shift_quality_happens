USE railway;
CREATE TABLE department (
    department_id INT AUTO_INCREMENT PRIMARY KEY,
    department_name VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE worklocation (
    work_location_id INT AUTO_INCREMENT PRIMARY KEY,
    location_name VARCHAR(255) NOT NULL,
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    city VARCHAR(100),
    country VARCHAR(100),
    timezone VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB;
CREATE TABLE employee (
    employee_id INT AUTO_INCREMENT PRIMARY KEY,
    employee_number VARCHAR(50) UNIQUE,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255),
    phone_number VARCHAR(50),
    hire_date DATE,
    employment_status VARCHAR(50),
    primary_work_location_id INT,
    CONSTRAINT fk_employee_location
        FOREIGN KEY (primary_work_location_id)
        REFERENCES worklocation(work_location_id)
) ENGINE=InnoDB;

CREATE TABLE employee_contract (
    contract_id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    department_id INT NOT NULL,
    contract_type VARCHAR(50),
    start_date DATE,
    end_date DATE,
    weekly_hours INT,
    salary_amount DECIMAL(12,2),
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_contract_employee
        FOREIGN KEY (employee_id) REFERENCES employee(employee_id),
    CONSTRAINT fk_contract_department
        FOREIGN KEY (department_id) REFERENCES department(department_id)
) ENGINE=InnoDB;
CREATE TABLE jobrole (
    job_role_id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(255) NOT NULL,
    job_role_description TEXT,
    is_certification_required BOOLEAN DEFAULT FALSE
) ENGINE=InnoDB;

CREATE TABLE employeejobrole (
    employee_id INT NOT NULL,
    job_role_id INT NOT NULL,
    assigned_date DATE,
    expiry_date DATE,
    proficiency_level VARCHAR(50),
    PRIMARY KEY (employee_id, job_role_id),
    CONSTRAINT fk_ejr_employee
        FOREIGN KEY (employee_id) REFERENCES employee(employee_id),
    CONSTRAINT fk_ejr_role
        FOREIGN KEY (job_role_id) REFERENCES jobrole(job_role_id)
) ENGINE=InnoDB;
CREATE TABLE shift (
    shift_id INT AUTO_INCREMENT PRIMARY KEY,
    department_id INT NOT NULL,
    work_location_id INT NOT NULL,
    shift_name VARCHAR(255),
    start_datetime DATETIME,
    end_datetime DATETIME,
    shift_status VARCHAR(50),
    CONSTRAINT fk_shift_department
        FOREIGN KEY (department_id) REFERENCES department(department_id),
    CONSTRAINT fk_shift_location
        FOREIGN KEY (work_location_id) REFERENCES worklocation(work_location_id)
) ENGINE=InnoDB;

CREATE TABLE shiftrequiredjobrole (
    shift_id INT NOT NULL,
    job_role_id INT NOT NULL,
    required_employee_count INT DEFAULT 1,
    PRIMARY KEY (shift_id, job_role_id),
    CONSTRAINT fk_srjr_shift
        FOREIGN KEY (shift_id) REFERENCES shift(shift_id),
    CONSTRAINT fk_srjr_role
        FOREIGN KEY (job_role_id) REFERENCES jobrole(job_role_id)
) ENGINE=InnoDB;
CREATE TABLE shiftassignment (
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

CREATE TABLE shiftapproval (
    shift_approval_id INT AUTO_INCREMENT PRIMARY KEY,
    shift_assignment_id INT NOT NULL,
    approver_employee_id INT NOT NULL,
    decision VARCHAR(50),
    approval_comment TEXT,
    decision_datetime DATETIME,
    CONSTRAINT fk_shiftapproval_assignment
        FOREIGN KEY (shift_assignment_id)
        REFERENCES shiftassignment(shift_assignment_id),
    CONSTRAINT fk_shiftapproval_employee
        FOREIGN KEY (approver_employee_id)
        REFERENCES employee(employee_id)
) ENGINE=InnoDB;
CREATE TABLE shiftswap (
    shift_swap_id INT AUTO_INCREMENT PRIMARY KEY,
    original_shift_assignment_id INT NOT NULL,
    employee_from_id INT NOT NULL,
    employee_to_id INT NOT NULL,
    swap_status VARCHAR(50),
    request_datetime DATETIME,
    reason TEXT,
    CONSTRAINT fk_shiftswap_assignment
        FOREIGN KEY (original_shift_assignment_id)
        REFERENCES shiftassignment(shift_assignment_id),
	CONSTRAINT fk_swap_employee_from
        FOREIGN KEY (employee_from_id) REFERENCES employee(employee_id),

    CONSTRAINT fk_swap_employee_to
        FOREIGN KEY (employee_to_id) REFERENCES employee(employee_id)
) ENGINE=InnoDB;

CREATE TABLE shiftswapapproval (
    shift_swap_approval_id INT AUTO_INCREMENT PRIMARY KEY,
    shift_swap_id INT NOT NULL,
    approver_employee_id INT NOT NULL,
    decision VARCHAR(50),
    shift_swap_comment TEXT,
    decision_datetime DATETIME,
    CONSTRAINT fk_shiftswapapproval_swap
        FOREIGN KEY (shift_swap_id) REFERENCES shiftswap(shift_swap_id),
    CONSTRAINT fk_shiftswapapproval_employee
        FOREIGN KEY (approver_employee_id)
        REFERENCES employee(employee_id)
) ENGINE=InnoDB;

CREATE TABLE leavetype (
    leave_type_id INT AUTO_INCREMENT PRIMARY KEY,
    leave_type_name VARCHAR(255),
    leave_type_description TEXT,
    requires_approval BOOLEAN DEFAULT TRUE,
    is_paid_leave BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE leaverequest (
    leave_request_id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    leave_type_id INT NOT NULL,
    start_date DATE,
    end_date DATE,
    request_status VARCHAR(50),
    reason TEXT,
    requested_datetime DATETIME,
    CONSTRAINT fk_leaverequest_employee
        FOREIGN KEY (employee_id) REFERENCES employee(employee_id),
    CONSTRAINT fk_leaverequest_type
        FOREIGN KEY (leave_type_id) REFERENCES leavetype(leave_type_id)
) ENGINE=InnoDB;

CREATE TABLE leaveapproval (
    leave_approval_id INT AUTO_INCREMENT PRIMARY KEY,
    leave_request_id INT NOT NULL,
    approver_employee_id INT NOT NULL,
    decision VARCHAR(50),
    leave_comment TEXT,
    decision_datetime DATETIME,
    CONSTRAINT fk_leaveapproval_request
        FOREIGN KEY (leave_request_id)
        REFERENCES leaverequest(leave_request_id),
    CONSTRAINT fk_leaveapproval_employee
        FOREIGN KEY (approver_employee_id)
        REFERENCES employee(employee_id)
) ENGINE=InnoDB;

CREATE TABLE leaveledger (
    leave_ledger_id INT AUTO_INCREMENT PRIMARY KEY,
    employee_id INT NOT NULL,
    leave_type_id INT NOT NULL,
    change_amount_days DECIMAL(6,2),
    transaction_type VARCHAR(50),
    reference_entity_type VARCHAR(100),
    reference_entity_id INT,
    transaction_datetime DATETIME,
    CONSTRAINT fk_leaveledger_employee
        FOREIGN KEY (employee_id) REFERENCES employee(employee_id),
    CONSTRAINT fk_leaveledger_type
        FOREIGN KEY (leave_type_id) REFERENCES leavetype(leave_type_id)
) ENGINE=InnoDB;
CREATE TABLE auditlog (
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
CREATE INDEX idx_leave_employee ON leaverequest(employee_id);

ALTER TABLE shiftassignment
ADD CONSTRAINT uq_shift_employee UNIQUE (shift_id, employee_id);

--Role based access, 1 role pr user currently

CREATE TABLE user_role (
       `user_role_id` INT NOT NULL,
       `user_role_name` VARCHAR(45) NOT NULL,
       PRIMARY KEY (`user_role_id`),
       UNIQUE INDEX `user_role_name_UNIQUE` (`user_role_name` ASC) VISIBLE);

--Add 3 roles

INSERT INTO user_role (`user_role_id`,`user_role_name`)
VALUES
    (1, 'Administrator'), (2, 'Employee'), (3, 'Manager')

--Add password column for employee to log in(hashed password stored)
--Unique constraint on email so users can't have same email to login with
ALTER TABLE employee
    ADD COLUMN `password` VARCHAR(255) NOT NULL AFTER `email`,
CHANGE COLUMN `email` `email` VARCHAR(255) NOT NULL ,
ADD UNIQUE INDEX `email_UNIQUE` (`email` ASC) VISIBLE;
--Add user_role_id to employee table
ALTER TABLE employee
    ADD COLUMN fk_user_role_id INT NULL AFTER `password`;
--Update name as password is a reserved keyword
ALTER TABLE employee
    CHANGE COLUMN `password` `login_password` VARCHAR(255) NOT NULL ;
--Add some random passwords
UPDATE employee
SET login_password = SHA2(CONCAT(UUID(), RAND()), 256) WHERE employee_id < 101;
--Set roles, every 30th is a manager
UPDATE employee
SET fk_user_role_id =
        CASE
            WHEN MOD(employee_id, 30) = 0 THEN 3   -- Manager
            ELSE 2                                  -- Employee
            END
WHERE employee_id < 101
--add not null constraint
ALTER TABLE employee
    MODIFY fk_user_role_id INT NOT NULL;
-- add fk constraint, don't delete a role with active users, if role_id gets updated, users cascade to keep roles.
ALTER TABLE employee
    ADD CONSTRAINT fk_employee_user_role
        FOREIGN KEY (fk_user_role_id)
            REFERENCES user_role(user_role_id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT;
--Add additional vacation time for a test employee
INSERT INTO `leaveledger` (`employee_id`, `leave_type_id`, `change_amount_days`, `transaction_type`, `reference_entity_type`, `transaction_datetime`) VALUES ('96', '1', '2', 'ACCRUAL', '', '2026-02-24 11:00:00');
--set default datetime for new entries to simplify INSERTs
ALTER TABLE `leaverequest`
    CHANGE COLUMN `requested_datetime` `requested_datetime` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ;
--Add leave request
INSERT INTO `leaverequest` (leave_request_id, `employee_id`, `leave_type_id`, `start_date`, `end_date`, `request_status`, `reason`) VALUES (150,'96', '1', '2026-05-05', '2026-05-08', 'PENDING', 'Vacation');


--
-- Trigger for leave approvals! We will use this to approve the above request
--

DELIMITER $$

CREATE TRIGGER trg_leaveapproval_before_insert
    BEFORE INSERT ON leaveapproval
    FOR EACH ROW
BEGIN
    DECLARE v_employee_id INT;
    DECLARE v_leave_type_id INT;
    DECLARE v_days_requested DECIMAL(6,2);
    DECLARE v_leave_balance DECIMAL(6,2);
    DECLARE v_is_manager INT;

    -- Check if approver is manager
    SELECT fk_user_role_id
    INTO v_is_manager
    FROM employee
    WHERE employee_id = NEW.approver_employee_id;

    IF v_is_manager <> 3 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Only managers can approve leave requests';
END IF;

-- Only continue logic if decision is APPROVED
IF NEW.decision = 'APPROVED' THEN

        -- Get leave request info
SELECT employee_id, leave_type_id,
       DATEDIFF(end_date, start_date) + 1
INTO v_employee_id, v_leave_type_id, v_days_requested
FROM leaverequest
WHERE leave_request_id = NEW.leave_request_id;

-- Calculate current leave balance for the chosen type
SELECT COALESCE(SUM(change_amount_days),0)
INTO v_leave_balance
FROM leaveledger
WHERE employee_id = v_employee_id
  AND leave_type_id = v_leave_type_id;

-- Check if enough balance
IF v_leave_balance < v_days_requested THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Insufficient leave balance';
END IF;

        -- Update leave request
UPDATE leaverequest
SET request_status = 'APPROVED'
WHERE leave_request_id = NEW.leave_request_id;

-- Insert negative ledger entry
INSERT INTO leaveledger (
    employee_id,
    leave_type_id,
    change_amount_days,
    transaction_type,
    reference_entity_type,
    reference_entity_id,
    transaction_datetime
)
VALUES (
           v_employee_id,
           v_leave_type_id,
           -v_days_requested,
           'USAGE',
           'LeaveRequest',
           NEW.leave_request_id,
           NOW()
       );

END IF;

END$$

DELIMITER ;

--Test the leave approval trigger works

INSERT INTO leaveapproval (
    leave_request_id,
    approver_employee_id,
    decision,
    decision_datetime
)
VALUES (
           150,              -- the leave_request_id of employee 96
           30,              -- NOT a manager
           'APPROVED',
           NOW()
       );

-- Check that leaveledger has an entry for the leave request.
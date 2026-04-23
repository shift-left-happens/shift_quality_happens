USE shift_happens;
# DELIMITER $$
#
# CREATE TRIGGER trg_leaveapproval_before_insert
#     BEFORE INSERT
#     ON leave_approval
#     FOR EACH ROW
# BEGIN
#     DECLARE v_employee_id INT;
#     DECLARE v_leave_type_id INT;
#     DECLARE v_days_requested DECIMAL(6, 2);
#     DECLARE v_leave_balance DECIMAL(6, 2);
#     DECLARE v_is_manager INT;
#     DECLARE v_request_status VARCHAR(20);
#
#     /* Fetch and lock leave request */
#     SELECT employee_id,
#            leave_type_id,
#            DATEDIFF(end_date, start_date) + 1,
#            request_status
#     INTO v_employee_id,
#         v_leave_type_id,
#         v_days_requested,
#         v_request_status
#     FROM leave_request
#     WHERE leave_request_id = NEW.leave_request_id
#     LIMIT 1
#     FOR
#     UPDATE;
#
#     IF v_employee_id IS NULL THEN
#         SIGNAL SQLSTATE '45000'
#             SET MESSAGE_TEXT = 'Leave request does not exist';
#     END IF;
#
#     IF v_request_status <> 'PENDING' THEN
#         SIGNAL SQLSTATE '45000'
#             SET MESSAGE_TEXT = 'Leave request is not pending';
#     END IF;
#
#     /* Validate approver */
#     SELECT fk_user_role_id
#     INTO v_is_manager
#     FROM employee
#     WHERE employee_id = NEW.approver_employee_id
#     LIMIT 1;
#
#     IF v_is_manager IS NULL THEN
#         SIGNAL SQLSTATE '45000'
#             SET MESSAGE_TEXT = 'Approver does not exist';
#     END IF;
#
#     IF v_is_manager <> 3 THEN
#         SIGNAL SQLSTATE '45000'
#             SET MESSAGE_TEXT = 'Only managers can approve leave requests';
#     END IF;
#
#     /* Only process if APPROVED */
#     IF NEW.decision = 'APPROVED' THEN
#
#         SELECT COALESCE(SUM(change_amount_days), 0)
#         INTO v_leave_balance
#         FROM leave_ledger
#         WHERE employee_id = v_employee_id
#           AND leave_type_id = v_leave_type_id;
#
#         IF v_leave_balance < v_days_requested THEN
#             SIGNAL SQLSTATE '45000'
#                 SET MESSAGE_TEXT = 'Insufficient leave balance';
#         END IF;
#
#         UPDATE leave_request
#         SET request_status = 'APPROVED'
#         WHERE leave_request_id = NEW.leave_request_id
#         LIMIT 1;
#
#         INSERT INTO leave_ledger (employee_id,
#                                   leave_type_id,
#                                   change_amount_days,
#                                   transaction_type,
#                                   reference_entity_type,
#                                   reference_entity_id,
#                                   transaction_datetime)
#         VALUES (v_employee_id,
#                 v_leave_type_id,
#                 -v_days_requested,
#                 'USAGE',
#                 'LeaveRequest',
#                 NEW.leave_request_id,
#                 NOW());
#
#     END IF;
#
# END$$
#
# DELIMITER ;
#
# /*Test the leave approval trigger works*/
#
# /*Find random employee with role 2 (regular employee)*/
# SET @random_employee = (SELECT employee.employee_id
#                         FROM employee
#                         WHERE fk_user_role_id = 2
#                         ORDER BY RAND()
#                         LIMIT 1);
# /*Find random employee with role 3 (Manager, the required role for approvals)*/
# SET @random_manager = (SELECT employee.employee_id
#                        FROM employee
#                        WHERE fk_user_role_id = 3
#                        ORDER BY RAND()
#                        LIMIT 1);
# /*Add additional vacation time for a test employee from above manager*/
# INSERT INTO `leave_ledger` (`employee_id`, `leave_type_id`, `change_amount_days`, `transaction_type`,
#                             `reference_entity_type`, reference_entity_id, `transaction_datetime`)
# VALUES (@random_employee, 1, 10, 'ACCRUAL', 'Employee', @random_manager, '2026-02-24 11:00:00');
# /*set default datetime for new entries to simplify INSERTs*/
# ALTER TABLE `leave_request`
#     MODIFY requested_datetime DATETIME DEFAULT CURRENT_TIMESTAMP;
# /*Add leave request*/
# INSERT INTO `leave_request` (`employee_id`, `leave_type_id`, `start_date`, `end_date`, `request_status`, `reason`)
# VALUES (@random_employee, 1, '2026-05-05', '2026-05-08', 'PENDING', 'Vacation');
# SET @new_leave_request_id = LAST_INSERT_ID();
#
# INSERT INTO leave_approval (leave_request_id,
#                             approver_employee_id,
#                             decision,
#                             decision_datetime)
# VALUES (@new_leave_request_id, /* the leave_request_id of employee 96*/
#         @random_manager, /* NOT a manager*/
#         'APPROVED',
#         NOW());


DELIMITER $$

/*Prevent deletion of leave ledger entries*/
CREATE TRIGGER trg_no_delete_leave_ledger
    BEFORE DELETE
    ON leave_ledger
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Ledger entries cannot be deleted';
END$$

/* Test DELETE
START TRANSACTION
DELETE FROM leave_ledger ORDER BY leave_ledger_id DESC LIMIT 1;
ROLLBACK;
 */

/*Days cannot be 0*/
CREATE TRIGGER trg_validate_ledger_amount
    BEFORE INSERT
    ON leave_ledger
    FOR EACH ROW
BEGIN
    IF NEW.change_amount_days = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Ledger change cannot be zero';
    END IF;
END$$

/* Test INSERT
START TRANSACTION;
INSERT INTO leave_ledger (
    employee_id,
    leave_type_id,
    change_amount_days,
    transaction_type,
    transaction_datetime
)
VALUES (1, 1, 0, 'USAGE', NOW());
ROLLBACK;
*/

CREATE TRIGGER trg_prevent_employee_change
    BEFORE UPDATE
    ON leave_request
    FOR EACH ROW
BEGIN
    IF OLD.employee_id <> NEW.employee_id THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Employee cannot be changed';
    END IF;
END$$

DELIMITER ;

/*
-- Test the trigger
-- Start safe test transaction
START TRANSACTION;

-- Step 1: Get one leave request
SELECT leave_request_id, employee_id
INTO @v_leave_request_id, @v_old_employee_id
FROM leave_request
LIMIT 1;

-- Step 2: Find a different employee
SELECT employee_id
INTO @v_new_employee_id
FROM employee
WHERE employee_id <> @v_old_employee_id
LIMIT 1;

-- Step 3: Attempt to update (THIS SHOULD FAIL)
UPDATE leave_request
SET employee_id = @v_new_employee_id
WHERE leave_request_id = @v_leave_request_id;

-- Rollback so nothing changes even if trigger is disabled
ROLLBACK;
*/

SELECT 1;

DELIMITER $$
DROP TRIGGER IF EXISTS trg_validate_employee_ins$$
CREATE TRIGGER trg_validate_employee_ins
    BEFORE INSERT
    ON employee
    FOR EACH ROW
BEGIN
    IF LENGTH(NEW.login_password) < 8 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Password needs to be longer than 8 chars.';
    END IF;
end $$
DROP TRIGGER IF EXISTS trg_validate_employee_update$$
CREATE TRIGGER trg_validate_employee_update
    BEFORE UPDATE
    ON employee
    FOR EACH ROW
BEGIN
    IF LENGTH(NEW.login_password) < 8 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Password needs to be longer than 8 chars.';
    END IF;
end $$

DROP TRIGGER IF EXISTS trg_validate_contract_ins$$
CREATE TRIGGER trg_validate_contract_ins
    BEFORE INSERT
    ON employee_contract
    FOR EACH ROW
BEGIN
    IF NEW.salary_amount <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'salary_amount needs to be positive';
    END IF;
    IF NEW.weekly_hours <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'weekly_hours need to be positive';
    END IF;
    IF NEW.start_date > NEW.end_date THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'start_date cannot be after end_date';
    END IF;
end $$

DROP TRIGGER IF EXISTS trg_validate_contract_update$$
CREATE TRIGGER trg_validate_contract_update
    BEFORE UPDATE
    ON employee_contract
    FOR EACH ROW
BEGIN
    IF NEW.salary_amount <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'salary_amount needs to be positive';
    END IF;
    IF NEW.weekly_hours <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'weekly_hours need to be positive';
    END IF;
    IF NEW.start_date > NEW.end_date THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'start_date cannot be after end_date';
    END IF;
end $$

DROP TRIGGER IF EXISTS trg_no_contract_overlap_ins$$

CREATE TRIGGER trg_no_contract_overlap_ins
    BEFORE INSERT
    ON employee_contract
    FOR EACH ROW
BEGIN
    IF NEW.is_active = 1 AND EXISTS (SELECT 1
                                     FROM employee_contract ec
                                     WHERE ec.employee_id = NEW.employee_id
                                       AND ec.is_active = 1
                                       AND NEW.start_date <= IFNULL(ec.end_date, '9999-12-31') -- New contract starts before an existing active contract ended
                                       AND ec.start_date <= IFNULL(NEW.end_date, '9999-12-31') -- Existing contract starts before new contract ends
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Contract period overlaps with an existing active contract.';
    END IF;
END $$

DROP TRIGGER IF EXISTS trg_no_contract_overlap_upd$$

CREATE TRIGGER trg_no_contract_overlap_upd
    BEFORE UPDATE
    ON employee_contract
    FOR EACH ROW
BEGIN
    IF NEW.is_active = 1 AND EXISTS (SELECT 1
                                     FROM employee_contract ec
                                     WHERE ec.employee_id = NEW.employee_id
                                       AND ec.contract_id != OLD.contract_id
                                       AND ec.is_active = 1
                                       AND NEW.start_date <= IFNULL(ec.end_date, '9999-12-31')
                                       AND ec.start_date <= IFNULL(NEW.end_date, '9999-12-31')) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Contract period overlaps with an existing active contract.';
    END IF;
END $$


/*
 AUDIT Triggers
 */

/*
* Logs for Employee table
*/

DROP TRIGGER IF EXISTS trg_employee_insert$$

-- 1️⃣ Audit INSERT
CREATE TRIGGER trg_employee_insert
    AFTER INSERT
    ON employee
    FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type,
                           entity_id,
                           action_type,
                           db_user,
                           action_datetime,
                           old_value_snapshot,
                           new_value_snapshot)
    VALUES ('EMPLOYEE',
            NEW.employee_id,
            'INSERT',
            USER(),
            NOW(),
            NULL,
            JSON_OBJECT(
                    'employee_id', NEW.employee_id,
                    'employee_number', NEW.employee_number,
                    'first_name', NEW.first_name,
                    'last_name', NEW.last_name,
                    'email', NEW.email,
                    'phone_number', NEW.phone_number,
                    'hire_date', NEW.hire_date,
                    'employment_status', NEW.employment_status,
                    'primary_work_location_id', NEW.primary_work_location_id,
                    'login_password', '*****', -- hidden password
                    'user_role', NEW.user_role
            ));
END$$


-- Audit UPDATEs
-- 2️⃣ Audit UPDATE
DROP TRIGGER IF EXISTS trg_employee_update$$

CREATE TRIGGER trg_employee_update
    BEFORE UPDATE
    ON employee
    FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type,
                           entity_id,
                           action_type,
                           db_user,
                           action_datetime,
                           old_value_snapshot,
                           new_value_snapshot)
    VALUES ('EMPLOYEE',
            OLD.employee_id,
            'UPDATE',
            USER(),
            NOW(),
            JSON_OBJECT(
                    'employee_id', OLD.employee_id,
                    'employee_number', OLD.employee_number,
                    'first_name', OLD.first_name,
                    'last_name', OLD.last_name,
                    'email', OLD.email,
                    'phone_number', OLD.phone_number,
                    'hire_date', OLD.hire_date,
                    'employment_status', OLD.employment_status,
                    'primary_work_location_id', OLD.primary_work_location_id,
                    'login_password', '*****', -- Password hidden
                    'user_role', OLD.user_role
            ),
            JSON_OBJECT(
                    'employee_id', NEW.employee_id,
                    'employee_number', NEW.employee_number,
                    'first_name', NEW.first_name,
                    'last_name', NEW.last_name,
                    'email', NEW.email,
                    'phone_number', NEW.phone_number,
                    'hire_date', NEW.hire_date,
                    'employment_status', NEW.employment_status,
                    'primary_work_location_id', NEW.primary_work_location_id,
                    'login_password', '*****', -- Password hidden
                    'user_role', NEW.user_role
            ));
END$$

DROP TRIGGER IF EXISTS trg_employee_delete$$
-- 3️⃣ Audit DELETEs
CREATE TRIGGER trg_employee_delete
    BEFORE DELETE
    ON employee
    FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type,
                           entity_id,
                           action_type,
                           db_user,
                           action_datetime,
                           old_value_snapshot,
                           new_value_snapshot)
    VALUES ('EMPLOYEE',
            OLD.employee_id,
            'DELETE',
            USER(),
            NOW(),
            JSON_OBJECT(
                    'employee_id', OLD.employee_id,
                    'employee_number', OLD.employee_number,
                    'first_name', OLD.first_name,
                    'last_name', OLD.last_name,
                    'email', OLD.email,
                    'phone_number', OLD.phone_number,
                    'hire_date', OLD.hire_date,
                    'employment_status', OLD.employment_status,
                    'primary_work_location_id', OLD.primary_work_location_id,
                    'login_password', '*****', -- Password hidden
                    'user_role', OLD.user_role
            ),
            NULL);
END$$

DELIMITER ;

/* TEST above triggers */

INSERT INTO employee (employee_number, first_name, last_name, email, login_password, user_role,
                      phone_number, hire_date, employment_status, primary_work_location_id)
VALUES ('EMP00999', 'First999', 'Last999', 'employee999@shift.dk',
        'f8262c2c54195e78753bf48d51b2e6895493d97f514d933d4847120ffdb39ee4', 'Employee', '+45 50000101', '2020-09-19', 'ACTIVE',
        1);

SET @new_employee_id = LAST_INSERT_ID();

UPDATE employee
SET employment_status = 'INACTIVE'
WHERE employee_id = @new_employee_id
LIMIT 1;

DELETE
FROM employee
WHERE employee_id = @new_employee_id
LIMIT 1;

DELIMITER $$

-- Department Audit
DROP TRIGGER IF EXISTS trg_department_insert$$
CREATE TRIGGER trg_department_insert AFTER INSERT ON department FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('DEPARTMENT', NEW.department_id, 'INSERT', USER(), NOW(), NULL, JSON_OBJECT('department_id', NEW.department_id, 'department_name', NEW.department_name, 'is_active', NEW.is_active));
END$$

DROP TRIGGER IF EXISTS trg_department_update$$
CREATE TRIGGER trg_department_update BEFORE UPDATE ON department FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('DEPARTMENT', OLD.department_id, 'UPDATE', USER(), NOW(), 
    JSON_OBJECT('department_id', OLD.department_id, 'department_name', OLD.department_name, 'is_active', OLD.is_active),
    JSON_OBJECT('department_id', NEW.department_id, 'department_name', NEW.department_name, 'is_active', NEW.is_active));
END$$

DROP TRIGGER IF EXISTS trg_department_delete$$
CREATE TRIGGER trg_department_delete BEFORE DELETE ON department FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('DEPARTMENT', OLD.department_id, 'DELETE', USER(), NOW(), JSON_OBJECT('department_id', OLD.department_id, 'department_name', OLD.department_name, 'is_active', OLD.is_active), NULL);
END$$

-- Work Location Audit
DROP TRIGGER IF EXISTS trg_work_location_insert$$
CREATE TRIGGER trg_work_location_insert AFTER INSERT ON work_location FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('WORK_LOCATION', NEW.work_location_id, 'INSERT', USER(), NOW(), NULL, JSON_OBJECT('work_location_id', NEW.work_location_id, 'location_name', NEW.location_name, 'city', NEW.city));
END$$

DROP TRIGGER IF EXISTS trg_work_location_update$$
CREATE TRIGGER trg_work_location_update BEFORE UPDATE ON work_location FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('WORK_LOCATION', OLD.work_location_id, 'UPDATE', USER(), NOW(), JSON_OBJECT('work_location_id', OLD.work_location_id), JSON_OBJECT('work_location_id', NEW.work_location_id));
END$$

DROP TRIGGER IF EXISTS trg_work_location_delete$$
CREATE TRIGGER trg_work_location_delete BEFORE DELETE ON work_location FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('WORK_LOCATION', OLD.work_location_id, 'DELETE', USER(), NOW(), JSON_OBJECT('work_location_id', OLD.work_location_id), NULL);
END$$

-- User Role Audit
# DROP TRIGGER IF EXISTS trg_user_role_insert$$
# CREATE TRIGGER trg_user_role_insert AFTER INSERT ON user_role FOR EACH ROW
# BEGIN
#     INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
#     VALUES ('USER_ROLE', NEW.user_role_id, 'INSERT', USER(), NOW(), NULL, JSON_OBJECT('user_role_id', NEW.user_role_id, 'user_role_name', NEW.user_role_name));
# END$$
#
# DROP TRIGGER IF EXISTS trg_user_role_update$$
# CREATE TRIGGER trg_user_role_update BEFORE UPDATE ON user_role FOR EACH ROW
# BEGIN
#     INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
#     VALUES ('USER_ROLE', OLD.user_role_id, 'UPDATE', USER(), NOW(), JSON_OBJECT('user_role_name', OLD.user_role_name), JSON_OBJECT('user_role_name', NEW.user_role_name));
# END$$
#
# DROP TRIGGER IF EXISTS trg_user_role_delete$$
# CREATE TRIGGER trg_user_role_delete BEFORE DELETE ON user_role FOR EACH ROW
# BEGIN
#     INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
#     VALUES ('USER_ROLE', OLD.user_role_id, 'DELETE', USER(), NOW(), JSON_OBJECT('user_role_id', OLD.user_role_id), NULL);
# END$$

-- Employee Contract Audit
DROP TRIGGER IF EXISTS trg_employee_contract_insert$$
CREATE TRIGGER trg_employee_contract_insert AFTER INSERT ON employee_contract FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('EMPLOYEE_CONTRACT', NEW.contract_id, 'INSERT', USER(), NOW(), NULL, JSON_OBJECT('contract_id', NEW.contract_id, 'employee_id', NEW.employee_id));
END$$

DROP TRIGGER IF EXISTS trg_employee_contract_update$$
CREATE TRIGGER trg_employee_contract_update BEFORE UPDATE ON employee_contract FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('EMPLOYEE_CONTRACT', OLD.contract_id, 'UPDATE', USER(), NOW(), JSON_OBJECT('contract_id', OLD.contract_id), JSON_OBJECT('contract_id', NEW.contract_id));
END$$

DROP TRIGGER IF EXISTS trg_employee_contract_delete$$
CREATE TRIGGER trg_employee_contract_delete BEFORE DELETE ON employee_contract FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('EMPLOYEE_CONTRACT', OLD.contract_id, 'DELETE', USER(), NOW(), JSON_OBJECT('contract_id', OLD.contract_id), NULL);
END$$

-- Job Role Audit
DROP TRIGGER IF EXISTS trg_job_role_insert$$
CREATE TRIGGER trg_job_role_insert AFTER INSERT ON job_role FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('JOB_ROLE', NEW.job_role_id, 'INSERT', USER(), NOW(), NULL, JSON_OBJECT('role_name', NEW.role_name));
END$$

DROP TRIGGER IF EXISTS trg_job_role_update$$
CREATE TRIGGER trg_job_role_update BEFORE UPDATE ON job_role FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('JOB_ROLE', OLD.job_role_id, 'UPDATE', USER(), NOW(), JSON_OBJECT('role_name', OLD.role_name), JSON_OBJECT('role_name', NEW.role_name));
END$$

DROP TRIGGER IF EXISTS trg_job_role_delete$$
CREATE TRIGGER trg_job_role_delete BEFORE DELETE ON job_role FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('JOB_ROLE', OLD.job_role_id, 'DELETE', USER(), NOW(), JSON_OBJECT('job_role_id', OLD.job_role_id), NULL);
END$$

-- Employee Job Role Audit
DROP TRIGGER IF EXISTS trg_employee_job_role_insert$$
CREATE TRIGGER trg_employee_job_role_insert AFTER INSERT ON employee_job_role FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('EMPLOYEE_JOB_ROLE', NEW.employee_job_role_id, 'INSERT', USER(), NOW(), NULL, JSON_OBJECT('employee_id', NEW.employee_id, 'job_role_id', NEW.job_role_id));
END$$

DROP TRIGGER IF EXISTS trg_employee_job_role_update$$
CREATE TRIGGER trg_employee_job_role_update BEFORE UPDATE ON employee_job_role FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('EMPLOYEE_JOB_ROLE', OLD.employee_job_role_id, 'UPDATE', USER(), NOW(), JSON_OBJECT('employee_job_role_id', OLD.employee_job_role_id), JSON_OBJECT('employee_job_role_id', NEW.employee_job_role_id));
END$$

DROP TRIGGER IF EXISTS trg_employee_job_role_delete$$
CREATE TRIGGER trg_employee_job_role_delete BEFORE DELETE ON employee_job_role FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('EMPLOYEE_JOB_ROLE', OLD.employee_job_role_id, 'DELETE', USER(), NOW(), JSON_OBJECT('employee_job_role_id', OLD.employee_job_role_id), NULL);
END$$

-- Shift Audit
DROP TRIGGER IF EXISTS trg_shift_insert$$
CREATE TRIGGER trg_shift_insert AFTER INSERT ON shift FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('SHIFT', NEW.shift_id, 'INSERT', USER(), NOW(), NULL, JSON_OBJECT('shift_name', NEW.shift_name, 'start_datetime', NEW.start_datetime));
END$$

DROP TRIGGER IF EXISTS trg_shift_update$$
CREATE TRIGGER trg_shift_update BEFORE UPDATE ON shift FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('SHIFT', OLD.shift_id, 'UPDATE', USER(), NOW(), JSON_OBJECT('shift_id', OLD.shift_id), JSON_OBJECT('shift_id', NEW.shift_id));
END$$

DROP TRIGGER IF EXISTS trg_shift_delete$$
CREATE TRIGGER trg_shift_delete BEFORE DELETE ON shift FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('SHIFT', OLD.shift_id, 'DELETE', USER(), NOW(), JSON_OBJECT('shift_id', OLD.shift_id), NULL);
END$$

-- Shift Required Job Role Audit
DROP TRIGGER IF EXISTS trg_shift_required_job_role_insert$$
CREATE TRIGGER trg_shift_required_job_role_insert AFTER INSERT ON shift_required_job_role FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('SHIFT_REQUIRED_JOB_ROLE', NEW.shift_required_job_role_id, 'INSERT', USER(), NOW(), NULL, JSON_OBJECT('shift_id', NEW.shift_id, 'job_role_id', NEW.job_role_id));
END$$

DROP TRIGGER IF EXISTS trg_shift_required_job_role_update$$
CREATE TRIGGER trg_shift_required_job_role_update BEFORE UPDATE ON shift_required_job_role FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('SHIFT_REQUIRED_JOB_ROLE', OLD.shift_required_job_role_id, 'UPDATE', USER(), NOW(), JSON_OBJECT('shift_required_job_role_id', OLD.shift_required_job_role_id), JSON_OBJECT('shift_required_job_role_id', NEW.shift_required_job_role_id));
END$$

DROP TRIGGER IF EXISTS trg_shift_required_job_role_delete$$
CREATE TRIGGER trg_shift_required_job_role_delete BEFORE DELETE ON shift_required_job_role FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('SHIFT_REQUIRED_JOB_ROLE', OLD.shift_required_job_role_id, 'DELETE', USER(), NOW(), JSON_OBJECT('shift_required_job_role_id', OLD.shift_required_job_role_id), NULL);
END$$

-- Shift Assignment Audit
DROP TRIGGER IF EXISTS trg_shift_assignment_insert$$
CREATE TRIGGER trg_shift_assignment_insert AFTER INSERT ON shift_assignment FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('SHIFT_ASSIGNMENT', NEW.shift_assignment_id, 'INSERT', USER(), NOW(), NULL, JSON_OBJECT('shift_id', NEW.shift_id, 'employee_id', NEW.employee_id));
END$$

DROP TRIGGER IF EXISTS trg_shift_assignment_update$$
CREATE TRIGGER trg_shift_assignment_update BEFORE UPDATE ON shift_assignment FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('SHIFT_ASSIGNMENT', OLD.shift_assignment_id, 'UPDATE', USER(), NOW(), JSON_OBJECT('assignment_status', OLD.assignment_status), JSON_OBJECT('assignment_status', NEW.assignment_status));
END$$

DROP TRIGGER IF EXISTS trg_shift_assignment_delete$$
CREATE TRIGGER trg_shift_assignment_delete BEFORE DELETE ON shift_assignment FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('SHIFT_ASSIGNMENT', OLD.shift_assignment_id, 'DELETE', USER(), NOW(), JSON_OBJECT('shift_assignment_id', OLD.shift_assignment_id), NULL);
END$$

-- Shift Approval Audit
DROP TRIGGER IF EXISTS trg_shift_approval_insert$$
CREATE TRIGGER trg_shift_approval_insert AFTER INSERT ON shift_approval FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('SHIFT_APPROVAL', NEW.shift_approval_id, 'INSERT', USER(), NOW(), NULL, JSON_OBJECT('shift_assignment_id', NEW.shift_assignment_id, 'decision', NEW.decision));
END$$

DROP TRIGGER IF EXISTS trg_shift_approval_update$$
CREATE TRIGGER trg_shift_approval_update BEFORE UPDATE ON shift_approval FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('SHIFT_APPROVAL', OLD.shift_approval_id, 'UPDATE', USER(), NOW(), JSON_OBJECT('decision', OLD.decision), JSON_OBJECT('decision', NEW.decision));
END$$

DROP TRIGGER IF EXISTS trg_shift_approval_delete$$
CREATE TRIGGER trg_shift_approval_delete BEFORE DELETE ON shift_approval FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('SHIFT_APPROVAL', OLD.shift_approval_id, 'DELETE', USER(), NOW(), JSON_OBJECT('shift_approval_id', OLD.shift_approval_id), NULL);
END$$

-- Shift Swap Audit
DROP TRIGGER IF EXISTS trg_shift_swap_insert$$
CREATE TRIGGER trg_shift_swap_insert AFTER INSERT ON shift_swap FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('SHIFT_SWAP', NEW.shift_swap_id, 'INSERT', USER(), NOW(), NULL, JSON_OBJECT('employee_from_id', NEW.employee_from_id, 'employee_to_id', NEW.employee_to_id));
END$$

DROP TRIGGER IF EXISTS trg_shift_swap_update$$
CREATE TRIGGER trg_shift_swap_update BEFORE UPDATE ON shift_swap FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('SHIFT_SWAP', OLD.shift_swap_id, 'UPDATE', USER(), NOW(), JSON_OBJECT('swap_status', OLD.swap_status), JSON_OBJECT('swap_status', NEW.swap_status));
END$$

DROP TRIGGER IF EXISTS trg_shift_swap_delete$$
CREATE TRIGGER trg_shift_swap_delete BEFORE DELETE ON shift_swap FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('SHIFT_SWAP', OLD.shift_swap_id, 'DELETE', USER(), NOW(), JSON_OBJECT('shift_swap_id', OLD.shift_swap_id), NULL);
END$$

-- Shift Swap Approval Audit
DROP TRIGGER IF EXISTS trg_shift_swap_approval_insert$$
CREATE TRIGGER trg_shift_swap_approval_insert AFTER INSERT ON shift_swap_approval FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('SHIFT_SWAP_APPROVAL', NEW.shift_swap_approval_id, 'INSERT', USER(), NOW(), NULL, JSON_OBJECT('shift_swap_id', NEW.shift_swap_id, 'decision', NEW.decision));
END$$

DROP TRIGGER IF EXISTS trg_shift_swap_approval_update$$
CREATE TRIGGER trg_shift_swap_approval_update BEFORE UPDATE ON shift_swap_approval FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('SHIFT_SWAP_APPROVAL', OLD.shift_swap_approval_id, 'UPDATE', USER(), NOW(), JSON_OBJECT('decision', OLD.decision), JSON_OBJECT('decision', NEW.decision));
END$$

DROP TRIGGER IF EXISTS trg_shift_swap_approval_delete$$
CREATE TRIGGER trg_shift_swap_approval_delete BEFORE DELETE ON shift_swap_approval FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('SHIFT_SWAP_APPROVAL', OLD.shift_swap_approval_id, 'DELETE', USER(), NOW(), JSON_OBJECT('shift_swap_approval_id', OLD.shift_swap_approval_id), NULL);
END$$

-- Leave Type Audit
DROP TRIGGER IF EXISTS trg_leave_type_insert$$
CREATE TRIGGER trg_leave_type_insert AFTER INSERT ON leave_type FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('LEAVE_TYPE', NEW.leave_type_id, 'INSERT', USER(), NOW(), NULL, JSON_OBJECT('leave_type_name', NEW.leave_type_name));
END$$

DROP TRIGGER IF EXISTS trg_leave_type_update$$
CREATE TRIGGER trg_leave_type_update BEFORE UPDATE ON leave_type FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('LEAVE_TYPE', OLD.leave_type_id, 'UPDATE', USER(), NOW(), JSON_OBJECT('leave_type_name', OLD.leave_type_name), JSON_OBJECT('leave_type_name', NEW.leave_type_name));
END$$

DROP TRIGGER IF EXISTS trg_leave_type_delete$$
CREATE TRIGGER trg_leave_type_delete BEFORE DELETE ON leave_type FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('LEAVE_TYPE', OLD.leave_type_id, 'DELETE', USER(), NOW(), JSON_OBJECT('leave_type_id', OLD.leave_type_id), NULL);
END$$

-- Leave Request Audit
DROP TRIGGER IF EXISTS trg_leave_request_insert$$
CREATE TRIGGER trg_leave_request_insert AFTER INSERT ON leave_request FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('LEAVE_REQUEST', NEW.leave_request_id, 'INSERT', USER(), NOW(), NULL, JSON_OBJECT('employee_id', NEW.employee_id, 'leave_type_id', NEW.leave_type_id));
END$$

DROP TRIGGER IF EXISTS trg_leave_request_update$$
CREATE TRIGGER trg_leave_request_update BEFORE UPDATE ON leave_request FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('LEAVE_REQUEST', OLD.leave_request_id, 'UPDATE', USER(), NOW(), JSON_OBJECT('request_status', OLD.request_status), JSON_OBJECT('request_status', NEW.request_status));
END$$

DROP TRIGGER IF EXISTS trg_leave_request_delete$$
CREATE TRIGGER trg_leave_request_delete BEFORE DELETE ON leave_request FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('LEAVE_REQUEST', OLD.leave_request_id, 'DELETE', USER(), NOW(), JSON_OBJECT('leave_request_id', OLD.leave_request_id), NULL);
END$$

-- Leave Approval Audit
DROP TRIGGER IF EXISTS trg_leave_approval_insert$$
CREATE TRIGGER trg_leave_approval_insert AFTER INSERT ON leave_approval FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('LEAVE_APPROVAL', NEW.leave_approval_id, 'INSERT', USER(), NOW(), NULL, JSON_OBJECT('leave_request_id', NEW.leave_request_id, 'decision', NEW.decision));
END$$

DROP TRIGGER IF EXISTS trg_leave_approval_update$$
CREATE TRIGGER trg_leave_approval_update BEFORE UPDATE ON leave_approval FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('LEAVE_APPROVAL', OLD.leave_approval_id, 'UPDATE', USER(), NOW(), JSON_OBJECT('decision', OLD.decision), JSON_OBJECT('decision', NEW.decision));
END$$

DROP TRIGGER IF EXISTS trg_leave_approval_delete$$
CREATE TRIGGER trg_leave_approval_delete BEFORE DELETE ON leave_approval FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('LEAVE_APPROVAL', OLD.leave_approval_id, 'DELETE', USER(), NOW(), JSON_OBJECT('leave_approval_id', OLD.leave_approval_id), NULL);
END$$

-- Leave Ledger Audit
DROP TRIGGER IF EXISTS trg_leave_ledger_insert$$
CREATE TRIGGER trg_leave_ledger_insert AFTER INSERT ON leave_ledger FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('LEAVE_LEDGER', NEW.leave_ledger_id, 'INSERT', USER(), NOW(), NULL, JSON_OBJECT('employee_id', NEW.employee_id, 'change_amount_days', NEW.change_amount_days));
END$$

DROP TRIGGER IF EXISTS trg_leave_ledger_update$$
CREATE TRIGGER trg_leave_ledger_update BEFORE UPDATE ON leave_ledger FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type, entity_id, action_type, db_user, action_datetime, old_value_snapshot, new_value_snapshot)
    VALUES ('LEAVE_LEDGER', OLD.leave_ledger_id, 'UPDATE', USER(), NOW(), JSON_OBJECT('change_amount_days', OLD.change_amount_days), JSON_OBJECT('change_amount_days', NEW.change_amount_days));
END$$

DELIMITER ;

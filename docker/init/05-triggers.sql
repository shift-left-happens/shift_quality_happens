USE shift_happens;

DELIMITER $$

-- =========================================
-- TRIGGER: Validate and process leave approvals
-- =========================================
CREATE TRIGGER trg_leaveapproval_before_insert
    BEFORE INSERT
    ON leave_approval
    FOR EACH ROW
BEGIN
    DECLARE v_employee_id INT;
    DECLARE v_leave_type_id INT;
    DECLARE v_days_requested DECIMAL(6, 2);
    DECLARE v_leave_balance DECIMAL(6, 2);
    DECLARE v_is_manager INT;
    DECLARE v_request_status VARCHAR(20);

    /* Fetch and lock leave request */
    SELECT employee_id,
           leave_type_id,
           DATEDIFF(end_date, start_date) + 1,
           request_status
    INTO v_employee_id,
        v_leave_type_id,
        v_days_requested,
        v_request_status
    FROM leave_request
    WHERE leave_request_id = NEW.leave_request_id
    LIMIT 1
    FOR UPDATE;

    IF v_employee_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Leave request does not exist';
    END IF;

    IF v_request_status <> 'PENDING' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Leave request is not pending';
    END IF;

    /* Validate approver */
    SELECT fk_user_role_id
    INTO v_is_manager
    FROM employee
    WHERE employee_id = NEW.approver_employee_id
    LIMIT 1;

    IF v_is_manager IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Approver does not exist';
    END IF;

    IF v_is_manager <> 3 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Only managers can approve leave requests';
    END IF;

    /* Only process if APPROVED */
    IF NEW.decision = 'APPROVED' THEN

        SELECT COALESCE(SUM(change_amount_days), 0)
        INTO v_leave_balance
        FROM leave_ledger
        WHERE employee_id = v_employee_id
          AND leave_type_id = v_leave_type_id;

        IF v_leave_balance < v_days_requested THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Insufficient leave balance';
        END IF;

        UPDATE leave_request
        SET request_status = 'APPROVED'
        WHERE leave_request_id = NEW.leave_request_id
        LIMIT 1;

        INSERT INTO leave_ledger (employee_id,
                                  leave_type_id,
                                  change_amount_days,
                                  transaction_type,
                                  reference_entity_type,
                                  reference_entity_id,
                                  transaction_datetime)
        VALUES (v_employee_id,
                v_leave_type_id,
                -v_days_requested,
                'USAGE',
                'LeaveRequest',
                NEW.leave_request_id,
                NOW());

    END IF;

END$$

-- =========================================
-- TRIGGER: Prevent deletion of leave ledger entries
-- =========================================
CREATE TRIGGER trg_no_delete_leave_ledger
    BEFORE DELETE
    ON leave_ledger
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Ledger entries cannot be deleted';
END$$

-- =========================================
-- TRIGGER: Days cannot be 0
-- =========================================
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

-- =========================================
-- TRIGGER: Prevent changing employee on leave request
-- =========================================
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

-- =========================================
-- TRIGGER: Validate employee password length (INSERT)
-- =========================================
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
END$$

-- =========================================
-- TRIGGER: Validate employee password length (UPDATE)
-- =========================================
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
END$$

-- =========================================
-- TRIGGER: Validate contract fields (INSERT)
-- =========================================
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
END$$

-- =========================================
-- TRIGGER: Validate contract fields (UPDATE)
-- =========================================
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
END$$

-- =========================================
-- TRIGGER: Prevent overlapping active contracts (INSERT)
-- =========================================
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
                                       AND NEW.start_date <= IFNULL(ec.end_date, '9999-12-31')
                                       AND ec.start_date <= IFNULL(NEW.end_date, '9999-12-31')
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Contract period overlaps with an existing active contract.';
    END IF;
END$$

-- =========================================
-- TRIGGER: Prevent overlapping active contracts (UPDATE)
-- =========================================
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
END$$

-- =========================================
-- AUDIT TRIGGERS: Employee table
-- =========================================
DROP TRIGGER IF EXISTS trg_employee_insert$$
CREATE TRIGGER trg_employee_insert
    AFTER INSERT
    ON employee
    FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type,
                           entity_id,
                           action_type,
                           performed_by_employee_id,
                           action_datetime,
                           old_value_snapshot,
                           new_value_snapshot)
    VALUES ('EMPLOYEE',
            NEW.employee_id,
            'INSERT',
            NULL,
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
                    'login_password', '*****',
                    'fk_user_role_id', NEW.fk_user_role_id
            ));
END$$

DROP TRIGGER IF EXISTS trg_employee_update$$
CREATE TRIGGER trg_employee_update
    BEFORE UPDATE
    ON employee
    FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type,
                           entity_id,
                           action_type,
                           performed_by_employee_id,
                           action_datetime,
                           old_value_snapshot,
                           new_value_snapshot)
    VALUES ('EMPLOYEE',
            OLD.employee_id,
            'UPDATE',
            NULL,
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
                    'login_password', '*****',
                    'fk_user_role_id', OLD.fk_user_role_id
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
                    'login_password', '*****',
                    'fk_user_role_id', NEW.fk_user_role_id
            ));
END$$

DROP TRIGGER IF EXISTS trg_employee_delete$$
CREATE TRIGGER trg_employee_delete
    BEFORE DELETE
    ON employee
    FOR EACH ROW
BEGIN
    INSERT INTO audit_log (entity_type,
                           entity_id,
                           action_type,
                           performed_by_employee_id,
                           action_datetime,
                           old_value_snapshot,
                           new_value_snapshot)
    VALUES ('EMPLOYEE',
            OLD.employee_id,
            'DELETE',
            NULL,
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
                    'login_password', '*****',
                    'fk_user_role_id', OLD.fk_user_role_id
            ),
            NULL);
END$$

DELIMITER ;

-- =========================================
-- TEST: Leave approval trigger
-- =========================================

SET @random_employee = (SELECT employee.employee_id
                        FROM employee
                        WHERE fk_user_role_id = 2
                        ORDER BY RAND()
                        LIMIT 1);

SET @random_manager = (SELECT employee.employee_id
                       FROM employee
                       WHERE fk_user_role_id = 3
                       ORDER BY RAND()
                       LIMIT 1);

INSERT INTO `leave_ledger` (`employee_id`, `leave_type_id`, `change_amount_days`, `transaction_type`,
                            `reference_entity_type`, reference_entity_id, `transaction_datetime`)
VALUES (@random_employee, 1, 10, 'ACCRUAL', 'Employee', @random_manager, '2026-02-24 11:00:00');

ALTER TABLE `leave_request`
    MODIFY requested_datetime DATETIME DEFAULT CURRENT_TIMESTAMP;

INSERT INTO `leave_request` (`employee_id`, `leave_type_id`, `start_date`, `end_date`, `request_status`, `reason`)
VALUES (@random_employee, 1, '2026-05-05', '2026-05-08', 'PENDING', 'Vacation');
SET @new_leave_request_id = LAST_INSERT_ID();

INSERT INTO leave_approval (leave_request_id,
                            approver_employee_id,
                            decision,
                            decision_datetime)
VALUES (@new_leave_request_id,
        @random_manager,
        'APPROVED',
        NOW());

-- =========================================
-- TEST: Audit triggers
-- =========================================

INSERT INTO employee (employee_number, first_name, last_name, email, login_password, fk_user_role_id,
                      phone_number, hire_date, employment_status, primary_work_location_id)
VALUES ('EMP00999', 'First999', 'Last999', 'employee999@shift.dk',
        'f8262c2c54195e78753bf48d51b2e6895493d97f514d933d4847120ffdb39ee4', 2, '+45 50000101', '2020-09-19', 'ACTIVE',
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

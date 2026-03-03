USE
shift_happens;
DELIMITER $$

CREATE TRIGGER trg_leaveapproval_before_insert
    BEFORE INSERT ON leave_approval
    FOR EACH ROW
BEGIN
    DECLARE v_employee_id INT;
    DECLARE v_leave_type_id INT;
    DECLARE v_days_requested DECIMAL(6,2);
    DECLARE v_leave_balance DECIMAL(6,2);
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
    LIMIT 1 FOR UPDATE;

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

        INSERT INTO leave_ledger (
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

/*Test the leave approval trigger works*/

/*Find random employee with role 2 (regular employee)*/
SET @random_employee = (SELECT employee.employee_id FROM employee WHERE fk_user_role_id = 2 ORDER BY RAND() LIMIT 1);
/*Find random employee with role 3 (Manager, the required role for approvals)*/
SET @random_manager = (SELECT employee.employee_id FROM employee WHERE fk_user_role_id = 3 ORDER BY RAND() LIMIT 1);
/*Add additional vacation time for a test employee from above manager*/
INSERT INTO `leave_ledger` (`employee_id`, `leave_type_id`, `change_amount_days`, `transaction_type`, `reference_entity_type`, reference_entity_id, `transaction_datetime`) VALUES (@random_employee, 1, 10, 'ACCRUAL', 'Employee', @random_manager, '2026-02-24 11:00:00');
/*set default datetime for new entries to simplify INSERTs*/
ALTER TABLE `leave_request`
    MODIFY requested_datetime DATETIME DEFAULT CURRENT_TIMESTAMP;
/*Add leave request*/
INSERT INTO `leave_request` (`employee_id`, `leave_type_id`, `start_date`, `end_date`, `request_status`, `reason`) VALUES (@random_employee, 1, '2026-05-05', '2026-05-08', 'PENDING', 'Vacation');
SET @new_leave_request_id = LAST_INSERT_ID();

INSERT INTO leave_approval (
    leave_request_id,
    approver_employee_id,
    decision,
    decision_datetime
)
VALUES (
           @new_leave_request_id,              /* the leave_request_id of employee 96*/
           @random_manager,              /* NOT a manager*/
           'APPROVED',
           NOW()
       );


/*Prevent deletion of leave ledger entries*/
CREATE TRIGGER trg_no_delete_leave_ledger
    BEFORE DELETE ON leave_ledger
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Ledger entries cannot be deleted';
END;
/* Test DELETE
START TRANSACTION
DELETE FROM leave_ledger ORDER BY leave_ledger_id DESC LIMIT 1;
ROLLBACK;
 */

/*Days cannot be 0*/
CREATE TRIGGER trg_validate_ledger_amount
    BEFORE INSERT ON leave_ledger
    FOR EACH ROW
BEGIN
    IF NEW.change_amount_days = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Ledger change cannot be zero';
    END IF;
END;

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
    BEFORE UPDATE ON leave_request
    FOR EACH ROW
BEGIN
    IF OLD.employee_id <> NEW.employee_id THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Employee cannot be changed';
    END IF;
END;

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

DELIMITER $$
DROP TRIGGER IF EXISTS trg_validate_employee_ins;
CREATE TRIGGER trg_validate_employee_ins
    BEFORE INSERT ON employee
    FOR EACH ROW
BEGIN
    IF LENGTH(NEW.login_password) < 8 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Password needs to be longer than 8 chars.';
    END IF;
end $$
DROP TRIGGER IF EXISTS trg_validate_employee_update;
CREATE TRIGGER trg_validate_employee_update
    BEFORE UPDATE ON employee
    FOR EACH ROW
BEGIN
    IF LENGTH(NEW.login_password) < 8 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Password needs to be longer than 8 chars.';
    END IF;
end $$

DROP TRIGGER IF EXISTS trg_validate_contract_ins;
CREATE TRIGGER trg_validate_contract_ins
    BEFORE INSERT ON employee_contract
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

DROP TRIGGER IF EXISTS trg_validate_contract_update;
CREATE TRIGGER trg_validate_contract_update
    BEFORE UPDATE ON employee_contract
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

DROP TRIGGER IF EXISTS trg_no_contract_overlap_ins;

CREATE TRIGGER trg_no_contract_overlap_ins
    BEFORE INSERT ON employee_contract
    FOR EACH ROW
BEGIN
    IF NEW.is_active = 1 AND EXISTS (
        SELECT 1
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

DROP TRIGGER IF EXISTS trg_no_contract_overlap_upd;

CREATE TRIGGER trg_no_contract_overlap_upd
    BEFORE UPDATE ON employee_contract
    FOR EACH ROW
BEGIN
    IF NEW.is_active = 1 AND EXISTS (
        SELECT 1
        FROM employee_contract ec
        WHERE ec.employee_id = NEW.employee_id
          AND ec.contract_id != OLD.contract_id
          AND ec.is_active = 1
          AND NEW.start_date <= IFNULL(ec.end_date, '9999-12-31')
          AND ec.start_date <= IFNULL(NEW.end_date, '9999-12-31')
    ) THEN
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

DROP TRIGGER IF EXISTS trg_employee_insert;

DELIMITER $$

-- 1️⃣ Audit INSERT
CREATE TRIGGER trg_employee_insert
    AFTER INSERT ON employee
    FOR EACH ROW
BEGIN
    INSERT INTO audit_log (
        entity_type,
        entity_id,
        action_type,
        performed_by_employee_id,
        action_datetime,
        old_value_snapshot,
        new_value_snapshot
    )
    VALUES (
               'EMPLOYEE',
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
                       'login_password', '*****', -- hidden password
                       'fk_user_role_id', NEW.fk_user_role_id
               )
           );
END$$



-- Audit UPDATEs
-- 2️⃣ Audit UPDATE
DROP TRIGGER IF EXISTS trg_employee_update;

CREATE TRIGGER trg_employee_update
    BEFORE UPDATE ON employee
    FOR EACH ROW
BEGIN
    INSERT INTO audit_log (
        entity_type,
        entity_id,
        action_type,
        performed_by_employee_id,
        action_datetime,
        old_value_snapshot,
        new_value_snapshot
    )
    VALUES (
               'EMPLOYEE',
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
                       'login_password', '*****', -- Password hidden
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
                       'login_password', '*****', -- Password hidden
                       'fk_user_role_id', NEW.fk_user_role_id
               )
           );
END$$

DROP TRIGGER IF EXISTS trg_employee_delete;
-- 3️⃣ Audit DELETEs
CREATE TRIGGER trg_employee_delete
    BEFORE DELETE ON employee
    FOR EACH ROW
BEGIN
    INSERT INTO audit_log (
        entity_type,
        entity_id,
        action_type,
        performed_by_employee_id,
        action_datetime,
        old_value_snapshot,
        new_value_snapshot
    )
    VALUES (
               'EMPLOYEE',
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
                       'login_password', '*****', -- Password hidden
                       'fk_user_role_id', OLD.fk_user_role_id
               ),
               NULL
           );
END$$

DELIMITER ;

/* TEST above triggers */

INSERT INTO employee (employee_number, first_name, last_name, email, login_password, fk_user_role_id,
                      phone_number, hire_date, employment_status, primary_work_location_id)
VALUES ('EMP00999', 'First999', 'Last999', 'employee999@shift.dk',
        'f8262c2c54195e78753bf48d51b2e6895493d97f514d933d4847120ffdb39ee4', 2, '+45 50000101', '2020-09-19', 'ACTIVE',
        1);

SET @new_employee_id = LAST_INSERT_ID();

UPDATE employee SET employment_status = 'INACTIVE' WHERE employee_id = @new_employee_id LIMIT 1;

DELETE FROM employee WHERE employee_id = @new_employee_id LIMIT 1;

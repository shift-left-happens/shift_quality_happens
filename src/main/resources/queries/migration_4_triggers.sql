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
DROP TRIGGER IF EXISTS trg_validate_employee_update $$
CREATE TRIGGER trg_validate_employee_update
    BEFORE UPDATE ON employee
    FOR EACH ROW
BEGIN
    IF LENGTH(NEW.login_password) < 8 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Password needs to be longer than 8 chars.';
    END IF;
end $$

DROP TRIGGER IF EXISTS trg_validate_contract_ins $$
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

DROP TRIGGER IF EXISTS trg_validate_contract_update $$
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

DROP TRIGGER IF EXISTS trg_no_contract_overlap_ins $$

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

DROP TRIGGER IF EXISTS trg_no_contract_overlap_upd $$

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
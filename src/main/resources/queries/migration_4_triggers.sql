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
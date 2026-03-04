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
DROP TRIGGER IF EXISTS trg_employee_update $$

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

DROP TRIGGER IF EXISTS trg_employee_delete $$
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

DELIMITER $$

/* TEST above triggers */

INSERT INTO employee (employee_number, first_name, last_name, email, login_password, fk_user_role_id,
                      phone_number, hire_date, employment_status, primary_work_location_id)
VALUES ('EMP00999', 'First999', 'Last999', 'employee999@shift.dk',
        'f8262c2c54195e78753bf48d51b2e6895493d97f514d933d4847120ffdb39ee4', 2, '+45 50000101', '2020-09-19', 'ACTIVE',
        1);

SET @new_employee_id = LAST_INSERT_ID();

UPDATE employee SET employment_status = 'INACTIVE' WHERE employee_id = @new_employee_id LIMIT 1;

DELETE FROM employee WHERE employee_id = @new_employee_id LIMIT 1;

-- =========================================
-- Function 1: Get leave balance for an employee + leave type
-- Returns the net balance (ACCRUALs positive, USAGEs negative)
-- =========================================
DELIMITER $$

CREATE FUNCTION fn_leave_balance(p_employee_id INT, p_leave_type_id INT)
RETURNS DECIMAL(6,2)
NOT DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE result DECIMAL(6,2);

    SELECT COALESCE(SUM(change_amount_days), 0)
    INTO result
    FROM leave_ledger
    WHERE employee_id = p_employee_id
      AND leave_type_id = p_leave_type_id;

    RETURN result;
END$$

DELIMITER ;

-- =========================================
-- Function 2: Count shift assignments for an employee
-- Pass a status like 'COMPLETED' to filter, or NULL to count all
-- =========================================
DELIMITER $$

CREATE FUNCTION fn_count_shift_assignments(p_employee_id INT, p_status VARCHAR(50))
RETURNS INT
NOT DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE result INT;

    SELECT COUNT(*)
    INTO result
    FROM shift_assignment
    WHERE employee_id = p_employee_id
      AND (p_status IS NULL OR assignment_status = p_status);

    RETURN result;
END$$

DELIMITER ;

-- =========================================
-- Function 3: Total scheduled hours for an employee in a date range
-- Joins shift_assignment -> shift, sums hours for non-cancelled shifts
-- =========================================
DELIMITER $$

CREATE FUNCTION fn_scheduled_hours_in_period(p_employee_id INT, p_start DATE, p_end DATE)
RETURNS DECIMAL(10,2)
NOT DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE result DECIMAL(10,2);

    SELECT COALESCE(SUM(TIMESTAMPDIFF(MINUTE, s.start_datetime, s.end_datetime)) / 60.0, 0)
    INTO result
    FROM shift_assignment sa
    JOIN shift s ON sa.shift_id = s.shift_id
    WHERE sa.employee_id = p_employee_id
      AND s.start_datetime >= p_start
      AND s.end_datetime   <= DATE_ADD(p_end, INTERVAL 1 DAY)
      AND s.shift_status  != 'CANCELLED';

    RETURN result;
END$$

DELIMITER ;

-- =========================================
-- Function 4: Check if an employee is on approved leave on a given date
-- Returns 1 (true) or 0 (false)
-- =========================================
DELIMITER $$

CREATE FUNCTION fn_is_employee_on_leave(p_employee_id INT, p_check_date DATE)
RETURNS TINYINT
NOT DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE result TINYINT;

    SELECT IF(COUNT(*) > 0, 1, 0)
    INTO result
    FROM leave_request
    WHERE employee_id    = p_employee_id
      AND request_status = 'APPROVED'
      AND p_check_date BETWEEN start_date AND end_date;

    RETURN result;
END$$

DELIMITER ;

-- =========================================
-- Usage examples:
-- SELECT fn_leave_balance(5, 1);                                        -- vacation balance for employee 5
-- SELECT fn_count_shift_assignments(10, NULL);                          -- all assignments for employee 10
-- SELECT fn_count_shift_assignments(10, 'COMPLETED');                   -- only completed ones
-- SELECT fn_scheduled_hours_in_period(10, '2026-02-01', '2026-02-28'); -- hours this month
-- SELECT fn_is_employee_on_leave(10, '2026-03-15');                     -- on leave that day? 1/0
-- =========================================

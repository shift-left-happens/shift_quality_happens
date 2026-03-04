USE shift_happens;

-- =========================================
-- FUNCTION: Calculate leave balance for an employee + leave type
-- =========================================
DELIMITER $$

CREATE FUNCTION fn_get_leave_balance(
    p_employee_id INT,
    p_leave_type_id INT
)
    RETURNS DECIMAL(6, 2)
    DETERMINISTIC
    READS SQL DATA
BEGIN
    DECLARE v_balance DECIMAL(6, 2);

    SELECT COALESCE(SUM(change_amount_days), 0)
    INTO v_balance
    FROM leave_ledger
    WHERE employee_id = p_employee_id
      AND leave_type_id = p_leave_type_id;

    RETURN v_balance;
END$$

DELIMITER ;

-- =========================================
-- PROCEDURE: Submit a leave request (validates balance before inserting)
-- =========================================
DELIMITER $$

CREATE PROCEDURE sp_submit_leave_request(
    IN p_employee_id INT,
    IN p_leave_type_id INT,
    IN p_start_date DATE,
    IN p_end_date DATE,
    IN p_reason TEXT
)
BEGIN
    DECLARE v_days_requested DECIMAL(6, 2);
    DECLARE v_balance DECIMAL(6, 2);

    SET v_days_requested = DATEDIFF(p_end_date, p_start_date) + 1;

    -- Check balance using our function
    SET v_balance = fn_get_leave_balance(p_employee_id, p_leave_type_id);

    IF v_balance < v_days_requested THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Insufficient leave balance to submit this request';
    END IF;

    INSERT INTO leave_request (employee_id, leave_type_id,
                               start_date, end_date,
                               request_status, reason, requested_datetime)
    VALUES (p_employee_id, p_leave_type_id,
            p_start_date, p_end_date,
            'PENDING', p_reason, NOW());

    SELECT LAST_INSERT_ID() AS new_leave_request_id;
END$$

DELIMITER ;

-- =========================================
-- PROCEDURE: Assign employee to shift (checks for conflicts)
-- =========================================
DELIMITER $$

CREATE PROCEDURE sp_assign_employee_to_shift(
    IN p_shift_id INT,
    IN p_employee_id INT
)
BEGIN
    DECLARE v_conflict_count INT;
    DECLARE v_shift_start DATETIME;
    DECLARE v_shift_end DATETIME;

    -- Get the shift times
    SELECT start_datetime, end_datetime
    INTO v_shift_start, v_shift_end
    FROM shift
    WHERE shift_id = p_shift_id;

    -- Check for overlapping shift assignments
    SELECT COUNT(*)
    INTO v_conflict_count
    FROM shift_assignment sa
             JOIN shift s ON sa.shift_id = s.shift_id
    WHERE sa.employee_id = p_employee_id
      AND sa.assignment_status IN ('ASSIGNED', 'CONFIRMED')
      AND s.start_datetime < v_shift_end
      AND s.end_datetime > v_shift_start;

    IF v_conflict_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Employee has a conflicting shift assignment';
    END IF;

    INSERT INTO shift_assignment (shift_id, employee_id,
                                  assignment_status, assigned_datetime)
    VALUES (p_shift_id, p_employee_id,
            'ASSIGNED', NOW());

    SELECT LAST_INSERT_ID() AS new_assignment_id;
END$$

DELIMITER ;

-- =========================================
-- FUNCTION: Count active employees in a department
-- =========================================
DELIMITER $$

CREATE FUNCTION fn_department_headcount(
    p_department_id INT
)
    RETURNS INT
    DETERMINISTIC
    READS SQL DATA
BEGIN
    DECLARE v_count INT;

    SELECT COUNT(*)
    INTO v_count
    FROM employee_contract ec
             JOIN employee e ON ec.employee_id = e.employee_id
    WHERE ec.department_id = p_department_id
      AND ec.is_active = 1
      AND e.employment_status = 'ACTIVE';

    RETURN v_count;
END$$

DELIMITER ;


# DELIMITER $$
#
# DROP PROCEDURE IF EXISTS approve_leave_request $$
#
# CREATE PROCEDURE approve_leave_request(
#     IN p_leave_request_id INT,
#     IN p_approver_employee_id INT,
#     IN p_decision VARCHAR(20),
#     IN p_leave_comment TEXT
# )
# BEGIN
#     DECLARE v_employee_id INT;
#     DECLARE v_leave_type_id INT;
#     DECLARE v_days_requested DECIMAL(6,2);
#     DECLARE v_leave_balance DECIMAL(6,2);
#     DECLARE v_is_manager INT;
#     DECLARE v_request_status VARCHAR(20);
#
#     DECLARE EXIT HANDLER FOR SQLEXCEPTION
#         BEGIN
#             ROLLBACK;
#             RESIGNAL;
#         END;
#
#     START TRANSACTION;
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
#     WHERE leave_request_id = p_leave_request_id
#     LIMIT 1
#     FOR UPDATE;
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
#     WHERE employee_id = p_approver_employee_id
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
#     /* Insert approval record */
#     INSERT INTO leave_approval (
#         leave_request_id,
#         approver_employee_id,
#         decision,
#         leave_comment,
#         decision_datetime
#     )
#     VALUES (
#                p_leave_request_id,
#                p_approver_employee_id,
#                p_decision,
#             p_leave_comment,
#                NOW()
#            );
#
#     /* If approved, process balance and update */
#     IF p_decision = 'APPROVED' THEN
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
#         WHERE leave_request_id = p_leave_request_id;
#
#         INSERT INTO leave_ledger (
#             employee_id,
#             leave_type_id,
#             change_amount_days,
#             transaction_type,
#             reference_entity_type,
#             reference_entity_id,
#             transaction_datetime
#         )
#         VALUES (
#                    v_employee_id,
#                    v_leave_type_id,
#                    -v_days_requested,
#                    'USAGE',
#                    'LeaveRequest',
#                    p_leave_request_id,
#                    NOW()
#                );
#
#     END IF;
#
#     IF p_decision = 'REJECTED' THEN
#         UPDATE leave_request
#         SET request_status = 'REJECTED'
#         WHERE leave_request_id = p_leave_request_id;
#     END IF;
#
#     COMMIT;
#
# END $$
#
# DELIMITER ;
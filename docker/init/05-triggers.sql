USE shift_happens;

-- =========================================
-- TRIGGER: Validate and process leave approvals
-- =========================================
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

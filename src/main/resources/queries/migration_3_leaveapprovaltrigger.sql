
--Add additional vacation time for a test employee
INSERT INTO `leaveledger` (`employee_id`, `leave_type_id`, `change_amount_days`, `transaction_type`, `reference_entity_type`, `transaction_datetime`) VALUES ('96', '1', '2', 'ACCRUAL', '', '2026-02-24 11:00:00');
--set default datetime for new entries to simplify INSERTs
ALTER TABLE `leaverequest`
    CHANGE COLUMN `requested_datetime` `requested_datetime` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ;
--Add leave request
INSERT INTO `leaverequest` (leave_request_id, `employee_id`, `leave_type_id`, `start_date`, `end_date`, `request_status`, `reason`) VALUES (150,'96', '1', '2026-05-05', '2026-05-08', 'PENDING', 'Vacation');
/*
 TODO Instead of using 150 in leave_request_id, get last_insert_id
 */

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

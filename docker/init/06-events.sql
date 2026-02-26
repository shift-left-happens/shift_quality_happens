USE shift_happens;

-- =========================================
-- EVENT: Auto-complete past shifts (runs daily)
-- Marks shifts as COMPLETED if their end_datetime has passed
-- =========================================
DELIMITER $$

CREATE EVENT evt_auto_complete_shifts
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_TIMESTAMP
DO
BEGIN
    UPDATE shift
    SET shift_status = 'COMPLETED'
    WHERE shift_status = 'PLANNED'
      AND end_datetime < NOW();
END$$

DELIMITER ;

-- =========================================
-- EVENT: Auto-expire pending leave requests older than 30 days
-- =========================================
DELIMITER $$

CREATE EVENT evt_expire_old_leave_requests
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_TIMESTAMP
DO
BEGIN
    UPDATE leave_request
    SET request_status = 'EXPIRED'
    WHERE request_status = 'PENDING'
      AND requested_datetime < DATE_SUB(NOW(), INTERVAL 30 DAY);
END$$

DELIMITER ;

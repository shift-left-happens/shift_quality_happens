USE shift_happens;
SET NAMES utf8mb4;
-- Indsæt roller
# INSERT INTO user_role (`user_role_id`, `user_role_name`)
# VALUES (1, 'Administrator'),
#        (2, 'Employee'),
#        (3, 'Manager');

-- Sæt roller på eksisterende employees (hver 30. er manager)
UPDATE employee
SET user_role =
        CASE
            WHEN MOD(employee_id, 30) = 0 THEN 'Manager' /*Manager*/
            ELSE 'Employee' /*Employee*/
            END
WHERE employee_id < 101;
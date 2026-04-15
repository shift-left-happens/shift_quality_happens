USE shift_happens;
-- Indsæt roller
INSERT INTO user_role (`user_role_id`, `user_role_name`)
VALUES (1, 'Administrator'),
       (2, 'Employee'),
       (3, 'Manager');

-- Sæt roller på eksisterende employees (hver 30. er manager)
UPDATE employee
SET fk_user_role_id =
        CASE
            WHEN MOD(employee_id, 30) = 0 THEN 3 /*Manager*/
            ELSE 2 /*Employee*/
            END
WHERE employee_id < 101;
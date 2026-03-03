USE shift_happens;
ALTER TABLE employee
    ADD COLUMN `password` VARCHAR(255) NOT NULL AFTER `email`,
    CHANGE COLUMN `email` `email` VARCHAR(255) NOT NULL,
    ADD UNIQUE INDEX `email_UNIQUE` (`email` ASC) VISIBLE;
/*Update name as password is a reserved keyword*/
ALTER TABLE employee
    CHANGE COLUMN `password` `login_password` VARCHAR(255) NOT NULL;
/*Add some random passwords*/
UPDATE employee
SET login_password = SHA2(CONCAT(UUID(), RAND()), 256)
WHERE employee_id < 101;
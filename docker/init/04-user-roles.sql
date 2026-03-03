USE shift_happens;
/*Role based access, 1 role pr user currently*/
CREATE TABLE user_role
(
    `user_role_id`   INT         NOT NULL,
    `user_role_name` VARCHAR(45) NOT NULL,
    PRIMARY KEY (`user_role_id`),
    UNIQUE INDEX `user_role_name_UNIQUE` (`user_role_name` ASC) VISIBLE
);

/*Add 3 roles*/

INSERT INTO user_role (`user_role_id`, `user_role_name`)
VALUES (1, 'Administrator'),
       (2, 'Employee'),
       (3, 'Manager');

/*Add user_role_id to employee table*/
ALTER TABLE employee
    ADD COLUMN fk_user_role_id INT NULL AFTER `login_password`;
/*Set roles, every 30th is a manager*/
UPDATE employee
SET fk_user_role_id =
        CASE
            WHEN MOD(employee_id, 30) = 0 THEN 3 /*Manager*/
            ELSE 2 /*Employee*/
            END
WHERE employee_id < 101;
/*add not null constraint*/
ALTER TABLE employee
    MODIFY fk_user_role_id INT NOT NULL;
/* add fk constraint, don't delete a role with active users, if role_id gets updated, users cascade to keep roles.*/
ALTER TABLE employee
    ADD CONSTRAINT fk_employee_user_role
        FOREIGN KEY (fk_user_role_id)
            REFERENCES user_role (user_role_id)
            ON UPDATE CASCADE
            ON DELETE RESTRICT;
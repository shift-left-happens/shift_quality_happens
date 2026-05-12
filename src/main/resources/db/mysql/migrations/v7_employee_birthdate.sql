/* v7: Add birth_date to employee for BR-EM-01..05 validation. */
ALTER TABLE employee
    ADD COLUMN birth_date DATE NULL AFTER hire_date;

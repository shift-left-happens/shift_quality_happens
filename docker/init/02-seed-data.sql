USE shift_happens;
SET NAMES utf8mb4;
SET @entities_to_generate = 100;
START TRANSACTION;
-- =========================================
-- NUMBER GENERATOR (1 → 100)
-- =========================================
WITH RECURSIVE seq AS (SELECT 1 AS n
                       UNION ALL
                       SELECT n + 1
                       FROM seq
                       WHERE n < @entities_to_generate)
SELECT n
INTO @dummy
FROM seq
LIMIT 1;


-- Realistiske afdelingsnavne
INSERT INTO department (department_name, is_active)
VALUES ('Akutmodtagelse', 1),
       ('Intensiv', 1),
       ('Kirurgisk afdeling', 1),
       ('Medicinsk afdeling', 1),
       ('Børneafdeling', 1),
       ('Ortopædkirurgi', 1),
       ('Onkologi', 1),
       ('Radiologi', 1),
       ('Fysioterapi', 1),
       ('Psykiatri', 1),
       ('Ambulant behandling', 1),
       ('Laboratorium', 1),
       ('IT-support', 1),
       ('Administration', 1),
       ('Reception', 1),
       ('Rengøring', 1),
       ('Kantine', 1),
       ('Transport', 1),
       ('Lager', 1),
       ('Sikkerhed', 1);


-- Realistiske lokationer
INSERT INTO work_location (location_name, address_line_1, address_line_2, city, country, timezone, is_active)
VALUES ('Rigshospitalet', 'Blegdamsvej 9', NULL, 'København', 'Danmark', 'Europe/Copenhagen', 1),
       ('Aarhus Universitetshospital', 'Palle Juul-Jensens Blvd. 99', NULL, 'Aarhus', 'Danmark', 'Europe/Copenhagen', 1),
       ('Odense Universitetshospital', 'J.B. Winsløws Vej 4', NULL, 'Odense', 'Danmark', 'Europe/Copenhagen', 1),
       ('Aalborg Universitetshospital', 'Hobrovej 18-22', NULL, 'Aalborg', 'Danmark', 'Europe/Copenhagen', 1),
       ('Esbjerg Sygehus', 'Finsensgade 35', NULL, 'Esbjerg', 'Danmark', 'Europe/Copenhagen', 1),
       ('Randers Sygehus', 'Skovlyvej 15', NULL, 'Randers', 'Danmark', 'Europe/Copenhagen', 1),
       ('Herning Sygehus', 'Gl. Landevej 61', NULL, 'Herning', 'Danmark', 'Europe/Copenhagen', 1),
       ('Hillerød Hospital', 'Dyrehavevej 29', NULL, 'Hillerød', 'Danmark', 'Europe/Copenhagen', 1),
       ('Slagelse Sygehus', 'Ingemannsvej 18', NULL, 'Slagelse', 'Danmark', 'Europe/Copenhagen', 1),
       ('Vejle Sygehus', 'Beriderbakken 4', NULL, 'Vejle', 'Danmark', 'Europe/Copenhagen', 1);


-- Realistiske navne og emails (10 eksempler)
INSERT INTO employee (employee_number, first_name, last_name, email, phone_number,
                      hire_date, employment_status, primary_work_location_id)
VALUES
('EMP00001', 'Sofie', 'Jensen', 'sofie.jensen@hospital.dk', '+45 50123456', '2018-03-15', 'ACTIVE', 1),
('EMP00002', 'Mads', 'Nielsen', 'mads.nielsen@hospital.dk', '+45 50234567', '2019-07-01', 'ACTIVE', 2),
('EMP00003', 'Ida', 'Hansen', 'ida.hansen@hospital.dk', '+45 50345678', '2020-01-20', 'ACTIVE', 3),
('EMP00004', 'Frederik', 'Larsen', 'frederik.larsen@hospital.dk', '+45 50456789', '2017-11-10', 'INACTIVE', 4),
('EMP00005', 'Emma', 'Pedersen', 'emma.pedersen@hospital.dk', '+45 50567890', '2021-05-05', 'ACTIVE', 5),
('EMP00006', 'Noah', 'Andersen', 'noah.andersen@hospital.dk', '+45 50678901', '2016-09-23', 'ACTIVE', 6),
('EMP00007', 'Laura', 'Christensen', 'laura.christensen@hospital.dk', '+45 50789012', '2015-02-14', 'ACTIVE', 7),
('EMP00008', 'William', 'Møller', 'william.moller@hospital.dk', '+45 50890123', '2018-12-30', 'INACTIVE', 8),
('EMP00009', 'Josefine', 'Thomsen', 'josefine.thomsen@hospital.dk', '+45 50901234', '2022-04-18', 'ACTIVE', 9),
('EMP00010', 'Oliver', 'Poulsen', 'oliver.poulsen@hospital.dk', '+45 50111222', '2019-08-27', 'ACTIVE', 10),
('EMP00011', 'Katrine', 'Mortensen', 'katrine.mortensen@hospital.dk', '+45 50122334', '2017-06-12', 'ACTIVE', 1),
('EMP00012', 'Andreas', 'Olsen', 'andreas.olsen@hospital.dk', '+45 50233445', '2018-09-03', 'ACTIVE', 2),
('EMP00013', 'Camilla', 'Sørensen', 'camilla.sorensen@hospital.dk', '+45 50344556', '2020-02-17', 'ACTIVE', 3),
('EMP00014', 'Magnus', 'Christiansen', 'magnus.christiansen@hospital.dk', '+45 50455667', '2016-12-22', 'INACTIVE', 4),
('EMP00015', 'Julie', 'Madsen', 'julie.madsen@hospital.dk', '+45 50566778', '2021-03-11', 'ACTIVE', 5),
('EMP00016', 'Lucas', 'Knudsen', 'lucas.knudsen@hospital.dk', '+45 50677889', '2015-10-29', 'ACTIVE', 6),
('EMP00017', 'Maria', 'Kristensen', 'maria.kristensen@hospital.dk', '+45 50788990', '2019-01-05', 'ACTIVE', 7),
('EMP00018', 'Alexander', 'Jepsen', 'alexander.jepsen@hospital.dk', '+45 50899001', '2018-05-19', 'INACTIVE', 8),
('EMP00019', 'Caroline', 'Lund', 'caroline.lund@hospital.dk', '+45 50900112', '2022-07-23', 'ACTIVE', 9),
('EMP00020', 'Sebastian', 'Bach', 'sebastian.bach@hospital.dk', '+45 50111323', '2019-11-14', 'ACTIVE', 10),
('EMP00021', 'Mathilde', 'Holm', 'mathilde.holm@hospital.dk', '+45 50122435', '2017-08-08', 'ACTIVE', 1),
('EMP00022', 'Jonas', 'Vestergaard', 'jonas.vestergaard@hospital.dk', '+45 50233546', '2018-10-16', 'ACTIVE', 2),
('EMP00023', 'Maja', 'Lauridsen', 'maja.lauridsen@hospital.dk', '+45 50344657', '2020-04-02', 'ACTIVE', 3),
('EMP00024', 'Rasmus', 'Simonsen', 'rasmus.simonsen@hospital.dk', '+45 50455768', '2016-11-27', 'INACTIVE', 4),
('EMP00025', 'Nanna', 'Damgaard', 'nanna.damgaard@hospital.dk', '+45 50566879', '2021-06-18', 'ACTIVE', 5),
('EMP00026', 'Victor', 'Leth', 'victor.leth@hospital.dk', '+45 50677980', '2015-03-21', 'ACTIVE', 6),
('EMP00027', 'Louise', 'Bjerregaard', 'louise.bjerregaard@hospital.dk', '+45 50789091', '2019-02-13', 'ACTIVE', 7),
('EMP00028', 'Emil', 'Dahl', 'emil.dahl@hospital.dk', '+45 50890102', '2018-08-25', 'INACTIVE', 8),
('EMP00029', 'Signe', 'Friis', 'signe.friis@hospital.dk', '+45 50901213', '2022-10-30', 'ACTIVE', 9),
('EMP00030', 'Malthe', 'Enevoldsen', 'malthe.enevoldsen@hospital.dk', '+45 50112324', '2019-12-19', 'ACTIVE', 10),
('EMP00031', 'Anne', 'Gram', 'anne.gram@hospital.dk', '+45 50123435', '2017-04-04', 'ACTIVE', 1),
('EMP00032', 'Simon', 'Hedegaard', 'simon.hedegaard@hospital.dk', '+45 50234546', '2018-11-11', 'ACTIVE', 2),
('EMP00033', 'Helena', 'Iversen', 'helena.iversen@hospital.dk', '+45 50345657', '2020-06-06', 'ACTIVE', 3),
('EMP00034', 'Tobias', 'Jørgensen', 'tobias.jorgensen@hospital.dk', '+45 50456768', '2016-01-15', 'INACTIVE', 4),
('EMP00035', 'Cecilie', 'Krogh', 'cecilie.krogh@hospital.dk', '+45 50567879', '2021-08-09', 'ACTIVE', 5),
('EMP00036', 'Oscar', 'Lind', 'oscar.lind@hospital.dk', '+45 50678980', '2015-06-20', 'ACTIVE', 6),
('EMP00037', 'Nicoline', 'Munch', 'nicoline.munch@hospital.dk', '+45 50789091', '2019-03-03', 'ACTIVE', 7),
('EMP00038', 'Mikkel', 'Nyborg', 'mikkel.nyborg@hospital.dk', '+45 50890102', '2018-10-14', 'INACTIVE', 8),
('EMP00039', 'Amalie', 'Overgaard', 'amalie.overgaard@hospital.dk', '+45 50901213', '2022-12-01', 'ACTIVE', 9),
('EMP00040', 'Patrick', 'Petersen', 'patrick.petersen@hospital.dk', '+45 50112324', '2019-05-28', 'ACTIVE', 10),
('EMP00041', 'Sara', 'Qvist', 'sara.qvist@hospital.dk', '+45 50123435', '2017-09-17', 'ACTIVE', 1),
('EMP00042', 'Benjamin', 'Rasmussen', 'benjamin.rasmussen@hospital.dk', '+45 50234546', '2018-12-22', 'ACTIVE', 2),
('EMP00043', 'Line', 'Schmidt', 'line.schmidt@hospital.dk', '+45 50345657', '2020-08-13', 'ACTIVE', 3),
('EMP00044', 'Kasper', 'Thygesen', 'kasper.thygesen@hospital.dk', '+45 50456768', '2016-03-30', 'INACTIVE', 4),
('EMP00045', 'Victoria', 'Uldall', 'victoria.uldall@hospital.dk', '+45 50567879', '2021-10-21', 'ACTIVE', 5),
('EMP00046', 'Jeppe', 'Vang', 'jeppe.vang@hospital.dk', '+45 50678980', '2015-08-02', 'ACTIVE', 6),
('EMP00047', 'Mille', 'Westergaard', 'mille.westergaard@hospital.dk', '+45 50789091', '2019-04-15', 'ACTIVE', 7),
('EMP00048', 'Rikke', 'Østergaard', 'rikke.ostergaard@hospital.dk', '+45 50890102', '2018-11-27', 'INACTIVE', 8),
('EMP00049', 'Philip', 'Aagaard', 'philip.aagaard@hospital.dk', '+45 50901213', '2022-11-10', 'ACTIVE', 9),
('EMP00050', 'Sidsel', 'Bendtsen', 'sidsel.bendtsen@hospital.dk', '+45 50112324', '2019-07-06', 'ACTIVE', 10),
('EMP00051', 'Julie', 'Bonde', 'julie.bonde@hospital.dk', '+45 50123457', '2018-04-15', 'ACTIVE', 1),
('EMP00052', 'Martin', 'Brandt', 'martin.brandt@hospital.dk', '+45 50234568', '2019-08-01', 'ACTIVE', 2),
('EMP00053', 'Sanne', 'Bæk', 'sanne.baek@hospital.dk', '+45 50345679', '2020-02-21', 'ACTIVE', 3),
('EMP00054', 'Thomas', 'Berg', 'thomas.berg@hospital.dk', '+45 50456780', '2017-12-11', 'INACTIVE', 4),
('EMP00055', 'Pernille', 'Buhl', 'pernille.buhl@hospital.dk', '+45 50567891', '2021-06-15', 'ACTIVE', 5),
('EMP00056', 'Henrik', 'Bøgh', 'henrik.bogh@hospital.dk', '+45 50678902', '2016-10-23', 'ACTIVE', 6),
('EMP00057', 'Lone', 'Bøttger', 'lone.bottger@hospital.dk', '+45 50789013', '2015-03-14', 'ACTIVE', 7),
('EMP00058', 'Peter', 'Bækgaard', 'peter.baekgaard@hospital.dk', '+45 50890124', '2018-11-30', 'INACTIVE', 8),
('EMP00059', 'Mette', 'Bøje', 'mette.boje@hospital.dk', '+45 50901235', '2022-05-19', 'ACTIVE', 9),
('EMP00060', 'Jesper', 'Bølling', 'jesper.bolling@hospital.dk', '+45 50111223', '2019-09-27', 'ACTIVE', 10),
('EMP00061', 'Nina', 'Børsting', 'nina.borsting@hospital.dk', '+45 50122336', '2017-07-12', 'ACTIVE', 1),
('EMP00062', 'Søren', 'Bøttcher', 'soeren.boettcher@hospital.dk', '+45 50233447', '2018-10-05', 'ACTIVE', 2),
('EMP00063', 'Tina', 'Bøving', 'tina.boving@hospital.dk', '+45 50344558', '2020-03-17', 'ACTIVE', 3),
('EMP00064', 'Anders', 'Bøye', 'anders.boye@hospital.dk', '+45 50455669', '2016-11-22', 'INACTIVE', 4),
('EMP00065', 'Charlotte', 'Bøyesen', 'charlotte.boyesen@hospital.dk', '+45 50566780', '2021-04-11', 'ACTIVE', 5),
('EMP00066', 'Jan', 'Bøytler', 'jan.boeytler@hospital.dk', '+45 50677891', '2015-11-29', 'ACTIVE', 6),
('EMP00067', 'Birgitte', 'Bøgholm', 'birgitte.bogholm@hospital.dk', '+45 50788992', '2019-02-05', 'ACTIVE', 7),
('EMP00068', 'Kasper', 'Bøje', 'kasper.boje@hospital.dk', '+45 50899003', '2018-06-19', 'INACTIVE', 8),
('EMP00069', 'Lars', 'Bøllingtoft', 'lars.bollingtoft@hospital.dk', '+45 50900114', '2022-08-23', 'ACTIVE', 9),
('EMP00070', 'Mads', 'Bønløkke', 'mads.bonloekke@hospital.dk', '+45 50111325', '2019-10-14', 'ACTIVE', 10),
('EMP00071', 'Pia', 'Bønlykke', 'pia.bonlykke@hospital.dk', '+45 50122437', '2017-09-08', 'ACTIVE', 1),
('EMP00072', 'Rikke', 'Børgesen', 'rikke.boergesen@hospital.dk', '+45 50233548', '2018-11-16', 'ACTIVE', 2),
('EMP00073', 'Steffen', 'Børsting', 'steffen.borsting@hospital.dk', '+45 50344659', '2020-05-02', 'ACTIVE', 3),
('EMP00074', 'Tanja', 'Børup', 'tanja.borup@hospital.dk', '+45 50455770', '2016-10-27', 'INACTIVE', 4),
('EMP00075', 'Ulrik', 'Børve', 'ulrik.borve@hospital.dk', '+45 50566881', '2021-07-18', 'ACTIVE', 5),
('EMP00076', 'Vibeke', 'Børving', 'vibeke.borving@hospital.dk', '+45 50677982', '2015-04-21', 'ACTIVE', 6),
('EMP00077', 'Yvonne', 'Børzen', 'yvonne.borzen@hospital.dk', '+45 50789093', '2019-03-13', 'ACTIVE', 7),
('EMP00078', 'Zenia', 'Bøsgaard', 'zenia.bosgaard@hospital.dk', '+45 50890104', '2018-09-25', 'INACTIVE', 8),
('EMP00079', 'Aksel', 'Bøtker', 'aksel.botker@hospital.dk', '+45 50901215', '2022-11-30', 'ACTIVE', 9),
('EMP00080', 'Bente', 'Bøving', 'bente.boving@hospital.dk', '+45 50112326', '2019-11-19', 'ACTIVE', 10),
('EMP00081', 'Carsten', 'Bøye', 'carsten.boye@hospital.dk', '+45 50123437', '2017-05-04', 'ACTIVE', 1),
('EMP00082', 'Dorte', 'Bøytler', 'dorte.boeytler@hospital.dk', '+45 50234548', '2018-12-11', 'ACTIVE', 2),
('EMP00083', 'Erik', 'Bøgh', 'erik.bogh@hospital.dk', '+45 50345659', '2020-07-06', 'ACTIVE', 3),
('EMP00084', 'Fie', 'Bøttger', 'fie.bottger@hospital.dk', '+45 50456770', '2016-02-15', 'INACTIVE', 4),
('EMP00085', 'Gitte', 'Bøje', 'gitte.boje@hospital.dk', '+45 50567881', '2021-09-09', 'ACTIVE', 5),
('EMP00086', 'Hans', 'Bølling', 'hans.bolling@hospital.dk', '+45 50678982', '2015-07-20', 'ACTIVE', 6),
('EMP00087', 'Inge', 'Børsting', 'inge.borsting@hospital.dk', '+45 50789093', '2019-04-03', 'ACTIVE', 7),
('EMP00088', 'Jens', 'Bøttcher', 'jens.boettcher@hospital.dk', '+45 50890104', '2018-11-14', 'INACTIVE', 8),
('EMP00089', 'Karen', 'Bøving', 'karen.boving@hospital.dk', '+45 50901215', '2022-12-11', 'ACTIVE', 9),
('EMP00090', 'Lene', 'Bøye', 'lene.boye@hospital.dk', '+45 50112326', '2019-06-28', 'ACTIVE', 10),
('EMP00091', 'Morten', 'Bøyesen', 'morten.boyesen@hospital.dk', '+45 50123437', '2017-10-17', 'ACTIVE', 1),
('EMP00092', 'Niels', 'Bøytler', 'niels.boeytler@hospital.dk', '+45 50234548', '2018-12-22', 'ACTIVE', 2),
('EMP00093', 'Ole', 'Bøgholm', 'ole.bogholm@hospital.dk', '+45 50345659', '2020-08-13', 'ACTIVE', 3),
('EMP00094', 'Pia', 'Bøje', 'pia.boje@hospital.dk', '+45 50456770', '2016-03-30', 'INACTIVE', 4),
('EMP00095', 'Rikke', 'Bøllingtoft', 'rikke.bollingtoft@hospital.dk', '+45 50567881', '2021-10-21', 'ACTIVE', 5),
('EMP00096', 'Steen', 'Bønløkke', 'steen.bonloekke@hospital.dk', '+45 50678982', '2015-08-02', 'ACTIVE', 6),
('EMP00097', 'Tove', 'Bønlykke', 'tove.bonlykke@hospital.dk', '+45 50789093', '2019-04-15', 'ACTIVE', 7),
('EMP00098', 'Ulla', 'Børgesen', 'ulla.boergesen@hospital.dk', '+45 50890104', '2018-11-27', 'INACTIVE', 8),
('EMP00099', 'Vivi', 'Børsting', 'vivi.borsting@hospital.dk', '+45 50901215', '2022-11-10', 'ACTIVE', 9),
('EMP00100', 'Yvonne', 'Bendtsen', 'yvonne.bendtsen@hospital.dk', '+45 50112326', '2019-07-06', 'ACTIVE', 10);

-- =========================================
-- CONTRACTS (100)
-- =========================================
INSERT INTO employee_contract (employee_id, department_id, contract_type,
                               start_date, end_date, weekly_hours, salary_amount, is_active)
SELECT employee_id,
       1 + MOD(employee_id, 20), -- 20 departments
       ELT(1 + MOD(employee_id, 3), 'FULL_TIME', 'PART_TIME', 'TEMP'),
       DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND() * 2000) DAY),
       NULL,
       30 + MOD(employee_id, 10),
       35000 + RAND() * 40000,
       1
FROM employee;


-- Realistiske jobroller
INSERT INTO job_role (role_name, job_role_description, is_certification_required)
VALUES ('Sygeplejerske', 'Uddannet sygeplejerske', 1),
       ('Læge', 'Speciallæge', 1),
       ('Afdelingsleder', 'Leder af afdeling', 0),
       ('Social- og sundhedsassistent', 'Støtter patientpleje', 0),
       ('Receptionist', 'Reception og patientmodtagelse', 0),
       ('Sikkerhedsvagt', 'Sikkerhed på hospitalet', 1),
       ('Rengøringsassistent', 'Rengøring af faciliteter', 0),
       ('Bioanalytiker', 'Laboratoriearbejde', 1),
       ('Farmaceut', 'Medicinhåndtering', 1),
       ('Portør', 'Transport af patienter', 0),
       ('Lagerarbejder', 'Lager og logistik', 0),
       ('IT-supporter', 'Teknisk support', 0);

-- =========================================
-- EMPLOYEE JOB ROLES (~150 rows)
-- =========================================
INSERT INTO employee_job_role (employee_id, job_role_id, assigned_date, expiry_date, proficiency_level)
SELECT e.employee_id,
       1 + MOD(e.employee_id, 12), -- 12 job roles
       DATE_SUB(CURDATE(), INTERVAL MOD(e.employee_id, 600) DAY),
       NULL,
       ELT(1 + MOD(e.employee_id, 3), 'BEGINNER', 'INTERMEDIATE', 'ADVANCED')
FROM employee e;

-- =========================================
-- SHIFTS (100)
-- =========================================
INSERT INTO shift (department_id, work_location_id,
             shift_name, start_datetime, end_datetime, shift_status)
WITH RECURSIVE seq AS (SELECT 1 n
              UNION ALL
              SELECT n + 1
              FROM seq
              WHERE n < @entities_to_generate)
SELECT 1 + MOD(n, 20), -- 20 departments
    1 + MOD(n, 10), -- 10 work locations
    CONCAT('Shift ', n),
    DATE_ADD(CURDATE(), INTERVAL MOD(n, 14) DAY) + INTERVAL (MOD(n, 24)) HOUR,
    DATE_ADD(CURDATE(), INTERVAL MOD(n, 14) DAY) + INTERVAL (MOD(n, 24) + 8) HOUR,
    ELT(1 + MOD(n, 3), 'PLANNED', 'COMPLETED', 'CANCELLED')
FROM seq;

-- =========================================
-- SHIFT REQUIRED ROLES (~100)
-- =========================================
INSERT INTO shift_required_job_role (shift_id, job_role_id, required_employee_count)
SELECT shift_id,
       1 + MOD(shift_id, 12), -- 12 job roles
       1 + MOD(shift_id, 3)
FROM shift;

-- =========================================
-- SHIFT ASSIGNMENTS (100) UNIQUE PAIRS
-- =========================================
INSERT INTO shift_assignment (shift_id, employee_id,
                              assignment_status, assigned_datetime,
                              check_in_datetime, check_out_datetime)
SELECT s.shift_id,
       s.shift_id,
       ELT(1 + MOD(s.shift_id, 4), 'ASSIGNED', 'CONFIRMED', 'COMPLETED', 'NO_SHOW'),
       NOW() - INTERVAL MOD(s.shift_id, 10) DAY,
       NOW() - INTERVAL MOD(s.shift_id, 10) DAY + INTERVAL 1 HOUR,
       NOW() - INTERVAL MOD(s.shift_id, 10) DAY + INTERVAL 8 HOUR
FROM shift s;

-- =========================================
-- SHIFT APPROVALS (~70 mixed decisions)
-- =========================================
INSERT INTO shift_approval (shift_assignment_id, approver_employee_id,
                            decision, approval_comment, decision_datetime)
SELECT shift_assignment_id,
       1 + MOD(shift_assignment_id + 5, @entities_to_generate),
       ELT(1 + MOD(shift_assignment_id, 3), 'APPROVED', 'REJECTED', 'PENDING'),
       'Auto decision',
       NOW()
FROM shift_assignment
WHERE shift_assignment_id <= 70;

-- =========================================
-- SHIFT SWAPS (~50)
-- =========================================
INSERT INTO shift_swap (original_shift_assignment_id,
                        employee_from_id,
                        employee_to_id,
                        swap_status,
                        request_datetime,
                        reason)
SELECT sa.shift_assignment_id,
       sa.employee_id                                                               AS employee_from_id,
       1 + MOD(sa.employee_id + 5, @entities_to_generate)                           AS employee_to_id, -- simple offset to avoid same employee
       ELT(1 + MOD(sa.shift_assignment_id, 3), 'REQUESTED', 'APPROVED', 'DECLINED') AS swap_status,
       NOW() - INTERVAL MOD(sa.shift_assignment_id, 5) DAY                          AS request_datetime,
       'Personal reason'                                                            AS reason
FROM shift_assignment sa
WHERE sa.shift_assignment_id <= 50;

-- =========================================
-- SHIFT SWAP APPROVALS (~40)
-- =========================================
INSERT INTO shift_swap_approval (shift_swap_id, approver_employee_id,
                                 decision, shift_swap_comment, decision_datetime)
SELECT shift_swap_id,
       1 + MOD(shift_swap_id + 10, @entities_to_generate),
       ELT(1 + MOD(shift_swap_id, 3), 'APPROVED', 'REJECTED', 'PENDING'),
       'Swap review',
       NOW()
FROM shift_swap
WHERE shift_swap_id <= 40;

-- =========================================
-- LEAVE TYPES (REALISTIC SMALL SET)
-- =========================================
INSERT INTO leave_type (leave_type_name, leave_type_description,
                        requires_approval, is_paid_leave)
VALUES ('Vacation', 'Paid annual leave', 1, 1),
       ('Sick Leave', 'Medical leave', 1, 1),
       ('Maternity', 'Maternity leave', 1, 1),
       ('Paternity', 'Paternity leave', 1, 1),
       ('Unpaid Leave', 'Unpaid time off', 1, 0),
       ('Bereavement', 'Family death leave', 1, 1),
       ('Study Leave', 'Education leave', 1, 0),
       ('Emergency Leave', 'Emergency personal leave', 1, 1);

-- =========================================
-- LEAVE REQUESTS (100)
-- =========================================
INSERT INTO leave_request (employee_id, leave_type_id,
                           start_date, end_date,
                           request_status, reason, requested_datetime)
SELECT employee_id,
       1 + MOD(employee_id, 8),
       CURDATE() - INTERVAL MOD(employee_id, 30) DAY,
       CURDATE() + INTERVAL MOD(employee_id, 10) DAY,
       ELT(1 + MOD(employee_id, 3), 'PENDING', 'APPROVED', 'REJECTED'),
       'Personal leave',
       NOW() - INTERVAL MOD(employee_id, 5) DAY
FROM employee;

-- =========================================
-- LEAVE APPROVALS (~70)
-- =========================================
INSERT INTO leave_approval (leave_request_id,
                            approver_employee_id,
                            decision,
                            leave_comment,
                            decision_datetime)
SELECT leave_request_id,
       1 + MOD(leave_request_id + 7, @entities_to_generate),
       ELT(1 + MOD(leave_request_id, 3), 'APPROVED', 'REJECTED', 'PENDING'),
       'Reviewed',
       NOW()
FROM leave_request
WHERE leave_request_id <= 70;

-- =========================================
-- LEAVE LEDGER (100)
-- =========================================
INSERT INTO leave_ledger (employee_id, leave_type_id,
                          change_amount_days,
                          transaction_type,
                          reference_entity_type,
                          reference_entity_id,
                          transaction_datetime)
SELECT employee_id,
       1 + MOD(employee_id, 8),
       (1 + MOD(employee_id, 5)),
       ELT(1 + MOD(employee_id, 2), 'ACCRUAL', 'USAGE'),
       'LeaveRequest',
       employee_id,
       NOW() - INTERVAL MOD(employee_id, 30) DAY
FROM employee;

-- =========================================
-- AUDIT LOG (100)
-- =========================================
INSERT INTO audit_log (entity_type, entity_id, action_type,
                       db_user,
                       action_datetime,
                       old_value_snapshot,
                       new_value_snapshot)
SELECT 'Employee',
       employee_id,
       ELT(1 + MOD(employee_id, 3), 'CREATE', 'UPDATE', 'DELETE'),
       USER(),
       NOW(),
       '{}',
       '{}'
FROM employee;
COMMIT;
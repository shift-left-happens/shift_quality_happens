# Unit Test Implementation Plan

Source of truth: **`Software Quality exam - Shift Happens (1).docx`** (attached to the conversation). Only the sections present in that document are in scope. Older `Black_Box_Tests_Extended.md` is ignored.

## What's in the docx (verbatim section list)

| # | Section | Technique | Target |
|---|---|---|---|
| 1 | Employes blackbox analyse | BVA tables for email length, password length, first/last name length | Employee fields |
| 2 | First Name & last name | Decision table (Empty? / Letters only?) — 5 cases | EmployeeService |
| 3 | Password black box tests | Decision table (Length≥8 / Upper / Lower / Number) — 5 cases | EmployeeService |
| 4 | Email decision table | R0 length, R1 one `@`, R2 local part, R3 domain, R4 unique — 5 cases | EmployeeService |
| 5 | Age & Birth Date | 3-point BVA: ages 14,15,16,17,99,100,101,102 ; years 1924,1925,1926,1927,2009,2010,2011 | EmployeeService (needs new `birthDate` field) |
| 6 | Shift Duration & Timing | Decision table: start<end / rest≥11h / matches job role — rules 1,2,4,5 | ShiftService + ShiftAssignmentService |
| 7 | Weekly Working Hours | 2-point BVA: 0.0, 0.1, 36.9, 37.0, 37.1, 38.0 | ShiftAssignmentService |
| 8 | Consecutive Work Days | 3-point BVA: days 5,6,7,8 ; nights 3,4,5 ; night-shift def: ≥3h in 22:00–05:00 | ShiftAssignmentService |
| 9 | Shift Swap Approvals | Decision table — cases 1,2,3,5,6 | ShiftSwapApprovalService |
| 10 | Deletion Constraints | Decision table — cases 1,2,3 (active shifts / linked to active employees) | EmployeeService, JobRoleService |
| 11 | ISO 8601 Dates | EP: valid `2024-05-12T00:00`, invalid format `12-05-2024T00:00`, invalid date `2024-02-29T00:00` | Employee birthDate, Shift datetimes |

Not in docx (excluded): Contract Validity, Shift Status Transitions as standalone section, Input Validation Names as section 13 (covered by §2).

---

## Scoping note

The blackbox cases test validation rules that **mostly don't exist** in the current code. Three options:

1. **Full** — add the missing validation in services, then tests exercise it. Largest scope. Recommended.
2. **Phase 0–1** — birthDate + Employee field validation only (§1–§5, §11). Smaller.
3. **Tests only** — many fail because the rule isn't enforced. Useful as a gap report; not a passing suite.

This plan assumes option 1. Switch by removing later phases if you want option 2.

---

## Phase 0 — Add `birthDate` to Employee

Needed before §5 tests can be written.

- `Employee.java` — add `@Column(name="birth_date") private LocalDate birthDate;`
- `EmployeeService.patch()` — propagate the field
- DB:
  - `docker/init/01-schema.sql` — add `birth_date DATE` to `employee` CREATE
  - New `src/main/resources/db/mysql/migrations/v7_employee_birthdate.sql` — `ALTER TABLE employee ADD COLUMN birth_date DATE;`

---

## Phase 1 — Employee validation (§1–§5, §11)

### Production changes — add `EmployeeValidator` invoked from `EmployeeService.save()` / `patch()`

- **Names (§2):** letters only (Unicode letters incl. `Ø`); length per §1 = 1–100. Decision table case 5 (`"Jensen Jens"`) → see open question on last-name spacing.
- **Password (§3 + §1 BVA):** length 8–64 from docx §"3-Point BVA - Password Length" (despite §1's old 8–255 table — the newer §"Password Strength" section narrows it to 8–64). Must contain ≥1 lower, ≥1 upper, ≥1 digit. Validate *before* hashing.
- **Email (§4 + §1 BVA):** length 5–320, exactly one `@`, valid local (non-empty no spaces), valid domain (≥1 letter + `.` + TLD ≥2 letters), uniqueness via new `EmployeeRepository.existsByEmail(String)`.
- **Birth date (§5):** non-null, not in the future, age ∈ [16, 100] vs `LocalDate.now(clock)`.
- **ISO 8601 (§11):** enforced implicitly by `LocalDate.parse` rejecting `12-05-2024` and `2024-02-29`. Tests assert the parse-then-validate chain produces the right error.

### Tests — `src/test/java/dk/ek/shift_happens/employee/EmployeeServiceTest.java` (NEW)

JUnit 5 + Mockito, `@Nested` per concern, naming `should_xxx_when_yyy`.

| Nested class | Source rows from docx |
|---|---|
| `NameValidation` | §2 decision table cases 1–5; §1 BVA: lengths 0, 1, 2, 99, 100, 101 |
| `PasswordComposition` | §3 cases 1–5 (`pass`, `password`, `PASSWORD`, `Password`, `Passw0rd`) |
| `PasswordLengthBVA` | §"3-Point BVA – Password Length": 7, 8, 9, 63, 64, 65 |
| `EmailFormat` | §4 cases 1–5 incl. R4 uniqueness |
| `EmailLengthBVA` | §1 BVA email row: 4, 5, 6, 319, 320, 321 |
| `AgeBVA` | §5: ages 14, 15, 16, 17, 99, 100, 101, 102 — birthDate computed from frozen clock |
| `BirthYearBVA` | §5: 1924, 1925, 1926, 1927, 2009, 2010, 2011 (with fixed today 2026-05-12) |
| `ISO8601Date` | §11: `2024-05-12T00:00` valid; `12-05-2024T00:00` invalid format; `2024-02-29T00:00` invalid (2024 was leap so use `2024-02-30T00:00`) |

**Date stability:** inject `Clock` into `EmployeeService` (or pass `today` to validator). Tests use `Clock.fixed(Instant.parse("2026-05-12T12:00:00Z"), ZoneOffset.UTC)`.

**Mocks:** `EmployeeRepository`, `PasswordEncoder`. Assert encoder is called only after validation succeeds.

---

## Phase 2 — Shift timing rules (§6 rules 1 & 2)

Decision-table rule 1 is the happy path; rule 2 (`start ≥ end`) is already enforced. Add:

### Production changes
None required for rule 2 — `ShiftService.validate()` already throws when `!endDatetime.isAfter(startDatetime)`.

### Tests — fill `src/test/java/dk/ek/shift_happens/shift/ShiftServiceTest.java`

| Nested class | Source rows |
|---|---|
| `TimingValidation` | §6 rule 1 (valid); rule 2 (`start == end` and `start > end` both rejected) |

(Other rules 4, 5 belong to assignment — Phase 3.)

---

## Phase 3 — Shift assignment rules (§6 rules 4 & 5, §7, §8)

### Production changes — `ShiftAssignmentService.validate()`

- **Rule 4 (§6) Rest period ≥ 11h** (BR-WT-02): no other non-cancelled assignment for this employee ends within 11h of this start, nor starts within 11h after this end.
- **Rule 5 (§6) Job-role match** (FR-JR-05): employee has at least one `employee_job_role` matching some `shift_required_job_role` row for the shift.
- **§7 Weekly hours ≤ 37** (BR-WT-01): sum of this employee's non-cancelled assignment durations in the ISO week of the shift must not exceed 37h.
- **§8 Consecutive days ≤ 6** (BR-WT-04).
- **§8 Consecutive night shifts ≤ 3** (BR-WT-05), where *night shift* = ≥3h of work within the 22:00–05:00 window (docx §8 night definition).

### Tests — fill `src/test/java/dk/ek/shift_happens/shiftassignment/ShiftAssignmentServiceTest.java`

| Nested class | Source rows |
|---|---|
| `RestPeriodBVA` | §6 rule 4 + boundaries: 10h59m (invalid), 11h00m (valid), 11h01m (valid) gap |
| `JobRoleMatch` | §6 rule 5 (no matching role → invalid; one match → valid) |
| `WeeklyHoursBVA` | §7: 0.0, 0.1, 36.9, 37.0 (valid); 37.1, 38.0 (invalid) |
| `ConsecutiveDaysBVA` | §8: 5, 6 (valid); 7, 8 (invalid) |
| `NightShiftDefinition` | §8: 22:00–01:00 (3h night, counts); 21:00–00:00 (2h night, doesn't count); 03:00–06:00 (2h within 22–05 window, doesn't count); 22:00–01:01 (3h01m, counts) |
| `ConsecutiveNightShiftsBVA` | §8: 2, 3 (valid); 4, 5 (invalid) |

---

## Phase 4 — Shift swap approval (§9)

(docx labels this "5. Shift Swap Approvals" but indexed as §9 in the table above — same content.)

### Production changes — `ShiftSwapApprovalService`

Add `approve()` checks matching docx cases:
1. Approver role ∈ {Administrator, Manager} (case 2 denies otherwise)
2. Approver ≠ requester (case 3)
3. Swap status = Pending (case 5 — Cancelled / Declined denied)
4. Underlying shift not Cancelled (case 6)
5. Result causes no overlap (case 1 condition "Causes overlap = F")

### Tests — `src/test/java/dk/ek/shift_happens/shiftswapapproval/ShiftSwapApprovalServiceTest.java` (NEW)

| Test | docx case |
|---|---|
| `approves_happy_path` | 1 |
| `denies_when_employee_role` | 2 |
| `denies_self_approval` | 3 |
| `denies_when_status_cancelled_or_declined` | 5 |
| `denies_when_shift_cancelled` | 6 |
| `denies_when_would_overlap` | 1 condition inverted |

Existing empty `ShiftSwapServiceTest.java` stays untouched (it tests the swap-request service, not approval).

---

## Phase 5 — Deletion constraints (§10)

### Production changes

- `EmployeeService.delete()` — block if employee has any future non-cancelled `shift_assignment`.
- `JobRoleService.delete()` — block if any `employee_job_role` references this role (case 2) or any `shift_required_job_role` does (case 1).

### Tests

Fill `src/test/java/dk/ek/shift_happens/jobrole/JobRoleServiceTest.java`:

| Test | docx case |
|---|---|
| `blocks_when_active_shift_links_exist` | 1 |
| `blocks_when_linked_to_active_employees` | 2 |
| `deletes_when_no_dependencies` | 3 |

Add `DeletionConstraints` nested class to `EmployeeServiceTest`:

| Test | docx case (interpreted for employee) |
|---|---|
| `blocks_when_future_assignments_exist` | 1 |
| `deletes_when_no_dependencies` | 3 |

---

## Test infrastructure

- JUnit 5 + Mockito (both already pulled in by `spring-boot-starter-test`).
- No `@SpringBootTest` — plain unit tests, mocked repositories, fast.
- Run with `./mvnw test`. Format with `./mvnw spotless:apply` before any commit.
- JaCoCo report at `target/site/jacoco/index.html` after `./mvnw test`.

---

## Execution order

1. Phase 0 — birthDate column + entity field + migration.
2. Phase 1 — Employee validation + `EmployeeServiceTest`.
3. Phase 2 — Shift timing tests (small).
4. Phase 3 — Shift assignment validation + tests (largest phase).
5. Phase 4 — Swap approval validation + tests.
6. Phase 5 — Deletion guards + tests.
7. `./mvnw test`, fix, `./mvnw spotless:apply`.
8. Wait for explicit "commit" / "push" instruction.

---

## Open questions

1. **Scope:** Full (option 1), Phase 0–1 only (option 2), or tests-only (option 3)?
2. **Last-name spacing:** docx §2 case 5 marks `"Jensen Jens"` Valid. The section heading says "1 word, no whitespace". Which wins for last name validation?
3. **Birth-date migration:** edit `docker/init/01-schema.sql` AND add `v7_employee_birthdate.sql`, or migration only?
4. **Email uniqueness (R4) source:** add `existsByEmail` to `EmployeeRepository` and check in the service, OK?

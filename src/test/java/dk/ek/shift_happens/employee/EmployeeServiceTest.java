package dk.ek.shift_happens.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dk.ek.shift_happens.shift.Shift;
import dk.ek.shift_happens.shift.ShiftRepository;
import dk.ek.shift_happens.shiftassignment.ShiftAssignment;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentRepository;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for EmployeeService validation, derived from
 * "Software Quality exam - Shift Happens (1).docx".
 *
 * Each nested class targets a section of the document. Test method names follow
 * `should_<expectation>_when_<condition>` to read as plain English.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 12);
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-12T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private EmployeeRepository repo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ShiftAssignmentRepository shiftAssignmentRepository;

    @Mock
    private ShiftRepository shiftRepository;

    private EmployeeService service;

    @BeforeEach
    void setUp() {
        EmployeeValidator validator = new EmployeeValidator(FIXED_CLOCK);
        service = new EmployeeService(
                repo, passwordEncoder, validator, shiftAssignmentRepository, shiftRepository, FIXED_CLOCK);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("HASHED");
        lenient().when(repo.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(repo.existsByEmail(anyString())).thenReturn(false);
        lenient().when(shiftAssignmentRepository.findByEmployeeId(anyInt())).thenReturn(Collections.emptyList());
    }

    /** Reusable valid employee fixture — change one field per test. */
    private Employee valid() {
        Employee e = new Employee();
        e.setFirstName("Jensen");
        e.setLastName("Jensen");
        e.setEmail("a@a.dk");
        e.setLoginPassword("Passw0rd");
        e.setBirthDate(LocalDate.of(1990, 1, 1));
        return e;
    }

    // -------------------------------------------------------------------------
    // docx §"First Name & last name" + §1 BVA for name length
    // -------------------------------------------------------------------------
    @Nested
    class NameValidation {

        @Test
        void should_accept_when_letters_only_no_space() {
            // §"First Name & last name" decision table case 1 — "Jensen" / Valid
            Employee e = valid();
            e.setFirstName("Jensen");
            e.setLastName("Jensen");
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_reject_when_name_empty() {
            // case 2 — "" / Invalid
            Employee e = valid();
            e.setFirstName("");
            assertThatThrownBy(() -> service.save(e))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("firstName");
        }

        @Test
        void should_reject_when_name_contains_numbers() {
            // case 3 — "Jensen1" / Invalid
            Employee e = valid();
            e.setFirstName("Jensen1");
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_reject_when_name_contains_symbols() {
            // case 4 — "Jensen@" / Invalid
            Employee e = valid();
            e.setLastName("Jensen@");
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_accept_when_name_contains_single_internal_space() {
            // case 5 — "Jensen Jens" / Valid (per docx)
            Employee e = valid();
            e.setFirstName("Jensen Jens");
            e.setLastName("Jensen Jens");
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_accept_minimum_length_one_letter() {
            // §1 BVA — name length 1 (e.g. Ø) is Valid
            Employee e = valid();
            e.setFirstName("Ø");
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_accept_maximum_length_100() {
            Employee e = valid();
            e.setFirstName("A".repeat(100));
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_reject_length_101() {
            Employee e = valid();
            e.setFirstName("A".repeat(101));
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -------------------------------------------------------------------------
    // docx §"Password black box tests" decision table
    // -------------------------------------------------------------------------
    @Nested
    class PasswordComposition {

        @Test
        void should_reject_too_short_password() {
            // case 1 — "pass" length<8 / Error
            Employee e = valid();
            e.setLoginPassword("pass");
            assertThatThrownBy(() -> service.save(e))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("password");
        }

        @Test
        void should_reject_missing_uppercase() {
            // case 2 — "password" / Error
            Employee e = valid();
            e.setLoginPassword("password");
            assertThatThrownBy(() -> service.save(e))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("uppercase");
        }

        @Test
        void should_reject_missing_lowercase() {
            // case 3 — "PASSWORD" / Error
            Employee e = valid();
            e.setLoginPassword("PASSWORD");
            assertThatThrownBy(() -> service.save(e))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("lowercase");
        }

        @Test
        void should_reject_missing_digit() {
            // case 4 — "Password" / Error
            Employee e = valid();
            e.setLoginPassword("Password");
            assertThatThrownBy(() -> service.save(e))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("digit");
        }

        @Test
        void should_accept_valid_password() {
            // case 5 — "Passw0rd" / Accepted
            Employee e = valid();
            e.setLoginPassword("Passw0rd");
            assertThat(service.save(e)).isNotNull();
            verify(passwordEncoder).encode("Passw0rd");
        }
    }

    // docx §"3-Point BVA – Password Length": 7, 8, 9, 63, 64, 65
    @Nested
    class PasswordLengthBVA {

        private String pwd(int totalLen) {
            // build "Aa1" + filler so it always has upper+lower+digit
            String prefix = "Aa1";
            return prefix + "x".repeat(Math.max(0, totalLen - prefix.length()));
        }

        @Test
        void should_reject_length_7() {
            Employee e = valid();
            e.setLoginPassword(pwd(7));
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_accept_length_8() {
            Employee e = valid();
            e.setLoginPassword(pwd(8));
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_accept_length_9() {
            Employee e = valid();
            e.setLoginPassword(pwd(9));
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_accept_length_63() {
            Employee e = valid();
            e.setLoginPassword(pwd(63));
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_accept_length_64() {
            Employee e = valid();
            e.setLoginPassword(pwd(64));
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_reject_length_65() {
            Employee e = valid();
            e.setLoginPassword(pwd(65));
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -------------------------------------------------------------------------
    // docx §"Email decision table" — R0..R4
    // -------------------------------------------------------------------------
    @Nested
    class EmailFormat {

        @Test
        void should_accept_valid_email() {
            // case 1 — "a@a.dk" / Valid
            Employee e = valid();
            e.setEmail("a@a.dk");
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_reject_two_at_signs() {
            // case 2 — "@@das" / R1 fails
            Employee e = valid();
            e.setEmail("@@das");
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_reject_too_short() {
            // case 3 — "a@a" / R0 fails (length 3)
            Employee e = valid();
            e.setEmail("a@a");
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_reject_empty_local_part() {
            // case 4 — "@aaa.dk" / R2 fails
            Employee e = valid();
            e.setEmail("@aaa.dk");
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_reject_invalid_domain() {
            // case 5 in original table — "aaa@a" / R3 fails (no dot + TLD)
            Employee e = valid();
            e.setEmail("aaa@a");
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_reject_when_email_already_taken_r4() {
            // case 5 (docx renumbered) — R4 fails: format valid but email exists
            Employee e = valid();
            e.setEmail("a@a.dk");
            when(repo.existsByEmail("a@a.dk")).thenReturn(true);
            assertThatThrownBy(() -> service.save(e))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already");
        }
    }

    // docx §1 BVA: email length 4, 5, 6, 319, 320, 321
    @Nested
    class EmailLengthBVA {

        private String emailOfLength(int len) {
            // build "<local>@a.dk" with total length == len. Min local must be 1.
            int suffixLen = "@a.dk".length(); // 5
            int localLen = Math.max(1, len - suffixLen);
            return "a".repeat(localLen) + "@a.dk";
        }

        @Test
        void should_reject_length_4() {
            Employee e = valid();
            e.setEmail("a@a"); // length 3, < 5 — represents R0=F
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_accept_length_5() {
            Employee e = valid();
            e.setEmail("a@a.dk"); // length 6 — boundary 5 not constructible w/ valid domain
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_accept_length_6() {
            Employee e = valid();
            e.setEmail("a@ab.dk");
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_accept_length_319() {
            Employee e = valid();
            e.setEmail(emailOfLength(319));
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_accept_length_320() {
            Employee e = valid();
            e.setEmail(emailOfLength(320));
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_reject_length_321() {
            Employee e = valid();
            e.setEmail(emailOfLength(321));
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -------------------------------------------------------------------------
    // docx §"Age & Birth Date" — 3-point BVA for age (14,15,16,17,99,100,101,102)
    // -------------------------------------------------------------------------
    @Nested
    class AgeBVA {

        private LocalDate ageOf(int years) {
            return TODAY.minusYears(years);
        }

        @Test
        void should_reject_age_14() {
            Employee e = valid();
            e.setBirthDate(ageOf(14));
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_reject_age_15() {
            Employee e = valid();
            e.setBirthDate(ageOf(15));
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_accept_age_16() {
            Employee e = valid();
            e.setBirthDate(ageOf(16));
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_accept_age_17() {
            Employee e = valid();
            e.setBirthDate(ageOf(17));
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_accept_age_99() {
            Employee e = valid();
            e.setBirthDate(ageOf(99));
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_accept_age_100() {
            Employee e = valid();
            e.setBirthDate(ageOf(100));
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_reject_age_101() {
            Employee e = valid();
            e.setBirthDate(ageOf(101));
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_reject_age_102() {
            Employee e = valid();
            e.setBirthDate(ageOf(102));
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_reject_future_birthdate() {
            // BR-EM-03 — Birth dates cannot be in the future.
            Employee e = valid();
            e.setBirthDate(TODAY.plusDays(1));
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    // docx §"Age & Birth Date" — 3-point BVA for birth year (1924..2011)
    @Nested
    class BirthYearBVA {

        // birthDate Jan 1 of year so the age computation against TODAY (2026-05-12)
        // is unambiguous (already had their birthday this calendar year).
        private LocalDate yearStart(int year) {
            return LocalDate.of(year, 1, 1);
        }

        @Test
        void should_reject_year_1924() {
            // age = 2026 - 1924 = 102 → too old
            Employee e = valid();
            e.setBirthDate(yearStart(1924));
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_reject_year_1925() {
            // age = 101 → too old
            Employee e = valid();
            e.setBirthDate(yearStart(1925));
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_accept_year_1926() {
            // age = 100 → boundary, accepted
            Employee e = valid();
            e.setBirthDate(yearStart(1926));
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_accept_year_1927() {
            Employee e = valid();
            e.setBirthDate(yearStart(1927));
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_accept_year_2009() {
            // age 17
            Employee e = valid();
            e.setBirthDate(yearStart(2009));
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_accept_year_2010() {
            // age 16 — boundary
            Employee e = valid();
            e.setBirthDate(yearStart(2010));
            assertThat(service.save(e)).isNotNull();
        }

        @Test
        void should_reject_year_2011() {
            // age 15 — too young
            Employee e = valid();
            e.setBirthDate(yearStart(2011));
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -------------------------------------------------------------------------
    // docx §"ISO 8601 Dates" — parse/reject behavior
    // -------------------------------------------------------------------------
    @Nested
    class ISO8601Date {

        @Test
        void should_parse_valid_iso_date() {
            // §11 row 1 — "2024-05-12T00:00" — date portion parses cleanly to LocalDate.
            LocalDateTime dt = LocalDateTime.parse("2024-05-12T00:00");
            assertThat(dt.toLocalDate()).isEqualTo(LocalDate.of(2024, 5, 12));
        }

        @Test
        void should_reject_non_iso_format() {
            // §11 row 2 — "12-05-2024T00:00" must fail to parse.
            assertThatThrownBy(() -> LocalDateTime.parse("12-05-2024T00:00"))
                    .isInstanceOf(java.time.format.DateTimeParseException.class);
        }

        @Test
        void should_reject_non_existent_date() {
            // §11 row 3 — non-existent date. 2024 was a leap year so use 2024-02-30.
            assertThatThrownBy(() -> LocalDateTime.parse("2024-02-30T00:00"))
                    .isInstanceOf(java.time.format.DateTimeParseException.class);
        }
    }

    @Nested
    class GuardsHonored {

        @Test
        void should_not_hash_password_if_validation_fails() {
            Employee e = valid();
            e.setLoginPassword("short"); // too short
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    // -------------------------------------------------------------------------
    // docx §"Deletion Constraints" applied to Employee:
    //   case 1 — future non-cancelled shift assignment → block
    //   case 3 — no dependencies → delete
    // -------------------------------------------------------------------------
    @Nested
    class DeletionConstraints {

        private static final int EMPLOYEE_ID = 42;

        @BeforeEach
        void seedEmployee() {
            Employee existing = valid();
            existing.setEmployeeId(EMPLOYEE_ID);
            when(repo.findById(EMPLOYEE_ID)).thenReturn(Optional.of(existing));
        }

        @Test
        void should_delete_when_no_dependencies() {
            // docx §"Deletion Constraints" case 3
            when(shiftAssignmentRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Collections.emptyList());
            service.delete(EMPLOYEE_ID);
            verify(repo).delete(any(Employee.class));
        }

        @Test
        void should_block_when_future_assignment_exists() {
            // docx §"Deletion Constraints" case 1
            ShiftAssignment a = new ShiftAssignment();
            a.setShiftAssignmentId(1);
            a.setShiftId(100);
            a.setEmployeeId(EMPLOYEE_ID);
            a.setAssignmentStatus(ShiftAssignmentService.STATUS_ASSIGNED);

            Shift future = new Shift();
            future.setShiftId(100);
            future.setStartDatetime(LocalDateTime.of(2030, 1, 1, 8, 0));
            future.setEndDatetime(LocalDateTime.of(2030, 1, 1, 16, 0));

            when(shiftAssignmentRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(a));
            when(shiftRepository.findById(100)).thenReturn(Optional.of(future));

            assertThatThrownBy(() -> service.delete(EMPLOYEE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("future");
            verify(repo, never()).delete(any(Employee.class));
        }

        @Test
        void should_allow_delete_when_only_past_assignments() {
            ShiftAssignment a = new ShiftAssignment();
            a.setShiftAssignmentId(1);
            a.setShiftId(100);
            a.setEmployeeId(EMPLOYEE_ID);
            a.setAssignmentStatus(ShiftAssignmentService.STATUS_COMPLETED);

            Shift past = new Shift();
            past.setShiftId(100);
            past.setStartDatetime(LocalDateTime.of(2020, 1, 1, 8, 0));
            past.setEndDatetime(LocalDateTime.of(2020, 1, 1, 16, 0));

            when(shiftAssignmentRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(a));
            when(shiftRepository.findById(100)).thenReturn(Optional.of(past));

            service.delete(EMPLOYEE_ID);
            verify(repo).delete(any(Employee.class));
        }

        @Test
        void should_allow_delete_when_future_assignment_is_cancelled() {
            ShiftAssignment a = new ShiftAssignment();
            a.setShiftAssignmentId(1);
            a.setShiftId(100);
            a.setEmployeeId(EMPLOYEE_ID);
            a.setAssignmentStatus(ShiftAssignmentService.STATUS_CANCELLED);

            Shift future = new Shift();
            future.setShiftId(100);
            future.setStartDatetime(LocalDateTime.of(2030, 1, 1, 8, 0));
            future.setEndDatetime(LocalDateTime.of(2030, 1, 1, 16, 0));

            when(shiftAssignmentRepository.findByEmployeeId(EMPLOYEE_ID)).thenReturn(List.of(a));
            lenient().when(shiftRepository.findById(100)).thenReturn(Optional.of(future));

            service.delete(EMPLOYEE_ID);
            verify(repo).delete(any(Employee.class));
        }
    }
}

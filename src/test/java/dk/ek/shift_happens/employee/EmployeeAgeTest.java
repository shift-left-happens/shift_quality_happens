package dk.ek.shift_happens.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import dk.ek.shift_happens.shift.ShiftRepository;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for Employee age and birthdate validation, derived from
 * §"Age & Birth Date" and §"ISO 8601 Dates" in the .md file.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeAgeTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 12);
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-12T12:00:00Z"), ZoneOffset.UTC);

    @Mock private EmployeeRepository repo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ShiftAssignmentRepository shiftAssignmentRepository;
    @Mock private ShiftRepository shiftRepository;

    private EmployeeService service;

    @BeforeEach
    void setUp() {
        EmployeeValidator validator = new EmployeeValidator(FIXED_CLOCK);
        service = new EmployeeService(repo, passwordEncoder, validator, shiftAssignmentRepository, shiftRepository, FIXED_CLOCK);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("HASHED");
        lenient().when(repo.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Employee valid() {
        Employee e = new Employee();
        e.setFirstName("Jensen");
        e.setLastName("Jensen");
        e.setEmail("a@a.dk");
        e.setLoginPassword("Passw0rd");
        e.setHireDate(LocalDate.of(2024, 1, 1));
        e.setEmploymentStatus("ACTIVE");
        return e;
    }

    @ParameterizedTest(name = "Should validate age: {0} years old -> {1}")
    @CsvSource({
        "14, false",
        "15, false",
        "16, true",
        "17, true",
        "99, true",
        "100, true",
        "101, false",
        "102, false"
    })
    void should_validate_age_boundaries(int age, boolean expectedValid) {
        // §"Age & Birth Date" 3-point BVA for age
        Employee e = valid();
        e.setBirthDate(TODAY.minusYears(age));
        if (expectedValid) {
            assertThat(service.save(e)).isNotNull();
        } else {
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void should_reject_future_birthdate() {
        // BR-EM-03 — Birth dates cannot be in the future.
        Employee e = valid();
        e.setBirthDate(TODAY.plusDays(1));
        assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "Should validate birth year: {0} -> {1}")
    @CsvSource({
        "1924, false",
        "1925, false",
        "1926, true",
        "1927, true",
        "2009, true",
        "2010, true",
        "2011, false"
    })
    void should_validate_birth_year_boundaries(int year, boolean expectedValid) {
        // §"Age & Birth Date" 3-point BVA for birth year
        // Use Jan 1st to avoid birthday timing issues
        Employee e = valid();
        e.setBirthDate(LocalDate.of(year, 1, 1));
        if (expectedValid) {
            assertThat(service.save(e)).isNotNull();
        } else {
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @ParameterizedTest(name = "Should reject non-existent or invalid format date: {0}")
    @ValueSource(strings = {"12-05-2024T00:00", "2024-02-30T00:00"})
    void should_reject_invalid_date_formats(String dateStr) {
        // §"ISO 8601 Dates" — parse/reject behavior
        // These are more about java.time parsing, but documented as requirements
        assertThatThrownBy(() -> java.time.LocalDateTime.parse(dateStr))
                .isInstanceOf(java.time.format.DateTimeParseException.class);
    }

    @Test
    void should_parse_valid_iso_date() {
        // §"ISO 8601 Dates"
        java.time.LocalDateTime dt = java.time.LocalDateTime.parse("2024-05-12T00:00");
        assertThat(dt.toLocalDate()).isEqualTo(LocalDate.of(2024, 5, 12));
    }
}

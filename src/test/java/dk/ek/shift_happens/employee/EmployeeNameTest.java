package dk.ek.shift_happens.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import dk.ek.shift_happens.shift.ShiftRepository;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for Employee name validation, derived from
 * §"First Name & last name" and §1 BVA for name length in the .md file.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeNameTest {

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
        e.setBirthDate(LocalDate.of(1990, 1, 1));
        e.setHireDate(LocalDate.of(2024, 1, 1));
        e.setEmploymentStatus("ACTIVE");
        return e;
    }

    @ParameterizedTest(name = "Should accept firstName={0}")
    @ValueSource(strings = {"Jensen", "Jensen Jens", "Ø"})
    void should_accept_valid_names(String name) {
        // §"First Name & last name" Case 1, 5 and §1 BVA
        Employee e = valid();
        e.setFirstName(name);
        assertThat(service.save(e)).isNotNull();
    }

    @ParameterizedTest(name = "Should reject firstName={0}")
    @ValueSource(strings = {"", "Jensen1", "Jensen@"})
    void should_reject_invalid_names(String name) {
        // §"First Name & last name" Case 2, 3, 4
        Employee e = valid();
        e.setFirstName(name);
        assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "Should validate length boundary for firstName: {0} chars -> {1}")
    @CsvSource({
        "1, true",
        "100, true",
        "101, false"
    })
    void should_validate_name_length_boundaries(int length, boolean expectedValid) {
        // §1 BVA — name length 1-100 is Valid
        Employee e = valid();
        e.setFirstName("A".repeat(length));
        if (expectedValid) {
            assertThat(service.save(e)).isNotNull();
        } else {
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}

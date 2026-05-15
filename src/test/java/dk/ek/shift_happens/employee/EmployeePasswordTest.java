package dk.ek.shift_happens.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

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
 * Unit tests for Employee password validation, derived from
 * §"Password black box tests" and §"3-Point BVA – Password Length" in the .md file.
 */
@ExtendWith(MockitoExtension.class)
class EmployeePasswordTest {

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

    @ParameterizedTest(name = "Should reject password={0} due to composition rules")
    @ValueSource(strings = {"pass", "password", "PASSWORD", "Password"})
    void should_reject_invalid_composition_passwords(String password) {
        // §"Password black box tests" Case 1, 2, 3, 4
        Employee e = valid();
        e.setLoginPassword(password);
        assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_accept_valid_password() {
        // §"Password black box tests" Case 5
        Employee e = valid();
        e.setLoginPassword("Passw0rd");
        assertThat(service.save(e)).isNotNull();
        verify(passwordEncoder).encode("Passw0rd");
    }

    @ParameterizedTest(name = "Should validate length boundary for password: {0} chars -> {1}")
    @CsvSource({
        "7, false",
        "8, true",
        "9, true",
        "63, true",
        "64, true",
        "65, false"
    })
    void should_validate_password_length_boundaries(int length, boolean expectedValid) {
        // §"3-Point BVA – Password Length"
        Employee e = valid();
        e.setLoginPassword(pwd(length));
        if (expectedValid) {
            assertThat(service.save(e)).isNotNull();
        } else {
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    private String pwd(int totalLen) {
        String prefix = "Aa1";
        return prefix + "x".repeat(Math.max(0, totalLen - prefix.length()));
    }
}

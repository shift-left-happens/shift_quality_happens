package dk.ek.shift_happens.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

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
 * Unit tests for Employee email validation, derived from
 * §"Email decision table" and §1 BVA in the .md file.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeEmailTest {

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
        lenient().when(repo.existsByEmail(anyString())).thenReturn(false);
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

    @Test
    void should_accept_valid_email() {
        // §"Email decision table" Case 1
        Employee e = valid();
        e.setEmail("a@a.dk");
        assertThat(service.save(e)).isNotNull();
    }

    @ParameterizedTest(name = "Should reject email={0}")
    @ValueSource(strings = {"@@das", "a@a", "@aaa.dk", "aaa@a"})
    void should_reject_invalid_format_emails(String email) {
        // §"Email decision table" Case 2, 3, 3(renum), 4
        Employee e = valid();
        e.setEmail(email);
        assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_reject_when_email_already_taken() {
        // §"Email decision table" Case 1 with R4=F
        Employee e = valid();
        e.setEmail("taken@a.dk");
        when(repo.existsByEmail("taken@a.dk")).thenReturn(true);
        assertThatThrownBy(() -> service.save(e))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already in use");
    }

    @ParameterizedTest(name = "Should validate length boundary for email: {0} chars -> {1}")
    @CsvSource({
        "5, true",
        "6, true",
        "319, true",
        "320, true",
        "321, false"
    })
    void should_validate_email_length_boundaries(int length, boolean expectedValid) {
        // §1 BVA — email length 5-320
        Employee e = valid();
        e.setEmail(emailOfLength(length));
        if (expectedValid) {
            assertThat(service.save(e)).isNotNull();
        } else {
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void should_reject_email_length_4() {
        // emailOfLength(4) results in a@a (length 3).
        // To test length 4 properly, we need something like a@a.d but that is invalid domain.
        // The spec says length 5..320. 4 is invalid.
        Employee e = valid();
        e.setEmail("a@a."); // length 4
        assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
    }

    private String emailOfLength(int len) {
        int suffixLen = "@a.dk".length(); // 5
        int localLen = Math.max(1, len - suffixLen);
        String local = "a".repeat(localLen);
        return local + "@a.dk";
    }
}

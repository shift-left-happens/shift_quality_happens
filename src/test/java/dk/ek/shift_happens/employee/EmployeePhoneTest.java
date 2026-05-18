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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for Employee phone number validation, derived from
 * §"Phone number" decision table and BVA in the .md file.
 */
@ExtendWith(MockitoExtension.class)
class EmployeePhoneTest {

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

    @ParameterizedTest(name = "Decision Table Rule {0}: phone={1} -> valid={2}")
    @CsvSource({
        "R1, 12345, true",
        "R2, '+12 123456', true",
        "R2.1, '++12 123456', false",
        "R3, '+12345678', false",
        "R4, '+ 123456', false",
        "R5, 123aaa, false",
        "R6, '+123 1234567890123', false",
        "R7, 123, false",
        "R8, '+45  123', false",
        "R9, '+45-1234', false",
        "R10, ' ', false"
    })
    void should_validate_phone_number_decision_table(String ruleId, String phone, boolean expectedValid) {
        // §"Phone number" Decision Table
        Employee e = valid();
        e.setPhoneNumber(phone);
        if (expectedValid) {
            assertThat(service.save(e)).isNotNull();
        } else {
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @ParameterizedTest(name = "BVA: local phone number length {0} -> valid={1}")
    @CsvSource({"3, false", "4, true", "15, true", "16, false"})
    void should_validate_local_phone_length_bva(int digits, boolean expectedValid) {
        // §"Phone number" BVA Local phone number length 4-15
        Employee e = valid();
        e.setPhoneNumber("1".repeat(digits));
        if (expectedValid) {
            assertThat(service.save(e)).isNotNull();
        } else {
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @ParameterizedTest(name = "BVA: international phone number total digits valid={1}")
    @CsvSource({"'+998 12345678901', true", "'+998 123456789012', true", "'+998 1234567890123', false"})
    void should_validate_international_phone_total_digits_bva(String phone, boolean expectedValid) {
        // §"Phone number" BVA International number total digits 2-15
        Employee e = valid();
        e.setPhoneNumber(phone);
        if (expectedValid) {
            assertThat(service.save(e)).isNotNull();
        } else {
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @ParameterizedTest(name = "BVA: country code length valid={1}")
    @CsvSource({"'+ 1234', false", "'+1 1234', true", "'+12 1234', true", "'+998 1234', true", "'+9999 1234', false"})
    void should_validate_country_code_length_bva(String phone, boolean expectedValid) {
        // §"Phone number" BVA Country code length 1-3
        Employee e = valid();
        e.setPhoneNumber(phone);
        if (expectedValid) {
            assertThat(service.save(e)).isNotNull();
        } else {
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @ParameterizedTest(name = "Additional cases: phone={0} -> valid={1}")
    @CsvSource({"123 4567, false", "'+1 123', true", "'+45', false", "'+45 ', false"})
    void should_validate_additional_phone_cases(String phone, boolean expectedValid) {
        // §"Phone number" BVA additional cases
        Employee e = valid();
        e.setPhoneNumber(phone);
        if (expectedValid) {
            assertThat(service.save(e)).isNotNull();
        } else {
            assertThatThrownBy(() -> service.save(e)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}

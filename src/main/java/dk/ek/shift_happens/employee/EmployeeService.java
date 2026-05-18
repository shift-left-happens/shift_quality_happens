package dk.ek.shift_happens.employee;

import dk.ek.shift_happens.shift.ShiftRepository;
import dk.ek.shift_happens.shiftassignment.ShiftAssignment;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentRepository;
import dk.ek.shift_happens.shiftassignment.ShiftAssignmentService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service layer for Employee CRUD operations.
 *
 * Password handling:
 *   The PasswordEncoder bean is our PepperedPasswordEncoder, which:
 *     1. Prepends the server-side pepper to the raw password
 *     2. Hashes with BCrypt (which auto-generates a unique salt per hash)
 *   This means every stored password is: BCrypt(pepper + rawPassword + randomSalt)
 *   The salt is embedded in the BCrypt hash string itself ($2b$10$...).
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder; // PepperedPasswordEncoder (pepper + BCrypt + salt)
    private final EmployeeValidator employeeValidator;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftRepository shiftRepository;
    private final Clock clock;

    public List<Employee> findAll() {
        return this.employeeRepository.findAll();
    }

    public Optional<Employee> findById(Integer id) {
        return this.employeeRepository.findById(id);
    }

    /**
     * Create a new employee — password is hashed before storage.
     * The raw password is never stored; only the BCrypt(pepper + password) hash.
     */
    public Employee save(Employee employee) {
        boolean emailTaken = employee.getEmail() != null && employeeRepository.existsByEmail(employee.getEmail());
        employeeValidator.validateForCreate(employee, emailTaken);
        employee.setLoginPassword(passwordEncoder.encode(employee.getLoginPassword()));
        return this.employeeRepository.save(employee);
    }

    /**
     * Partial update — only non-null fields are updated.
     * If a new password is provided, it's hashed before storage.
     */
    public Optional<Employee> patch(Integer id, Employee patch) {
        return this.employeeRepository.findById(id).map(existing -> {
            if (patch.getEmployeeNumber() != null) existing.setEmployeeNumber(patch.getEmployeeNumber());
            if (patch.getFirstName() != null) {
                employeeValidator.validateName(patch.getFirstName(), "firstName");
                existing.setFirstName(patch.getFirstName());
            }
            if (patch.getLastName() != null) {
                employeeValidator.validateName(patch.getLastName(), "lastName");
                existing.setLastName(patch.getLastName());
            }
            if (patch.getEmail() != null) {
                boolean taken = !patch.getEmail().equals(existing.getEmail())
                        && employeeRepository.existsByEmail(patch.getEmail());
                employeeValidator.validateEmail(patch.getEmail(), taken);
                existing.setEmail(patch.getEmail());
            }
            if (patch.getLoginPassword() != null) {
                employeeValidator.validatePassword(patch.getLoginPassword());
                existing.setLoginPassword(passwordEncoder.encode(patch.getLoginPassword()));
            }
            if (patch.getUserRole() != null) existing.setUserRole(patch.getUserRole());
            if (patch.getPhoneNumber() != null) {
                employeeValidator.validatePhoneNumber(patch.getPhoneNumber());
                existing.setPhoneNumber(patch.getPhoneNumber());
            }
            if (patch.getHireDate() != null) {
                employeeValidator.validateHireDate(patch.getHireDate());
                existing.setHireDate(patch.getHireDate());
            }
            if (patch.getBirthDate() != null) {
                employeeValidator.validateBirthDate(patch.getBirthDate());
                existing.setBirthDate(patch.getBirthDate());
            }
            if (patch.getEmploymentStatus() != null) {
                employeeValidator.validateEmploymentStatus(patch.getEmploymentStatus());
                existing.setEmploymentStatus(patch.getEmploymentStatus());
            }
            if (patch.getPrimaryWorkLocationId() != null)
                existing.setPrimaryWorkLocationId(patch.getPrimaryWorkLocationId());
            return this.employeeRepository.save(existing);
        });
    }

    public void delete(Integer id) {
        Employee employee = employeeRepository
                .findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + id));

        // docx §"Deletion Constraints" case 1 — block when future non-cancelled assignments exist
        LocalDateTime now = LocalDateTime.now(clock);
        boolean hasFutureAssignment = false;
        for (ShiftAssignment a : shiftAssignmentRepository.findByEmployeeId(id)) {
            if (ShiftAssignmentService.STATUS_CANCELLED.equalsIgnoreCase(a.getAssignmentStatus())) continue;
            var maybeShift = shiftRepository.findById(a.getShiftId());
            if (maybeShift.isEmpty()) continue;
            var shift = maybeShift.get();
            if (shift.getStartDatetime() != null && shift.getStartDatetime().isAfter(now)) {
                hasFutureAssignment = true;
                break;
            }
        }
        if (hasFutureAssignment) {
            throw new IllegalArgumentException("Employee has future assigned shifts and cannot be deleted");
        }

        employeeRepository.delete(employee);
    }
}

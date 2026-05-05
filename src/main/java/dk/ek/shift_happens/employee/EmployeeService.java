package dk.ek.shift_happens.employee;

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
            if (patch.getFirstName() != null) existing.setFirstName(patch.getFirstName());
            if (patch.getLastName() != null) existing.setLastName(patch.getLastName());
            if (patch.getEmail() != null) existing.setEmail(patch.getEmail());
            if (patch.getLoginPassword() != null)
                existing.setLoginPassword(passwordEncoder.encode(patch.getLoginPassword()));
            if (patch.getUserRole() != null) existing.setUserRole(patch.getUserRole());
            if (patch.getPhoneNumber() != null) existing.setPhoneNumber(patch.getPhoneNumber());
            if (patch.getHireDate() != null) existing.setHireDate(patch.getHireDate());
            if (patch.getEmploymentStatus() != null) existing.setEmploymentStatus(patch.getEmploymentStatus());
            if (patch.getPrimaryWorkLocationId() != null)
                existing.setPrimaryWorkLocationId(patch.getPrimaryWorkLocationId());
            return this.employeeRepository.save(existing);
        });
    }

    public void delete(Integer id) {
        Employee employee = employeeRepository
                .findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + id));
        employeeRepository.delete(employee);
    }
}

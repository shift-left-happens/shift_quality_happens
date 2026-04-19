package dk.ek.shift_happens.employeejobrole;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/employeejobroles")
@RequiredArgsConstructor
public class EmployeeJobRoleController {

    private final EmployeeJobRoleService service;
    private final EmployeeRepository employeeRepository;

// Auth required — employees see only their own data
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<EmployeeJobRole> getAll(Authentication auth) {

        if (isEmployee(auth)) {
            Integer employeeId = getEmployeeIdFromEmail(auth.getName());
            return service.getByEmployeeId(employeeId);
        }

        return service.getAll();
    }

    // Auth required — employees can only access their own record
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public EmployeeJobRole getById(@PathVariable Integer id,
                                   Authentication auth) {

        EmployeeJobRole role = service.getById(id);

        if (isEmployee(auth) &&
                !role.getEmployeeId().equals(getEmployeeIdFromEmail(auth.getName()))) {
            throw forbidden();
        }

        return role;
    }

    // Create job role assignment for the currently authenticated user
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER','EMPLOYEE')")
    public EmployeeJobRole create(Authentication auth,
                                  @RequestBody EmployeeJobRole role) {

        role.setEmployeeId(getEmployeeIdFromEmail(auth.getName()));
        return service.create(role);
    }

    // Update existing record (only admin or owner)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER','EMPLOYEE')")
    public EmployeeJobRole update(@PathVariable Integer id,
                                  @RequestBody EmployeeJobRole role,
                                  Authentication auth) {

        EmployeeJobRole existing = service.getById(id);

        if (isEmployee(auth) &&
                !existing.getEmployeeId().equals(getEmployeeIdFromEmail(auth.getName()))) {
            throw forbidden();
        }

        return service.update(id, role);
    }

    // Delete record (only admin or owner)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER','EMPLOYEE')")
    public void delete(@PathVariable Integer id,
                       Authentication auth) {

        EmployeeJobRole existing = service.getById(id);

        if (isEmployee(auth) &&
                !existing.getEmployeeId().equals(getEmployeeIdFromEmail(auth.getName()))) {
            throw forbidden();
        }

        service.delete(id);
    }

    // =========================
    // HELPERS
    // =========================

    private boolean isEmployee(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE"));
    }

    private Integer getEmployeeIdFromEmail(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Employee not found"))
                .getEmployeeId();
    }

    private ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
}
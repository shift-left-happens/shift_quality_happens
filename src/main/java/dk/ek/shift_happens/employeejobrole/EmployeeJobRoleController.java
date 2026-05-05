package dk.ek.shift_happens.employeejobrole;

import dk.ek.shift_happens.auth.AuthHelper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employeejobroles")
@RequiredArgsConstructor
public class EmployeeJobRoleController {

    private final EmployeeJobRoleService service;
    private final AuthHelper authHelper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<EmployeeJobRole> getAll(Authentication auth) {
        if (authHelper.isEmployee(auth)) {
            return service.getByEmployeeId(authHelper.currentEmployeeId(auth));
        }
        return service.getAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public EmployeeJobRole getById(@PathVariable Integer id, Authentication auth) {
        EmployeeJobRole role = service.getById(id);

        if (authHelper.isEmployee(auth) && !role.getEmployeeId().equals(authHelper.currentEmployeeId(auth))) {
            throw authHelper.forbidden();
        }
        return role;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public EmployeeJobRole create(@RequestBody EmployeeJobRole role) {
        return service.create(role);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public EmployeeJobRole update(@PathVariable Integer id, @RequestBody EmployeeJobRole role) {
        return service.update(id, role);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}

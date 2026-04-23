package dk.ek.shift_happens.employee;

import dk.ek.shift_happens.auth.AuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final AuthHelper authHelper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Employee>> getEmployees(Authentication auth) {
        if (authHelper.isEmployee(auth)) {
            Integer selfId = authHelper.currentEmployeeId(auth);
            return ResponseEntity.ok(
                    this.employeeService.findById(selfId).map(List::of).orElse(List.of()));
        }
        return ResponseEntity.ok(this.employeeService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Employee> getEmployee(@PathVariable Integer id, Authentication auth) {
        if (authHelper.isEmployee(auth) && !id.equals(authHelper.currentEmployeeId(auth))) {
            throw authHelper.forbidden();
        }
        return this.employeeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ResponseEntity<Employee> createEmployee(@RequestBody Employee employee) {
        return ResponseEntity.status(201).body(this.employeeService.save(employee));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ResponseEntity<Employee> patchEmployee(@PathVariable Integer id, @RequestBody Employee employee) {
        return this.employeeService.patch(id, employee)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Integer id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

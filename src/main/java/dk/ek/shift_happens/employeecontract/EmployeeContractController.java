package dk.ek.shift_happens.employeecontract;

import dk.ek.shift_happens.auth.AuthHelper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/employeecontracts")
@RequiredArgsConstructor
public class EmployeeContractController {

    private final EmployeeContractRepository employeeContractRepository;
    private final AuthHelper authHelper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<EmployeeContract> getAll(Authentication auth) {
        if (authHelper.isEmployee(auth)) {
            return employeeContractRepository.findByEmployeeId(authHelper.currentEmployeeId(auth));
        }
        return employeeContractRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public EmployeeContract getById(@PathVariable Integer id, Authentication auth) {
        EmployeeContract contract = employeeContractRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (authHelper.isEmployee(auth) && !contract.getEmployeeId().equals(authHelper.currentEmployeeId(auth))) {
            throw authHelper.forbidden();
        }
        return contract;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeContract create(@RequestBody EmployeeContract contract) {
        return employeeContractRepository.save(contract);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public EmployeeContract update(@PathVariable Integer id, @RequestBody EmployeeContract details) {
        EmployeeContract existing = employeeContractRepository
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        existing.setEmployeeId(details.getEmployeeId());
        existing.setDepartmentId(details.getDepartmentId());
        existing.setContractType(details.getContractType());
        existing.setStartDate(details.getStartDate());
        existing.setEndDate(details.getEndDate());
        existing.setWeeklyHours(details.getWeeklyHours());
        existing.setSalaryAmount(details.getSalaryAmount());
        existing.setIsActive(details.getIsActive());
        return employeeContractRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        employeeContractRepository.deleteById(id);
    }
}

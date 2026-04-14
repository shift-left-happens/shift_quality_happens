package dk.ek.shift_happens.employee;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public List<Employee> findAll() {
        return this.employeeRepository.findAll();
    }

    public Optional<Employee> findById(Integer id) {
        return this.employeeRepository.findById(id);
    }

    public Employee save(Employee employee) {
        return this.employeeRepository.save(employee);
    }

    public Optional<Employee> patch(Integer id, Employee patch) {
        return this.employeeRepository.findById(id).map(existing -> {
            if (patch.getEmployeeNumber() != null) existing.setEmployeeNumber(patch.getEmployeeNumber());
            if (patch.getFirstName() != null) existing.setFirstName(patch.getFirstName());
            if (patch.getLastName() != null) existing.setLastName(patch.getLastName());
            if (patch.getEmail() != null) existing.setEmail(patch.getEmail());
            if (patch.getLoginPassword() != null) existing.setLoginPassword(patch.getLoginPassword());
            if (patch.getFkUserRoleId() != null) existing.setFkUserRoleId(patch.getFkUserRoleId());
            if (patch.getPhoneNumber() != null) existing.setPhoneNumber(patch.getPhoneNumber());
            if (patch.getHireDate() != null) existing.setHireDate(patch.getHireDate());
            if (patch.getEmploymentStatus() != null) existing.setEmploymentStatus(patch.getEmploymentStatus());
            if (patch.getPrimaryWorkLocationId() != null) existing.setPrimaryWorkLocationId(patch.getPrimaryWorkLocationId());
            return this.employeeRepository.save(existing);
        });
    }

    public delete(Integer id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id: " + id));
        employeeRepository.delete(employee);
    }
}

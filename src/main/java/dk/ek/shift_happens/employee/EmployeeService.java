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
            if (patch.getEmployee_number() != null) existing.setEmployee_number(patch.getEmployee_number());
            if (patch.getFirst_name() != null) existing.setFirst_name(patch.getFirst_name());
            if (patch.getLast_name() != null) existing.setLast_name(patch.getLast_name());
            if (patch.getEmail() != null) existing.setEmail(patch.getEmail());
            if (patch.getLogin_password() != null) existing.setLogin_password(patch.getLogin_password());
            if (patch.getFk_user_role_id() != null) existing.setFk_user_role_id(patch.getFk_user_role_id());
            if (patch.getPhone_number() != null) existing.setPhone_number(patch.getPhone_number());
            if (patch.getHire_date() != null) existing.setHire_date(patch.getHire_date());
            if (patch.getEmployment_status() != null) existing.setEmployment_status(patch.getEmployment_status());
            if (patch.getPrimary_work_location_id() != null) existing.setPrimary_work_location_id(patch.getPrimary_work_location_id());
            return this.employeeRepository.save(existing);
        });
    }
}

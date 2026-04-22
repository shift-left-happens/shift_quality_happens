package dk.ek.shift_happens.employeejobrole;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeJobRoleService {

    private final EmployeeJobRoleRepository repository;

    public List<EmployeeJobRole> getAll() {
        return repository.findAll();
    }

    public List<EmployeeJobRole> getByEmployeeId(Integer employeeId) {
        return repository.findByEmployeeId(employeeId);
    }

    public EmployeeJobRole getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
    }

    public EmployeeJobRole create(EmployeeJobRole role) {
        return repository.save(role);
    }

    public EmployeeJobRole update(Integer id, EmployeeJobRole updated) {
        EmployeeJobRole existing = getById(id);

        existing.setJobRoleId(updated.getJobRoleId());
        existing.setAssignedDate(updated.getAssignedDate());
        existing.setExpiryDate(updated.getExpiryDate());
        existing.setProficiencyLevel(updated.getProficiencyLevel());

        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}
package dk.ek.shift_happens.employeejobrole;

import dk.ek.shift_happens.employee.EmployeeRepository;
import dk.ek.shift_happens.jobrole.JobRoleRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EmployeeJobRoleService {

    private static final Set<String> VALID_PROFICIENCY = Set.of("Trainee", "Junior", "Senior", "Lead");

    private final EmployeeJobRoleRepository repository;
    private final EmployeeRepository employeeRepository;
    private final JobRoleRepository jobRoleRepository;

    public List<EmployeeJobRole> getAll() {
        return repository.findAll();
    }

    public List<EmployeeJobRole> getByEmployeeId(Integer employeeId) {
        return repository.findByEmployeeId(employeeId);
    }

    public EmployeeJobRole getById(Integer id) {
        return repository
                .findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee job role not found with id " + id));
    }

    public EmployeeJobRole create(EmployeeJobRole role) {
        validate(role);

        Optional<EmployeeJobRole> duplicate =
                repository.findByEmployeeIdAndJobRoleId(role.getEmployeeId(), role.getJobRoleId());
        if (duplicate.isPresent()) {
            throw new IllegalArgumentException("Employee already holds this job role");
        }

        role.setEmployeeJobRoleId(null);
        return repository.save(role);
    }

    public EmployeeJobRole update(Integer id, EmployeeJobRole updated) {
        EmployeeJobRole existing = getById(id);

        if (updated.getJobRoleId() != null) existing.setJobRoleId(updated.getJobRoleId());
        if (updated.getAssignedDate() != null) existing.setAssignedDate(updated.getAssignedDate());
        if (updated.getExpiryDate() != null) existing.setExpiryDate(updated.getExpiryDate());
        if (updated.getProficiencyLevel() != null) existing.setProficiencyLevel(updated.getProficiencyLevel());

        validate(existing);

        Optional<EmployeeJobRole> duplicate =
                repository.findByEmployeeIdAndJobRoleId(existing.getEmployeeId(), existing.getJobRoleId());
        if (duplicate.isPresent() && !duplicate.get().getEmployeeJobRoleId().equals(id)) {
            throw new IllegalArgumentException("Employee already holds this job role");
        }

        return repository.save(existing);
    }

    public void delete(Integer id) {
        EmployeeJobRole existing = getById(id);
        repository.delete(existing);
    }

    private void validate(EmployeeJobRole role) {
        if (role.getEmployeeId() == null || role.getEmployeeId() <= 0) {
            throw new IllegalArgumentException("employeeId is required and must be positive");
        }
        if (!employeeRepository.existsById(role.getEmployeeId())) {
            throw new IllegalArgumentException("employeeId " + role.getEmployeeId() + " does not exist");
        }
        if (role.getJobRoleId() == null || role.getJobRoleId() <= 0) {
            throw new IllegalArgumentException("jobRoleId is required and must be positive");
        }
        if (!jobRoleRepository.existsById(role.getJobRoleId())) {
            throw new IllegalArgumentException("jobRoleId " + role.getJobRoleId() + " does not exist");
        }
        if (role.getAssignedDate() != null
                && role.getExpiryDate() != null
                && role.getExpiryDate().isBefore(role.getAssignedDate())) {
            throw new IllegalArgumentException("expiryDate cannot be before assignedDate");
        }
        if (role.getProficiencyLevel() != null
                && !role.getProficiencyLevel().isBlank()
                && !VALID_PROFICIENCY.contains(role.getProficiencyLevel())) {
            throw new IllegalArgumentException("proficiencyLevel must be one of " + VALID_PROFICIENCY);
        }
    }
}

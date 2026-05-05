package dk.ek.shift_happens.department;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    public Department findById(Integer id) {
        return departmentRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found with id " + id));
    }

    public Department create(Department department) {
        return departmentRepository.save(department);
    }

    public Department update(Integer id, Department updates) {
        Department existing = findById(id);

        if (updates.getDepartmentName() != null) {
            existing.setDepartmentName(updates.getDepartmentName());
        }
        if (updates.getIsActive() != null) {
            existing.setIsActive(updates.getIsActive());
        }

        return departmentRepository.save(existing);
    }

    public void delete(Integer id) {
        Department existing = findById(id);
        departmentRepository.delete(existing);
    }
}

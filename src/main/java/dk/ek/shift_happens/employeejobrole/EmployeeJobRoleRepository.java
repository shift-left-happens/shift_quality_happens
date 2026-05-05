package dk.ek.shift_happens.employeejobrole;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeJobRoleRepository extends JpaRepository<EmployeeJobRole, Integer> {
    List<EmployeeJobRole> findByEmployeeId(Integer employeeId);
}

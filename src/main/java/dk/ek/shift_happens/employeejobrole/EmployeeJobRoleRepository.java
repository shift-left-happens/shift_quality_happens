package dk.ek.shift_happens.employeejobrole;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeJobRoleRepository extends JpaRepository<EmployeeJobRole, Integer> {
    List<EmployeeJobRole> findByEmployeeId(Integer employeeId);

    Optional<EmployeeJobRole> findByEmployeeIdAndJobRoleId(Integer employeeId, Integer jobRoleId);

    boolean existsByJobRoleId(Integer jobRoleId);
}

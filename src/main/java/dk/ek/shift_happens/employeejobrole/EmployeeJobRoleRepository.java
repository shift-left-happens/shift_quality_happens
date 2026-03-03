package dk.ek.shift_happens.employeejobrole;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeJobRoleRepository extends JpaRepository<EmployeeJobRole, Integer> {
    List<EmployeeJobRole> findAll();
}

package dk.ek.shift_happens.department;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    // You can add custom queries here if needed
    List<Department> findAll();
}

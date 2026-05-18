package dk.ek.shift_happens.shiftassignment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Integer> {
    List<ShiftAssignment> findAll();

    List<ShiftAssignment> findByEmployeeId(Integer employeeId);
}

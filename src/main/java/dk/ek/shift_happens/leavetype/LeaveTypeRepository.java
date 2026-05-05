package dk.ek.shift_happens.leavetype;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Integer> {
    List<LeaveType> findAll();
}

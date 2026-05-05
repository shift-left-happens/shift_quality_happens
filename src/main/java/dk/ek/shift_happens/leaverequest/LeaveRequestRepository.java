package dk.ek.shift_happens.leaverequest;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Integer> {
    List<LeaveRequest> findAll();

    List<LeaveRequest> findByEmployeeId(Integer employeeId);
}

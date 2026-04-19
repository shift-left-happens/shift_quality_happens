package dk.ek.shift_happens.leaveapproval;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveApprovalRepository extends JpaRepository<LeaveApproval, Integer> {
    List<LeaveApproval> findAll();
    List<LeaveApproval> findByLeaveRequestId(Integer leaveRequestId);
}

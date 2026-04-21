package dk.ek.shift_happens.leaveapproval;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveApprovalRepository extends JpaRepository<LeaveApproval, Integer> {
    List<LeaveApproval> findAll();
    List<LeaveApproval> findByLeaveRequestId(Integer leaveRequestId);

    @Query("SELECT la FROM LeaveApproval la WHERE la.leaveRequestId IN "
            + "(SELECT lr.leaveRequestId FROM dk.ek.shift_happens.leaverequest.LeaveRequest lr WHERE lr.employeeId = :employeeId)")
    List<LeaveApproval> findByRequestOwner(@Param("employeeId") Integer employeeId);
}

package dk.ek.shift_happens.leaveapproval;

import dk.ek.shift_happens.employee.Employee;
import dk.ek.shift_happens.employee.EmployeeRepository;
import dk.ek.shift_happens.leaverequest.LeaveRequest;
import dk.ek.shift_happens.leaverequest.LeaveRequestRepository;
import dk.ek.shift_happens.userrole.UserRole;
import dk.ek.shift_happens.userrole.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaveApprovalService {

    private final LeaveApprovalRepository leaveApprovalRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRoleRepository userRoleRepository;

    public List<LeaveApproval> findAll() {
        return this.leaveApprovalRepository.findAll();
    }

    public Optional<LeaveApproval> findById(Integer id) {
        return this.leaveApprovalRepository.findById(id);
    }

    public List<LeaveApproval> findByLeaveRequestId(Integer leaveRequestId) {
        return this.leaveApprovalRepository.findByLeaveRequestId(leaveRequestId);
    }

    public LeaveApproval approve(LeaveApproval approval) {
        if (approval.getLeaveRequestId() == null) {
            throw new IllegalArgumentException("leaveRequestId is required");
        }
        if (approval.getApproverEmployeeId() == null) {
            throw new IllegalArgumentException("approverEmployeeId is required");
        }
        if (approval.getDecision() == null || approval.getDecision().isBlank()) {
            throw new IllegalArgumentException("decision is required");
        }

        String normalizedDecision = approval.getDecision().trim().toUpperCase(Locale.ROOT);
        if (!normalizedDecision.equals("APPROVED") && !normalizedDecision.equals("REJECTED")) {
            throw new IllegalArgumentException("decision must be APPROVED or REJECTED");
        }

        LeaveRequest leaveRequest = this.leaveRequestRepository.findById(approval.getLeaveRequestId())
                .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));

        if (!"PENDING".equalsIgnoreCase(leaveRequest.getRequestStatus())) {
            throw new IllegalArgumentException("Only pending leave requests can be approved");
        }

        Employee approver = this.employeeRepository.findById(approval.getApproverEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Approver not found"));

        UserRole role = this.userRoleRepository.findById(approver.getFkUserRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Approver role not found"));

        String roleName = role.getUserRoleName() == null ? "" : role.getUserRoleName().trim().toLowerCase(Locale.ROOT);
        if (!roleName.equals("administrator") && !roleName.equals("manager")) {
            throw new IllegalArgumentException("Only Administrator or Manager can approve leave requests");
        }

        approval.setLeaveApprovalId(null);
        approval.setDecision(normalizedDecision);
        approval.setDecisionDatetime(LocalDateTime.now());
        LeaveApproval savedApproval = this.leaveApprovalRepository.save(approval);

        leaveRequest.setRequestStatus(normalizedDecision);
        this.leaveRequestRepository.save(leaveRequest);

        return savedApproval;
    }

    public boolean delete(Integer id) {
        if (!this.leaveApprovalRepository.existsById(id)) {
            return false;
        }
        this.leaveApprovalRepository.deleteById(id);
        return true;
    }
}

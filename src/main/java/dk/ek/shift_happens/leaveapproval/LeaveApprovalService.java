package dk.ek.shift_happens.leaveapproval;

import dk.ek.shift_happens.employee.Employee;
import dk.ek.shift_happens.employee.EmployeeRepository;
import dk.ek.shift_happens.employee.UserRole;
import dk.ek.shift_happens.leaverequest.LeaveRequest;
import dk.ek.shift_happens.leaverequest.LeaveRequestRepository;
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

    public List<LeaveApproval> findAll() {
        return this.leaveApprovalRepository.findAll();
    }

    public Optional<LeaveApproval> findById(Integer id) {
        return this.leaveApprovalRepository.findById(id);
    }

    public List<LeaveApproval> findByLeaveRequestId(Integer leaveRequestId) {
        return this.leaveApprovalRepository.findByLeaveRequestId(leaveRequestId);
    }

    public List<LeaveApproval> findByRequestOwner(Integer employeeId) {
        return this.leaveApprovalRepository.findByRequestOwner(employeeId);
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

        UserRole role = approver.getUserRole();
        if (role == null) {
            throw new IllegalArgumentException("Approver role not found");
        }

        if (role != UserRole.Administrator && role != UserRole.Manager) {
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

    public Optional<LeaveApproval> update(Integer id, LeaveApproval details) {
        return this.leaveApprovalRepository.findById(id).map(existing -> {
            if (details.getDecision() != null && !details.getDecision().isBlank()) {
                String normalized = details.getDecision().trim().toUpperCase(Locale.ROOT);
                if (!normalized.equals("APPROVED") && !normalized.equals("REJECTED")) {
                    throw new IllegalArgumentException("decision must be APPROVED or REJECTED");
                }
                existing.setDecision(normalized);

                this.leaveRequestRepository.findById(existing.getLeaveRequestId())
                        .ifPresent(req -> {
                            req.setRequestStatus(normalized);
                            this.leaveRequestRepository.save(req);
                        });
            }

            if (details.getLeaveComment() != null) {
                existing.setLeaveComment(details.getLeaveComment());
            }

            existing.setDecisionDatetime(LocalDateTime.now());
            return this.leaveApprovalRepository.save(existing);
        });
    }

    public boolean delete(Integer id) {
        if (!this.leaveApprovalRepository.existsById(id)) {
            return false;
        }
        this.leaveApprovalRepository.deleteById(id);
        return true;
    }
}

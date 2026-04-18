package dk.ek.shift_happens.leaverequest;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;

    public List<LeaveRequest> findAll() {
        return this.leaveRequestRepository.findAll();
    }

    public Optional<LeaveRequest> findById(Integer id) {
        return this.leaveRequestRepository.findById(id);
    }

    public LeaveRequest create(LeaveRequest leaveRequest) {
        validateRequest(leaveRequest);
        leaveRequest.setLeaveRequestId(null);
        if (leaveRequest.getRequestStatus() == null || leaveRequest.getRequestStatus().isBlank()) {
            leaveRequest.setRequestStatus("PENDING");
        }
        if (leaveRequest.getRequestedDatetime() == null) {
            leaveRequest.setRequestedDatetime(LocalDateTime.now());
        }
        return this.leaveRequestRepository.save(leaveRequest);
    }

    public Optional<LeaveRequest> patch(Integer id, LeaveRequest patch) {
        return this.leaveRequestRepository.findById(id).map(existing -> {
            if (patch.getLeaveTypeId() != null) existing.setLeaveTypeId(patch.getLeaveTypeId());
            if (patch.getStartDate() != null) existing.setStartDate(patch.getStartDate());
            if (patch.getEndDate() != null) existing.setEndDate(patch.getEndDate());
            if (patch.getRequestStatus() != null) existing.setRequestStatus(patch.getRequestStatus());
            if (patch.getReason() != null) existing.setReason(patch.getReason());
            validateRequest(existing);
            return this.leaveRequestRepository.save(existing);
        });
    }

    public boolean delete(Integer id) {
        if (!this.leaveRequestRepository.existsById(id)) {
            return false;
        }
        this.leaveRequestRepository.deleteById(id);
        return true;
    }

    private void validateRequest(LeaveRequest leaveRequest) {
        if (leaveRequest.getEmployeeId() == null) {
            throw new IllegalArgumentException("employeeId is required");
        }
        if (leaveRequest.getLeaveTypeId() == null) {
            throw new IllegalArgumentException("leaveTypeId is required");
        }
        if (leaveRequest.getStartDate() == null || leaveRequest.getEndDate() == null) {
            throw new IllegalArgumentException("start and end date are required");
        }
        if (leaveRequest.getStartDate().isAfter(leaveRequest.getEndDate())) {
            throw new IllegalArgumentException("start date cannot be after end date");
        }
    }
}

package dk.ek.shift_happens.view.employeeleaveoverview;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeLeaveOverviewDto(
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        String leaveTypeName,
        Boolean isPaidLeave,
        Boolean requiresApproval,
        LocalDate startDate,
        LocalDate endDate,
        String requestStatus,
        String reason,
        LocalDateTime requestedDatetime,
        String approvalDecision,
        String approvalComment,
        LocalDateTime approvalDatetime,
        String approverNumber,
        String approverFirstName,
        String approverLastName) {
    public static EmployeeLeaveOverviewDto from(EmployeeLeaveOverviewView view) {
        return new EmployeeLeaveOverviewDto(
                view.getEmployeeNumber(),
                view.getFirstName(),
                view.getLastName(),
                view.getEmail(),
                view.getLeaveTypeName(),
                view.getIsPaidLeave(),
                view.getRequiresApproval(),
                view.getStartDate(),
                view.getEndDate(),
                view.getRequestStatus(),
                view.getReason(),
                view.getRequestedDatetime(),
                view.getApprovalDecision(),
                view.getApprovalComment(),
                view.getApprovalDatetime(),
                view.getApproverNumber(),
                view.getApproverFirstName(),
                view.getApproverLastName());
    }
}

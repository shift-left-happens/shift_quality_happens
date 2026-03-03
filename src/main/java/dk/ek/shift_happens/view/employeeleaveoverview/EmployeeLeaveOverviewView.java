package dk.ek.shift_happens.view.employeeleaveoverview;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "vw_employee_leave_overview")
public class EmployeeLeaveOverviewView {

    @Id
    @Column(name = "leave_request_id")
    private Integer leaveRequestId;

    @Column(name = "employee_id")
    private Integer employeeId;

    @Column(name = "employee_number")
    private String employeeNumber;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "leave_type_id")
    private Integer leaveTypeId;

    @Column(name = "leave_type_name")
    private String leaveTypeName;

    @Column(name = "is_paid_leave")
    private Boolean isPaidLeave;

    @Column(name = "requires_approval")
    private Boolean requiresApproval;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "request_status")
    private String requestStatus;

    @Column(name = "reason")
    private String reason;

    @Column(name = "requested_datetime")
    private LocalDateTime requestedDatetime;

    @Column(name = "leave_approval_id")
    private Integer leaveApprovalId;

    @Column(name = "approval_decision")
    private String approvalDecision;

    @Column(name = "approval_comment")
    private String approvalComment;

    @Column(name = "approval_datetime")
    private LocalDateTime approvalDatetime;

    @Column(name = "approver_number")
    private String approverNumber;

    @Column(name = "approver_first_name")
    private String approverFirstName;

    @Column(name = "approver_last_name")
    private String approverLastName;
}

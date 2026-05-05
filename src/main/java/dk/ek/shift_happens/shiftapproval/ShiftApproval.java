package dk.ek.shift_happens.shiftapproval;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "shift_approval")
public class ShiftApproval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_approval_id")
    private Integer shiftApprovalId;

    @Column(name = "shift_assignment_id", nullable = false)
    private Integer shiftAssignmentId;

    @Column(name = "approver_employee_id", nullable = false)
    private Integer approverEmployeeId;

    @Column(name = "decision")
    private String decision;

    @Column(name = "approval_comment")
    private String approvalComment;

    @Column(name = "decision_datetime")
    private LocalDateTime decisionDatetime;
}

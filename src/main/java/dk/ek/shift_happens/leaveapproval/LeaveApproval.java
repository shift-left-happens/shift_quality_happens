package dk.ek.shift_happens.leaveapproval;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "leaveapproval")
public class LeaveApproval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_approval_id")
    private Integer leave_approval_id;

    @Column(name = "leave_request_id", nullable = false)
    private Integer leave_request_id;

    @Column(name = "approver_employee_id", nullable = false)
    private Integer approver_employee_id;

    @Column(name = "decision")
    private String decision;

    @Column(name = "leave_comment")
    private String leave_comment;

    @Column(name = "decision_datetime")
    private LocalDateTime decision_datetime;
}

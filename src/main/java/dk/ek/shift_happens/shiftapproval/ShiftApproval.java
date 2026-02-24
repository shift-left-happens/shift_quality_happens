package dk.ek.shift_happens.shiftapproval;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "shiftapproval")
public class ShiftApproval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_approval_id")
    private Integer shift_approval_id;

    @Column(name = "shift_assignment_id", nullable = false)
    private Integer shift_assignment_id;

    @Column(name = "approver_employee_id", nullable = false)
    private Integer approver_employee_id;

    @Column(name = "decision")
    private String decision;

    @Column(name = "approval_comment")
    private String approval_comment;

    @Column(name = "decision_datetime")
    private LocalDateTime decision_datetime;
}

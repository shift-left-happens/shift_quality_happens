package dk.ek.shift_happens.shiftswapapproval;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "shift_swap_approval")
public class ShiftSwapApproval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_swap_approval_id")
    private Integer shiftSwapApprovalId;

    @Column(name = "shift_swap_id", nullable = false)
    private Integer shiftSwapId;

    @Column(name = "approver_employee_id", nullable = false)
    private Integer approverEmployeeId;

    @Column(name = "decision")
    private String decision;

    @Column(name = "shift_swap_comment")
    private String shiftSwapComment;

    @Column(name = "decision_datetime")
    private LocalDateTime decisionDatetime;
}

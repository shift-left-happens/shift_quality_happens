package dk.ek.shift_happens.shiftswapapproval;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "shiftswapapproval")
public class ShiftSwapApproval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_swap_approval_id")
    private Integer shift_swap_approval_id;

    @Column(name = "shift_swap_id", nullable = false)
    private Integer shift_swap_id;

    @Column(name = "approver_employee_id", nullable = false)
    private Integer approver_employee_id;

    @Column(name = "decision")
    private String decision;

    @Column(name = "shift_swap_comment")
    private String shift_swap_comment;

    @Column(name = "decision_datetime")
    private LocalDateTime decision_datetime;
}

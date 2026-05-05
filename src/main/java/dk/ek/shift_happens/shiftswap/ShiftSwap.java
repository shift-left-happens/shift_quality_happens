package dk.ek.shift_happens.shiftswap;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "shift_swap")
public class ShiftSwap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_swap_id")
    private Integer shiftSwapId;

    @Column(name = "original_shift_assignment_id", nullable = false)
    private Integer originalShiftAssignmentId;

    @Column(name = "employee_from_id", nullable = false)
    private Integer employeeFromId;

    @Column(name = "employee_to_id", nullable = false)
    private Integer employeeToId;

    @Column(name = "swap_status")
    private String swapStatus;

    @Column(name = "request_datetime")
    private LocalDateTime requestDatetime;

    @Column(name = "reason")
    private String reason;
}

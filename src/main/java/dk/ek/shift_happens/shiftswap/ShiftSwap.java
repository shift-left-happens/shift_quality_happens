package dk.ek.shift_happens.shiftswap;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "shiftswap")
public class ShiftSwap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_swap_id")
    private Integer shift_swap_id;

    @Column(name = "original_shift_assignment_id", nullable = false)
    private Integer original_shift_assignment_id;

    @Column(name = "employee_from_id", nullable = false)
    private Integer employee_from_id;

    @Column(name = "employee_to_id", nullable = false)
    private Integer employee_to_id;

    @Column(name = "swap_status")
    private String swap_status;

    @Column(name = "request_datetime")
    private LocalDateTime request_datetime;

    @Column(name = "reason")
    private String reason;
}

package dk.ek.shift_happens.shiftassignment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "shiftassignment")
public class ShiftAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_assignment_id")
    private Integer shift_assignment_id;

    @Column(name = "shift_id", nullable = false)
    private Integer shift_id;

    @Column(name = "employee_id", nullable = false)
    private Integer employee_id;

    @Column(name = "assignment_status")
    private String assignment_status;

    @Column(name = "assigned_datetime")
    private LocalDateTime assigned_datetime;

    @Column(name = "check_in_datetime")
    private LocalDateTime check_in_datetime;

    @Column(name = "check_out_datetime")
    private LocalDateTime check_out_datetime;
}

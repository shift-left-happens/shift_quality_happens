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
@Table(name = "shift_assignment")
public class ShiftAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_assignment_id")
    private Integer shiftAssignmentId;

    @Column(name = "shift_id", nullable = false)
    private Integer shiftId;

    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    @Column(name = "assignment_status")
    private String assignmentStatus;

    @Column(name = "assigned_datetime")
    private LocalDateTime assignedDatetime;

    @Column(name = "check_in_datetime")
    private LocalDateTime checkInDatetime;

    @Column(name = "check_out_datetime")
    private LocalDateTime checkOutDatetime;
}

package dk.ek.shift_happens.shift;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "shift")
public class Shift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_id")
    private Integer shiftId;

    @Column(name = "department_id", nullable = false)
    private Integer departmentId;

    @Column(name = "work_location_id", nullable = false)
    private Integer workLocationId;

    @Column(name = "shift_name")
    private String shiftName;

    @Column(name = "start_datetime")
    private LocalDateTime startDatetime;

    @Column(name = "end_datetime")
    private LocalDateTime endDatetime;

    @Column(name = "shift_status")
    private String shiftStatus;
}

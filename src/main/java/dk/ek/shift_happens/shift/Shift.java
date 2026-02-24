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
    private Integer shift_id;

    @Column(name = "department_id", nullable = false)
    private Integer department_id;

    @Column(name = "work_location_id", nullable = false)
    private Integer work_location_id;

    @Column(name = "shift_name")
    private String shift_name;

    @Column(name = "start_datetime")
    private LocalDateTime start_datetime;

    @Column(name = "end_datetime")
    private LocalDateTime end_datetime;

    @Column(name = "shift_status")
    private String shift_status;
}

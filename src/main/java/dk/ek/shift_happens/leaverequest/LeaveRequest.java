package dk.ek.shift_happens.leaverequest;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "leaverequest")
public class LeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_request_id")
    private Integer leave_request_id;

    @Column(name = "employee_id", nullable = false)
    private Integer employee_id;

    @Column(name = "leave_type_id", nullable = false)
    private Integer leave_type_id;

    @Column(name = "start_date")
    private LocalDate start_date;

    @Column(name = "end_date")
    private LocalDate end_date;

    @Column(name = "request_status")
    private String request_status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "requested_datetime")
    private LocalDateTime requested_datetime;
}

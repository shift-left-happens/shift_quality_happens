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
@Table(name = "leave_request")
public class LeaveRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_request_id")
    private Integer leaveRequestId;

    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    @Column(name = "leave_type_id", nullable = false)
    private Integer leaveTypeId;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "request_status")
    private String requestStatus;

    @Column(name = "reason")
    private String reason;

    @Column(name = "requested_datetime")
    private LocalDateTime requestedDatetime;
}

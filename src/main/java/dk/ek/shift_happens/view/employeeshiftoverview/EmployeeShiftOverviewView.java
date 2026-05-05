package dk.ek.shift_happens.view.employeeshiftoverview;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "vw_employee_shift_overview")
public class EmployeeShiftOverviewView {

    @Id
    @Column(name = "shift_assignment_id")
    private Integer shiftAssignmentId;

    @Column(name = "employee_id")
    private Integer employeeId;

    @Column(name = "employee_number")
    private String employeeNumber;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "shift_id")
    private Integer shiftId;

    @Column(name = "shift_name")
    private String shiftName;

    @Column(name = "start_datetime")
    private LocalDateTime startDatetime;

    @Column(name = "end_datetime")
    private LocalDateTime endDatetime;

    @Column(name = "shift_status")
    private String shiftStatus;

    @Column(name = "department_name")
    private String departmentName;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "assignment_status")
    private String assignmentStatus;

    @Column(name = "assigned_datetime")
    private LocalDateTime assignedDatetime;

    @Column(name = "check_in_datetime")
    private LocalDateTime checkInDatetime;

    @Column(name = "check_out_datetime")
    private LocalDateTime checkOutDatetime;
}

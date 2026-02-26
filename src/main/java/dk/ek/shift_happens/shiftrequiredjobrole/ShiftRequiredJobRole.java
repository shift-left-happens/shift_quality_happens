package dk.ek.shift_happens.shiftrequiredjobrole;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "shift_required_job_role")
public class ShiftRequiredJobRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_required_job_role_id")
    private Integer shiftRequiredJobRoleId;

    @Column(name = "shift_id", nullable = false)
    private Integer shiftId;

    @Column(name = "job_role_id", nullable = false)
    private Integer jobRoleId;

    @Column(name = "required_employee_count")
    private Integer requiredEmployeeCount;
}

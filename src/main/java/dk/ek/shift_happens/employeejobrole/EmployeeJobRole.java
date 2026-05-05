package dk.ek.shift_happens.employeejobrole;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "employee_job_role")
public class EmployeeJobRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_job_role_id")
    private Integer employeeJobRoleId;

    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    @Column(name = "job_role_id", nullable = false)
    private Integer jobRoleId;

    @Column(name = "assigned_date")
    private LocalDate assignedDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "proficiency_level")
    private String proficiencyLevel;
}

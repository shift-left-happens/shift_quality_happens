package dk.ek.shift_happens.employeejobrole;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "employeejobrole")
@IdClass(EmployeeJobRoleId.class)
public class EmployeeJobRole {
    @Id
    @Column(name = "employee_id")
    private Integer employee_id;

    @Id
    @Column(name = "job_role_id")
    private Integer job_role_id;

    @Column(name = "assigned_date")
    private LocalDate assigned_date;

    @Column(name = "expiry_date")
    private LocalDate expiry_date;

    @Column(name = "proficiency_level")
    private String proficiency_level;
}

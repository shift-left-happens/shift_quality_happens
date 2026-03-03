package dk.ek.shift_happens.employeecontract;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "employee_contract")
public class EmployeeContract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Integer contractId;

    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    @Column(name = "department_id", nullable = false)
    private Integer departmentId;

    @Column(name = "contract_type")
    private String contractType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "weekly_hours")
    private Integer weeklyHours;

    @Column(name = "salary_amount")
    private BigDecimal salaryAmount;

    @Column(name = "is_active")
    private Boolean isActive;
}

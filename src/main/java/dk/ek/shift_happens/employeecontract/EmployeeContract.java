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
    private Integer contract_id;

    @Column(name = "employee_id", nullable = false)
    private Integer employee_id;

    @Column(name = "department_id", nullable = false)
    private Integer department_id;

    @Column(name = "contract_type")
    private String contract_type;

    @Column(name = "start_date")
    private LocalDate start_date;

    @Column(name = "end_date")
    private LocalDate end_date;

    @Column(name = "weekly_hours")
    private Integer weekly_hours;

    @Column(name = "salary_amount")
    private BigDecimal salary_amount;

    @Column(name = "is_active")
    private Boolean is_active;
}

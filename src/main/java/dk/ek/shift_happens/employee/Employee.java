package dk.ek.shift_happens.employee;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "employee")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Integer employee_id;

    @Column(name = "employee_number", unique = true)
    private String employee_number;

    @Column(name = "first_name")
    private String first_name;

    @Column(name = "last_name")
    private String last_name;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "login_password", nullable = false)
    private String login_password;

    @Column(name = "fk_user_role_id", nullable = false)
    private Integer fk_user_role_id;

    @Column(name = "phone_number")
    private String phone_number;

    @Column(name = "hire_date")
    private LocalDate hire_date;

    @Column(name = "employment_status")
    private String employment_status;

    @Column(name = "primary_work_location_id")
    private Integer primary_work_location_id;
}

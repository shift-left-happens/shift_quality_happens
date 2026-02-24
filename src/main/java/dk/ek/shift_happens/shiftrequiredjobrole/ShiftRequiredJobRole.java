package dk.ek.shift_happens.shiftrequiredjobrole;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "shiftrequiredjobrole")
@IdClass(ShiftRequiredJobRoleId.class)
public class ShiftRequiredJobRole {
    @Id
    @Column(name = "shift_id")
    private Integer shift_id;

    @Id
    @Column(name = "job_role_id")
    private Integer job_role_id;

    @Column(name = "required_employee_count")
    private Integer required_employee_count;
}

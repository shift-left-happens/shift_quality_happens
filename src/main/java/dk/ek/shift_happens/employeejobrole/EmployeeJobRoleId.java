package dk.ek.shift_happens.employeejobrole;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

// Composite key class for the employeejobrole junction table.
// JPA requires a separate class when a table has a composite primary key (employee_id + job_role_id).
// Must implement Serializable and override equals() and hashCode() so JPA can compare keys correctly.
@Getter
@Setter
@NoArgsConstructor
public class EmployeeJobRoleId implements Serializable {
    private Integer employee_id;
    private Integer job_role_id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmployeeJobRoleId that = (EmployeeJobRoleId) o;
        return Objects.equals(employee_id, that.employee_id) && Objects.equals(job_role_id, that.job_role_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(employee_id, job_role_id);
    }
}

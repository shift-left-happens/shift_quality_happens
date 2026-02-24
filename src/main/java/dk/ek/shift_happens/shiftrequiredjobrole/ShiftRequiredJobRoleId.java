package dk.ek.shift_happens.shiftrequiredjobrole;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

// Composite key class for the shiftrequiredjobrole junction table.
// JPA requires a separate class when a table has a composite primary key (shift_id + job_role_id).
// Must implement Serializable and override equals() and hashCode() so JPA can compare keys correctly.
@Getter
@Setter
@NoArgsConstructor
public class ShiftRequiredJobRoleId implements Serializable {
    private Integer shift_id;
    private Integer job_role_id;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShiftRequiredJobRoleId that = (ShiftRequiredJobRoleId) o;
        return Objects.equals(shift_id, that.shift_id) && Objects.equals(job_role_id, that.job_role_id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shift_id, job_role_id);
    }
}

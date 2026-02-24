package dk.ek.shift_happens.leavetype;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "leavetype")
public class LeaveType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_type_id")
    private Integer leave_type_id;

    @Column(name = "leave_type_name")
    private String leave_type_name;

    @Column(name = "leave_type_description")
    private String leave_type_description;

    @Column(name = "requires_approval")
    private Boolean requires_approval;

    @Column(name = "is_paid_leave")
    private Boolean is_paid_leave;
}

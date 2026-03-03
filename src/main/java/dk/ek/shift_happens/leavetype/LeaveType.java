package dk.ek.shift_happens.leavetype;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "leave_type")
public class LeaveType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_type_id")
    private Integer leaveTypeId;

    @Column(name = "leave_type_name")
    private String leaveTypeName;

    @Column(name = "leave_type_description")
    private String leaveTypeDescription;

    @Column(name = "requires_approval")
    private Boolean requiresApproval;

    @Column(name = "is_paid_leave")
    private Boolean isPaidLeave;
}

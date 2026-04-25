package dk.ek.shift_happens.leavetype.neo4j;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("LeaveType")
@Getter
@Setter
@NoArgsConstructor
public class LeaveTypeNode {

    @Id
    @GeneratedValue
    private Long id;

    private Integer leaveTypeId;
    private String leaveTypeName;
    private String leaveTypeDescription;
    private Boolean requiresApproval;
    private Boolean isPaidLeave;
}

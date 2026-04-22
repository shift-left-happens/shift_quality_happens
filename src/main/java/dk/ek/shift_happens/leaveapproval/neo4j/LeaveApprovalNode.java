package dk.ek.shift_happens.leaveapproval.neo4j;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDateTime;

@Node("LeaveApproval")
@Getter
@Setter
@NoArgsConstructor
public class LeaveApprovalNode {

    @Id
    @GeneratedValue
    private Long id;

    private Integer leaveApprovalId;
    private Integer leaveRequestId;
    private Integer approverEmployeeId;
    private String decision;
    private String leaveComment;
    private LocalDateTime decisionDatetime;
}

package dk.ek.shift_happens.leaverequest.neo4j;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Node("LeaveRequest")
@Getter
@Setter
@NoArgsConstructor
public class LeaveRequestNode {

    @Id
    @GeneratedValue
    private Long id;

    private Integer leaveRequestId;
    private Integer employeeId;
    private Integer leaveTypeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String requestStatus;
    private String reason;
    private LocalDateTime requestedDatetime;
}

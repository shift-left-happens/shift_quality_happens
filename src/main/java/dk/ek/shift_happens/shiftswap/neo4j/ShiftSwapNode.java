package dk.ek.shift_happens.shiftswap.neo4j;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDateTime;

@Node("ShiftSwap")
@Getter
@Setter
@NoArgsConstructor
public class ShiftSwapNode {

    @Id
    @GeneratedValue
    private Long id;

    private Integer shiftSwapId;
    private Integer originalShiftAssignmentId;
    private Integer employeeFromId;
    private Integer employeeToId;
    private String swapStatus;
    private LocalDateTime requestDatetime;
    private String reason;
}

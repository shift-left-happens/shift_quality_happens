package dk.ek.shift_happens.shift.neo4j;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDateTime;

@Node("Shift")
@Getter
@Setter
@NoArgsConstructor
public class ShiftNode {

    @Id
    @GeneratedValue
    private Long id;

    private Integer shiftId;
    private String shiftName;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String shiftStatus;
}

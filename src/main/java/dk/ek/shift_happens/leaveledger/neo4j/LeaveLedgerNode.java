package dk.ek.shift_happens.leaveledger.neo4j;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Node("LeaveLedger")
@Getter
@Setter
@NoArgsConstructor
public class LeaveLedgerNode {

    @Id
    @GeneratedValue
    private Long id;

    private Integer leaveLedgerId;
    private Integer employeeId;
    private Integer leaveTypeId;
    private BigDecimal changeAmountDays;
    private String transactionType;
    private String referenceEntityType;
    private Integer referenceEntityId;
    private LocalDateTime transactionDatetime;
}

package dk.ek.shift_happens.leaveledger;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "leave_ledger")
public class LeaveLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_ledger_id")
    private Integer leaveLedgerId;

    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    @Column(name = "leave_type_id", nullable = false)
    private Integer leaveTypeId;

    @Column(name = "change_amount_days")
    private BigDecimal changeAmountDays;

    @Column(name = "transaction_type")
    private String transactionType;

    @Column(name = "reference_entity_type")
    private String referenceEntityType;

    @Column(name = "reference_entity_id")
    private Integer referenceEntityId;

    @Column(name = "transaction_datetime")
    private LocalDateTime transactionDatetime;
}

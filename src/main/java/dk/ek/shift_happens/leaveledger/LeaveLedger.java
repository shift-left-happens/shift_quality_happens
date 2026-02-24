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
@Table(name = "leaveledger")
public class LeaveLedger {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_ledger_id")
    private Integer leave_ledger_id;

    @Column(name = "employee_id", nullable = false)
    private Integer employee_id;

    @Column(name = "leave_type_id", nullable = false)
    private Integer leave_type_id;

    @Column(name = "change_amount_days")
    private BigDecimal change_amount_days;

    @Column(name = "transaction_type")
    private String transaction_type;

    @Column(name = "reference_entity_type")
    private String reference_entity_type;

    @Column(name = "reference_entity_id")
    private Integer reference_entity_id;

    @Column(name = "transaction_datetime")
    private LocalDateTime transaction_datetime;
}

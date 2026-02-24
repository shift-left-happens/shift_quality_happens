package dk.ek.shift_happens.auditlog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "auditlog")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Integer audit_log_id;

    @Column(name = "entity_type")
    private String entity_type;

    @Column(name = "entity_id")
    private Integer entity_id;

    @Column(name = "action_type")
    private String action_type;

    @Column(name = "performed_by_employee_id")
    private Integer performed_by_employee_id;

    @Column(name = "action_datetime")
    private LocalDateTime action_datetime;

    @Column(name = "old_value_snapshot")
    private String old_value_snapshot;

    @Column(name = "new_value_snapshot")
    private String new_value_snapshot;
}

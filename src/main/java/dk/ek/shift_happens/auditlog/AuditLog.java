package dk.ek.shift_happens.auditlog;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "audit_log")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_log_id")
    private Integer auditLogId;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private Integer entityId;

    @Column(name = "action_type")
    private String actionType;

    @Column(name = "db_user")
    private String dbUser;

    @Column(name = "action_datetime")
    private LocalDateTime actionDatetime;

    @Column(name = "old_value_snapshot")
    private String oldValueSnapshot;

    @Column(name = "new_value_snapshot")
    private String newValueSnapshot;
}

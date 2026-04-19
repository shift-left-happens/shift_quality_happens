package dk.ek.shift_happens.auditlog.mongo;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

// MongoDB document for the 'audit_log' collection.
// Kept flat — audit_log are small reference data with no child entities to embed.
@Document(collection = "audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLogDocument {

    @Id
    private String id;

    private String entityType;

    private Integer entityId;

    private String actionType;

    private String dbUser;

    private LocalDateTime actionDatetime;

    private String oldValueSnapshot;

    private String newValueSnapshot;
}

package dk.ek.shift_happens.auditlog.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditLogMongoRepository extends MongoRepository<AuditLogDocument, String> {
}

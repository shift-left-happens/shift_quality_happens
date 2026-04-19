package dk.ek.shift_happens.leavetype.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface LeaveTypeMongoRepository extends MongoRepository<LeaveTypeDocument, String> {
}

package dk.ek.shift_happens.leaverequest.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface LeaveMongoRepository extends MongoRepository<LeaveDocument, String> {
}

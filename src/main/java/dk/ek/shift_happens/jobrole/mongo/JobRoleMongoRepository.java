package dk.ek.shift_happens.jobrole.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface JobRoleMongoRepository extends MongoRepository<JobRoleDocument, String> {
}

package dk.ek.shift_happens.worklocation.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkLocationMongoRepository extends MongoRepository<WorkLocationDocument, String> {
}

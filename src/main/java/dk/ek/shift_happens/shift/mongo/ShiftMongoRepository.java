package dk.ek.shift_happens.shift.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShiftMongoRepository extends MongoRepository<ShiftDocument, String> {
}

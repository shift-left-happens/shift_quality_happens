package dk.ek.shift_happens.userrole.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleMongoRepository extends MongoRepository<UserRoleDocument, String> {
}

package dk.ek.shift_happens.department.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface DepartmentMongoRepository extends MongoRepository<DepartmentDocument, String> {
}

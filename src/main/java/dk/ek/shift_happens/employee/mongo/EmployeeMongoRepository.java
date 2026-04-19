package dk.ek.shift_happens.employee.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmployeeMongoRepository extends MongoRepository<EmployeeDocument, Integer> {
}

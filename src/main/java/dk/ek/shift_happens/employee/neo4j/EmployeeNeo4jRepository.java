package dk.ek.shift_happens.employee.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface EmployeeNeo4jRepository extends Neo4jRepository<EmployeeNode, Long> {
}

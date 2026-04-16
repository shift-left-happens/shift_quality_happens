package dk.ek.shift_happens.department.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface DepartmentNeo4jRepository extends Neo4jRepository<DepartmentNode, Long> {
}

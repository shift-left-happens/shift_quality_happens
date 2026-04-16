package dk.ek.shift_happens.shift.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ShiftNeo4jRepository extends Neo4jRepository<ShiftNode, Long> {
}

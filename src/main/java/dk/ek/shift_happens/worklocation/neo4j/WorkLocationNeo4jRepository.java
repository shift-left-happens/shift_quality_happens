package dk.ek.shift_happens.worklocation.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface WorkLocationNeo4jRepository extends Neo4jRepository<WorkLocationNode, Long> {
}

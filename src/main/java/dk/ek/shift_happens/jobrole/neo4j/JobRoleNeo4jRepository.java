package dk.ek.shift_happens.jobrole.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface JobRoleNeo4jRepository extends Neo4jRepository<JobRoleNode, Long> {
}

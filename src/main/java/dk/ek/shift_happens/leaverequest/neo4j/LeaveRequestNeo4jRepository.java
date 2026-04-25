package dk.ek.shift_happens.leaverequest.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface LeaveRequestNeo4jRepository extends Neo4jRepository<LeaveRequestNode, Long> {
}

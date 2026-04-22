package dk.ek.shift_happens.leavetype.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface LeaveTypeNeo4jRepository extends Neo4jRepository<LeaveTypeNode, Long> {
}

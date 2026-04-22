package dk.ek.shift_happens.leaveapproval.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface LeaveApprovalNeo4jRepository extends Neo4jRepository<LeaveApprovalNode, Long> {
}

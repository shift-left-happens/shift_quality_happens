package dk.ek.shift_happens.leaveledger.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface LeaveLedgerNeo4jRepository extends Neo4jRepository<LeaveLedgerNode, Long> {
}

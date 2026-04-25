package dk.ek.shift_happens.shiftswap.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ShiftSwapNeo4jRepository extends Neo4jRepository<ShiftSwapNode, Long> {
}

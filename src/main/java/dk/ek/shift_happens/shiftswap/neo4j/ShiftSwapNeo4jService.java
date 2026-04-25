package dk.ek.shift_happens.shiftswap.neo4j;

import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShiftSwapNeo4jService {

    private final Neo4jClient neo4jClient;

    /**
     * Executes a shift swap inside a single Neo4j transaction:
     *  1. Validates the ShiftSwap node exists and has status 'pending'.
     *  2. Deletes the ASSIGNED_TO_SHIFT relationship from employeeFrom → shift.
     *  3. Creates a new ASSIGNED_TO_SHIFT relationship from employeeTo → shift.
     *  4. Sets the ShiftSwap node's swapStatus to 'completed'.
     *
     * If any part fails the entire transaction is rolled back.
     *
     * @param shiftSwapId the MySQL-mirrored shiftSwapId on the ShiftSwap node
     * @param shiftId     the shiftId on the target Shift node
     */
    @Transactional
    public Map<String, Object> executeSwap(Integer shiftSwapId, Integer shiftId) {
        String cypher = """
                MATCH (swap:ShiftSwap {shiftSwapId: $swapId})
                WHERE swap.swapStatus = 'pending'
                MATCH (fromEmp:Employee {employeeId: swap.employeeFromId})-[r:ASSIGNED_TO_SHIFT]->(shift:Shift {shiftId: $shiftId})
                MATCH (toEmp:Employee {employeeId: swap.employeeToId})
                DELETE r
                CREATE (toEmp)-[:ASSIGNED_TO_SHIFT]->(shift)
                SET swap.swapStatus = 'completed'
                RETURN swap.shiftSwapId AS shiftSwapId,
                       swap.employeeFromId AS employeeFromId,
                       swap.employeeToId AS employeeToId,
                       swap.swapStatus AS swapStatus,
                       shift.shiftId AS shiftId,
                       shift.shiftName AS shiftName
                """;

        Optional<Map<String, Object>> result = neo4jClient.query(cypher)
                .bind(shiftSwapId).to("swapId")
                .bind(shiftId).to("shiftId")
                .fetch()
                .one();

        return result.orElseThrow(() -> new IllegalStateException(
                "Swap could not be executed. Check that shiftSwapId=" + shiftSwapId
                + " exists, has status 'pending', and shiftId=" + shiftId + " is assigned to employeeFrom."));
    }
}

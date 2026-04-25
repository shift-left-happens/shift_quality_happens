package dk.ek.shift_happens.migration;

import java.util.List;

/**
 * Shared result summary for MongoDB and Neo4j migration runs.
 */
public record MigrationResult(
        int employees,
        int shifts,
        int departments,
        int leaveDocuments,
        int neo4jEmployees,
        int neo4jDepartments,
        int neo4jWorkLocations,
        int neo4jShifts,
        int neo4jJobRoles,
        int neo4jShiftSwaps,
        int neo4jShiftAssignments,
        int neo4jLeaveTypes,
        int neo4jLeaveRequests,
        int neo4jLeaveApprovals,
        int neo4jShiftApprovals,
        int neo4jShiftSwapApprovals,
        int neo4jLeaveLedgers,
        int neo4jEmployeeContracts,
        List<String> errors
) {
}

package dk.ek.shift_happens.shiftswap.neo4j;

import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/neo4j/shiftswaps")
@RequiredArgsConstructor
public class ShiftSwapNeo4jController {

    private final ShiftSwapNeo4jRepository shiftSwapNeo4jRepository;
    private final Neo4jClient neo4jClient;

    @GetMapping
    public List<ShiftSwapNode> getAll() {
        return shiftSwapNeo4jRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShiftSwapNode> getById(@PathVariable Long id) {
        return shiftSwapNeo4jRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/candidates/{shiftId}")
    public ResponseEntity<List<Map<String, Object>>> getSwapCandidates(@PathVariable Integer shiftId) {
        String cypher = """
                MATCH (target:Shift {shiftId: $shiftId})-[:SHIFT_IN_DEPT]->(targetDepartment:Department)
                MATCH (target)-[:REQUIRES_ROLE]->(requiredRole:JobRole)
                MATCH (candidate:Employee)-[:HAS_JOB_ROLE]->(requiredRole)
                MATCH (candidate)-[:WORKS_IN_DEPT]->(targetDepartment)
                WHERE coalesce(candidate.employmentStatus, '') = 'ACTIVE'
                  AND NOT EXISTS {
                      MATCH (candidate)-[:ASSIGNED_TO_SHIFT]->(target)
                  }
                  AND NOT EXISTS {
                      MATCH (candidate)-[:ASSIGNED_TO_SHIFT]->(other:Shift)
                      WHERE other.startDatetime < target.endDatetime
                        AND other.endDatetime > target.startDatetime
                  }
                OPTIONAL MATCH (candidate)-[:WORKS_AT_LOCATION]->(location:WorkLocation)
                  WITH target, targetDepartment, candidate, location,
                     collect(DISTINCT requiredRole.roleName) AS matchingRoles
                RETURN target.shiftId AS shiftId,
                       target.shiftName AS shiftName,
                       candidate.employeeId AS employeeId,
                       candidate.employeeNumber AS employeeNumber,
                       candidate.firstName AS firstName,
                       candidate.lastName AS lastName,
                       candidate.email AS email,
                      targetDepartment.departmentName AS department,
                       location.locationName AS location,
                       matchingRoles
                ORDER BY candidate.firstName, candidate.lastName
                """;

        return ResponseEntity.ok(runCandidateQuery(cypher, shiftId));
    }

    //kræver lige nu auth adgang, jeg tro fordi den bruger nået employe data som kun må tilgås som manager.
    @GetMapping("/candidates/location/{shiftId}")
    public ResponseEntity<List<Map<String, Object>>> getSwapCandidatesSameDepartmentAndLocation(@PathVariable Integer shiftId) {
        String cypher = """
                MATCH (target:Shift {shiftId: $shiftId})-[:SHIFT_IN_DEPT]->(targetDepartment:Department)
                MATCH (target)-[:SHIFT_AT_LOCATION]->(targetLocation:WorkLocation)
                MATCH (target)-[:REQUIRES_ROLE]->(requiredRole:JobRole)
                MATCH (candidate:Employee)-[:HAS_JOB_ROLE]->(requiredRole)
                MATCH (candidate)-[:WORKS_IN_DEPT]->(targetDepartment)
                MATCH (candidate)-[:WORKS_AT_LOCATION]->(targetLocation)
                WHERE coalesce(candidate.employmentStatus, '') = 'ACTIVE'
                  AND NOT EXISTS {
                      MATCH (candidate)-[:ASSIGNED_TO_SHIFT]->(target)
                  }
                  AND NOT EXISTS {
                      MATCH (candidate)-[:ASSIGNED_TO_SHIFT]->(other:Shift)
                      WHERE other.startDatetime < target.endDatetime
                        AND other.endDatetime > target.startDatetime
                  }
                WITH target, targetDepartment, targetLocation, candidate,
                     collect(DISTINCT requiredRole.roleName) AS matchingRoles
                RETURN target.shiftId AS shiftId,
                       target.shiftName AS shiftName,
                       candidate.employeeId AS employeeId,
                       candidate.employeeNumber AS employeeNumber,
                       candidate.firstName AS firstName,
                       candidate.lastName AS lastName,
                       candidate.email AS email,
                       targetDepartment.departmentName AS department,
                       targetLocation.locationName AS location,
                       matchingRoles
                ORDER BY candidate.firstName, candidate.lastName
                """;

        return ResponseEntity.ok(runCandidateQuery(cypher, shiftId));
    }

    private List<Map<String, Object>> runCandidateQuery(String cypher, Integer shiftId) {
        Collection<Map<String, Object>> rows = neo4jClient.query(cypher)
                .bind(shiftId).to("shiftId")
                .fetch()
                .all();

        return new ArrayList<>(rows);
    }
}

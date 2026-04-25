package dk.ek.shift_happens.graph;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/neo4j/insights")
public class Neo4jInsightsController {

    private final Neo4jClient neo4jClient;

    public Neo4jInsightsController(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    /**
     * GET /neo4j/insights/shift-roster
     * Shows all shifts with department, location and assigned employees (with job role).
     */
    @GetMapping("/shift-roster")
    public ResponseEntity<List<Map<String, Object>>> shiftRoster() {
        String cypher = """
                MATCH (e:Employee)-[a:ASSIGNED_TO_SHIFT]->(s:Shift)
                OPTIONAL MATCH (s)-[:SHIFT_IN_DEPT]->(d:Department)
                OPTIONAL MATCH (s)-[:SHIFT_AT_LOCATION]->(w:WorkLocation)
                OPTIONAL MATCH (e)-[:HAS_JOB_ROLE]->(j:JobRole)
                WITH s, d, w,
                     collect({
                         employeeId:       e.employeeId,
                         name:             e.firstName + ' ' + e.lastName,
                         jobRole:          j.roleName,
                         assignmentStatus: a.assignmentStatus
                     }) AS employees
                RETURN s.shiftId        AS shiftId,
                       s.shiftName      AS shiftName,
                       s.startDatetime  AS startDatetime,
                       s.endDatetime    AS endDatetime,
                       s.shiftStatus    AS shiftStatus,
                       d.departmentName AS department,
                       w.locationName   AS location,
                       employees
                ORDER BY s.startDatetime
                """;

        Collection<Map<String, Object>> rows = neo4jClient.query(cypher).fetch().all();
        return ResponseEntity.ok(new ArrayList<>(rows));
    }

    /**
     * GET /neo4j/insights/employee/{employeeId}
     * Full employee profile: department, location, job role and all assigned shifts.
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Map<String, Object>> employeeProfile(@PathVariable int employeeId) {
        String cypher = """
                MATCH (e:Employee {employeeId: $employeeId})
                OPTIONAL MATCH (e)-[:WORKS_IN_DEPT]->(d:Department)
                OPTIONAL MATCH (e)-[:WORKS_AT_LOCATION]->(w:WorkLocation)
                OPTIONAL MATCH (e)-[:HAS_JOB_ROLE]->(j:JobRole)
                OPTIONAL MATCH (e)-[a:ASSIGNED_TO_SHIFT]->(s:Shift)
                WITH e, d, w, j,
                     collect({
                         shiftId:          s.shiftId,
                         shiftName:        s.shiftName,
                         startDatetime:    s.startDatetime,
                         endDatetime:      s.endDatetime,
                         shiftStatus:      s.shiftStatus,
                         assignmentStatus: a.assignmentStatus
                     }) AS shifts
                RETURN e.employeeId       AS employeeId,
                       e.employeeNumber   AS employeeNumber,
                       e.firstName        AS firstName,
                       e.lastName         AS lastName,
                       e.email            AS email,
                       e.employmentStatus AS employmentStatus,
                       e.hireDate         AS hireDate,
                       d.departmentName   AS department,
                       w.locationName     AS location,
                       j.roleName         AS jobRole,
                       shifts
                """;

        Optional<Map<String, Object>> result = neo4jClient.query(cypher)
                .bind(employeeId).to("employeeId")
                .fetch()
                .one();

        return result.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /neo4j/insights/graph-summary
     * Node and relationship counts for a quick graph overview.
     */
    @GetMapping("/graph-summary")
    public ResponseEntity<Map<String, Object>> graphSummary() {
        String nodeCypher = """
                MATCH (n)
                RETURN labels(n)[0] AS label, count(n) AS count
                ORDER BY count DESC
                """;

        String relCypher = """
                MATCH ()-[r]->()
                RETURN type(r) AS type, count(r) AS count
                ORDER BY count DESC
                """;

        Collection<Map<String, Object>> nodes = neo4jClient.query(nodeCypher).fetch().all();
        Collection<Map<String, Object>> rels  = neo4jClient.query(relCypher).fetch().all();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("nodes",         new ArrayList<>(nodes));
        summary.put("relationships", new ArrayList<>(rels));

        return ResponseEntity.ok(summary);
    }
}

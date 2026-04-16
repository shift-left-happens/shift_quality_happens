package dk.ek.shift_happens.jobrole.neo4j;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/neo4j/jobroles")
@RequiredArgsConstructor
public class JobRoleNeo4jController {

    private final JobRoleNeo4jRepository jobRoleNeo4jRepository;

    @GetMapping
    public List<JobRoleNode> getAll() {
        return jobRoleNeo4jRepository.findAll();
    }

    @GetMapping("/{id}")
    public JobRoleNode getById(@PathVariable Long id) {
        return jobRoleNeo4jRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

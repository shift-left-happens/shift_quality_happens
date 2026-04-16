package dk.ek.shift_happens.worklocation.neo4j;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/neo4j/worklocations")
@RequiredArgsConstructor
public class WorkLocationNeo4jController {

    private final WorkLocationNeo4jRepository workLocationNeo4jRepository;

    @GetMapping
    public List<WorkLocationNode> getAll() {
        return workLocationNeo4jRepository.findAll();
    }

    @GetMapping("/{id}")
    public WorkLocationNode getById(@PathVariable Long id) {
        return workLocationNeo4jRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

package dk.ek.shift_happens.department.neo4j;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/neo4j/departments")
@RequiredArgsConstructor
public class DepartmentNeo4jController {

    private final DepartmentNeo4jRepository departmentNeo4jRepository;

    @GetMapping
    public List<DepartmentNode> getAll() {
        return departmentNeo4jRepository.findAll();
    }

    @GetMapping("/{id}")
    public DepartmentNode getById(@PathVariable Long id) {
        return departmentNeo4jRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

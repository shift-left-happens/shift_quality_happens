package dk.ek.shift_happens.leavetype.neo4j;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/neo4j/leave-types")
@RequiredArgsConstructor
public class LeaveTypeNeo4jController {

    private final LeaveTypeNeo4jRepository leaveTypeNeo4jRepository;

    @GetMapping
    public List<LeaveTypeNode> getAll() {
        return leaveTypeNeo4jRepository.findAll();
    }

    @GetMapping("/{id}")
    public LeaveTypeNode getById(@PathVariable Long id) {
        return leaveTypeNeo4jRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveTypeNode create(@RequestBody LeaveTypeNode node) {
        return leaveTypeNeo4jRepository.save(node);
    }

    @PutMapping("/{id}")
    public LeaveTypeNode update(@PathVariable Long id, @RequestBody LeaveTypeNode node) {
        if (!leaveTypeNeo4jRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        node.setId(id);
        return leaveTypeNeo4jRepository.save(node);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!leaveTypeNeo4jRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        leaveTypeNeo4jRepository.deleteById(id);
    }
}

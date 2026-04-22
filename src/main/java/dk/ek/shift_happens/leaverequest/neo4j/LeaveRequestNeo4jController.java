package dk.ek.shift_happens.leaverequest.neo4j;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/neo4j/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestNeo4jController {

    private final LeaveRequestNeo4jRepository leaveRequestNeo4jRepository;

    @GetMapping
    public List<LeaveRequestNode> getAll() {
        return leaveRequestNeo4jRepository.findAll();
    }

    @GetMapping("/{id}")
    public LeaveRequestNode getById(@PathVariable Long id) {
        return leaveRequestNeo4jRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveRequestNode create(@RequestBody LeaveRequestNode node) {
        return leaveRequestNeo4jRepository.save(node);
    }

    @PutMapping("/{id}")
    public LeaveRequestNode update(@PathVariable Long id, @RequestBody LeaveRequestNode node) {
        if (!leaveRequestNeo4jRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        node.setId(id);
        return leaveRequestNeo4jRepository.save(node);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!leaveRequestNeo4jRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        leaveRequestNeo4jRepository.deleteById(id);
    }
}

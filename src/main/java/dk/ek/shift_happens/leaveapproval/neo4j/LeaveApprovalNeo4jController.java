package dk.ek.shift_happens.leaveapproval.neo4j;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/neo4j/leave-approvals")
@RequiredArgsConstructor
public class LeaveApprovalNeo4jController {

    private final LeaveApprovalNeo4jRepository leaveApprovalNeo4jRepository;

    @GetMapping
    public List<LeaveApprovalNode> getAll() {
        return leaveApprovalNeo4jRepository.findAll();
    }

    @GetMapping("/{id}")
    public LeaveApprovalNode getById(@PathVariable Long id) {
        return leaveApprovalNeo4jRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveApprovalNode create(@RequestBody LeaveApprovalNode node) {
        return leaveApprovalNeo4jRepository.save(node);
    }

    @PutMapping("/{id}")
    public LeaveApprovalNode update(@PathVariable Long id, @RequestBody LeaveApprovalNode node) {
        if (!leaveApprovalNeo4jRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        node.setId(id);
        return leaveApprovalNeo4jRepository.save(node);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!leaveApprovalNeo4jRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        leaveApprovalNeo4jRepository.deleteById(id);
    }
}

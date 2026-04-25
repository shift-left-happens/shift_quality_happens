package dk.ek.shift_happens.leaveledger.neo4j;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/neo4j/leave-ledgers")
@RequiredArgsConstructor
public class LeaveLedgerNeo4jController {

    private final LeaveLedgerNeo4jRepository leaveLedgerNeo4jRepository;

    @GetMapping
    public List<LeaveLedgerNode> getAll() {
        return leaveLedgerNeo4jRepository.findAll();
    }

    @GetMapping("/{id}")
    public LeaveLedgerNode getById(@PathVariable Long id) {
        return leaveLedgerNeo4jRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveLedgerNode create(@RequestBody LeaveLedgerNode node) {
        return leaveLedgerNeo4jRepository.save(node);
    }

    @PutMapping("/{id}")
    public LeaveLedgerNode update(@PathVariable Long id, @RequestBody LeaveLedgerNode node) {
        if (!leaveLedgerNeo4jRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        node.setId(id);
        return leaveLedgerNeo4jRepository.save(node);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!leaveLedgerNeo4jRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        leaveLedgerNeo4jRepository.deleteById(id);
    }
}

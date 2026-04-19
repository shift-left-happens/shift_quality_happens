package dk.ek.shift_happens.auditlog.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/mongo/audit_log")
@RequiredArgsConstructor
public class AuditLogMongoController {

    private final AuditLogMongoRepository auditLogMongoRepository;

    @GetMapping
    public List<AuditLogDocument> getAll() {
        return auditLogMongoRepository.findAll();
    }

    @GetMapping("/{id}")
    public AuditLogDocument getById(@PathVariable String id) {
        return auditLogMongoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public AuditLogDocument create(@RequestBody AuditLogDocument auditLog) {
        return auditLogMongoRepository.save(auditLog);
    }

    @PutMapping("/{id}")
    public AuditLogDocument update(@PathVariable String id, @RequestBody AuditLogDocument auditLog) {
        if (!auditLogMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        auditLog.setId(id);
        return auditLogMongoRepository.save(auditLog);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        if (!auditLogMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        auditLogMongoRepository.deleteById(id);
    }
}

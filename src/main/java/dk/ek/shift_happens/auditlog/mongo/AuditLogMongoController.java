package dk.ek.shift_happens.auditlog.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}

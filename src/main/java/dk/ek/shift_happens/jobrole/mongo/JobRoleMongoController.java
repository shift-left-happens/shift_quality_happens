package dk.ek.shift_happens.jobrole.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/mongo/job_role")
@RequiredArgsConstructor
public class JobRoleMongoController {

    private final JobRoleMongoRepository jobRoleMongoRepository;

    @GetMapping
    public List<JobRoleDocument> getAll() {
        return jobRoleMongoRepository.findAll();
    }

    @GetMapping("/{id}")
    public JobRoleDocument getById(@PathVariable String id) {
        return jobRoleMongoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

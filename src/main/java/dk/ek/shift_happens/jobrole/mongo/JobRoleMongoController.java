package dk.ek.shift_happens.jobrole.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping
    public JobRoleDocument create(@RequestBody JobRoleDocument jobRole) {
        return jobRoleMongoRepository.save(jobRole);
    }

    @PutMapping("/{id}")
    public JobRoleDocument update(@PathVariable String id, @RequestBody JobRoleDocument jobRole) {
        if (!jobRoleMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        jobRole.setId(id);
        return jobRoleMongoRepository.save(jobRole);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        if (!jobRoleMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        jobRoleMongoRepository.deleteById(id);
    }
}

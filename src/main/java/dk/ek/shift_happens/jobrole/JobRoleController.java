package dk.ek.shift_happens.jobrole;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/jobroles")
@RequiredArgsConstructor
public class JobRoleController {

    private final JobRoleRepository jobRoleRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<JobRole> getAll() {
        return jobRoleRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public JobRole getById(@PathVariable Integer id) {
        return jobRoleRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public JobRole create(@RequestBody JobRole jobRole) {
        return jobRoleRepository.save(jobRole);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public JobRole update(@PathVariable Integer id, @RequestBody JobRole details) {
        JobRole existing =
                jobRoleRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        existing.setRoleName(details.getRoleName());
        existing.setJobRoleDescription(details.getJobRoleDescription());
        existing.setIsCertificationRequired(details.getIsCertificationRequired());
        return jobRoleRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        jobRoleRepository.deleteById(id);
    }
}

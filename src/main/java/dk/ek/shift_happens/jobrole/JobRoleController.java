package dk.ek.shift_happens.jobrole;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/jobroles")
@RequiredArgsConstructor
public class JobRoleController {

    private final JobRoleRepository jobRoleRepository;

    @GetMapping
    public List<JobRole> getJobRoles() {
        return this.jobRoleRepository.findAll();
    }
}

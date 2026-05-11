package dk.ek.shift_happens.jobrole;

import dk.ek.shift_happens.shiftrequiredjobrole.ShiftRequiredJobRoleRepository;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class JobRoleService {

    public static final int ROLE_NAME_MIN_LENGTH = 2;
    public static final int ROLE_NAME_MAX_LENGTH = 50;
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    private static final Pattern ROLE_NAME_PATTERN = Pattern.compile("^[A-Za-zÆØÅæøåÄÖÜäöü' -]+$");

    private final JobRoleRepository jobRoleRepository;
    private final ShiftRequiredJobRoleRepository shiftRequiredJobRoleRepository;

    public List<JobRole> findAll() {
        return jobRoleRepository.findAll();
    }

    public JobRole findById(Integer id) {
        return jobRoleRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job role not found with id " + id));
    }

    public JobRole create(JobRole jobRole) {
        validate(jobRole, null);
        jobRole.setJobRoleId(null);
        jobRole.setRoleName(jobRole.getRoleName().trim());
        return jobRoleRepository.save(jobRole);
    }

    public JobRole update(Integer id, JobRole updates) {
        JobRole existing = findById(id);

        if (updates.getRoleName() != null) {
            existing.setRoleName(updates.getRoleName().trim());
        }
        if (updates.getJobRoleDescription() != null) {
            existing.setJobRoleDescription(updates.getJobRoleDescription());
        }
        if (updates.getIsCertificationRequired() != null) {
            existing.setIsCertificationRequired(updates.getIsCertificationRequired());
        }

        validate(existing, id);
        return jobRoleRepository.save(existing);
    }

    public void delete(Integer id) {
        JobRole existing = findById(id);

        boolean usedByShift = !shiftRequiredJobRoleRepository.findAll().stream()
                .filter(r -> existing.getJobRoleId().equals(r.getJobRoleId()))
                .toList()
                .isEmpty();
        if (usedByShift) {
            throw new IllegalArgumentException("Job role is referenced by one or more shifts and cannot be deleted");
        }

        jobRoleRepository.delete(existing);
    }

    private void validate(JobRole jobRole, Integer existingId) {
        if (jobRole.getRoleName() == null) {
            throw new IllegalArgumentException("roleName is required");
        }
        String name = jobRole.getRoleName().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("roleName must not be blank");
        }
        if (name.length() < ROLE_NAME_MIN_LENGTH || name.length() > ROLE_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "roleName length must be between " + ROLE_NAME_MIN_LENGTH + " and " + ROLE_NAME_MAX_LENGTH);
        }
        if (!ROLE_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("roleName contains invalid characters");
        }

        Optional<JobRole> duplicate = jobRoleRepository.findByRoleNameIgnoreCase(name);
        if (duplicate.isPresent() && !duplicate.get().getJobRoleId().equals(existingId)) {
            throw new IllegalArgumentException("roleName already exists");
        }

        if (jobRole.getJobRoleDescription() != null
                && jobRole.getJobRoleDescription().length() > DESCRIPTION_MAX_LENGTH) {
            throw new IllegalArgumentException("jobRoleDescription exceeds " + DESCRIPTION_MAX_LENGTH + " characters");
        }
        if (jobRole.getJobRoleDescription() != null && containsUnsafeContent(jobRole.getJobRoleDescription())) {
            throw new IllegalArgumentException("jobRoleDescription contains unsafe content");
        }

        if (jobRole.getIsCertificationRequired() == null) {
            throw new IllegalArgumentException("isCertificationRequired is required");
        }
    }

    private boolean containsUnsafeContent(String value) {
        String lower = value.toLowerCase();
        return lower.contains("<script") || lower.contains("</script") || lower.contains("<") || lower.contains(">");
    }
}

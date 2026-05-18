package dk.ek.shift_happens.shiftrequiredjobrole;

import dk.ek.shift_happens.jobrole.JobRoleRepository;
import dk.ek.shift_happens.shift.ShiftRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ShiftRequiredJobRoleService {

    public static final int MIN_REQUIRED_COUNT = 1;
    public static final int MAX_REQUIRED_COUNT = 20;

    private final ShiftRequiredJobRoleRepository shiftRequiredJobRoleRepository;
    private final ShiftRepository shiftRepository;
    private final JobRoleRepository jobRoleRepository;

    public List<ShiftRequiredJobRole> findAll() {
        return shiftRequiredJobRoleRepository.findAll();
    }

    public ShiftRequiredJobRole findById(Integer id) {
        return shiftRequiredJobRoleRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Required job role not found with id " + id));
    }

    public ShiftRequiredJobRole create(ShiftRequiredJobRole required) {
        validate(required, null);

        Optional<ShiftRequiredJobRole> duplicate = shiftRequiredJobRoleRepository.findByShiftIdAndJobRoleId(
                required.getShiftId(), required.getJobRoleId());
        if (duplicate.isPresent()) {
            throw new IllegalArgumentException(
                    "A required job role already exists for this shift and job role combination");
        }

        required.setShiftRequiredJobRoleId(null);
        return shiftRequiredJobRoleRepository.save(required);
    }

    public ShiftRequiredJobRole update(Integer id, ShiftRequiredJobRole updates) {
        ShiftRequiredJobRole existing = findById(id);

        if (updates.getShiftId() != null) existing.setShiftId(updates.getShiftId());
        if (updates.getJobRoleId() != null) existing.setJobRoleId(updates.getJobRoleId());
        if (updates.getRequiredEmployeeCount() != null) {
            existing.setRequiredEmployeeCount(updates.getRequiredEmployeeCount());
        }

        validate(existing, id);

        Optional<ShiftRequiredJobRole> duplicate = shiftRequiredJobRoleRepository.findByShiftIdAndJobRoleId(
                existing.getShiftId(), existing.getJobRoleId());
        if (duplicate.isPresent()
                && !duplicate.get().getShiftRequiredJobRoleId().equals(id)) {
            throw new IllegalArgumentException(
                    "A required job role already exists for this shift and job role combination");
        }

        return shiftRequiredJobRoleRepository.save(existing);
    }

    public void delete(Integer id) {
        ShiftRequiredJobRole existing = findById(id);
        shiftRequiredJobRoleRepository.delete(existing);
    }

    private void validate(ShiftRequiredJobRole required, Integer existingId) {
        if (required.getShiftId() == null || required.getShiftId() <= 0) {
            throw new IllegalArgumentException("shiftId is required and must be positive");
        }
        if (!shiftRepository.existsById(required.getShiftId())) {
            throw new IllegalArgumentException("shiftId " + required.getShiftId() + " does not exist");
        }

        if (required.getJobRoleId() == null || required.getJobRoleId() <= 0) {
            throw new IllegalArgumentException("jobRoleId is required and must be positive");
        }
        if (!jobRoleRepository.existsById(required.getJobRoleId())) {
            throw new IllegalArgumentException("jobRoleId " + required.getJobRoleId() + " does not exist");
        }

        if (required.getRequiredEmployeeCount() == null) {
            throw new IllegalArgumentException("requiredEmployeeCount is required");
        }
        int count = required.getRequiredEmployeeCount();
        if (count < MIN_REQUIRED_COUNT || count > MAX_REQUIRED_COUNT) {
            throw new IllegalArgumentException(
                    "requiredEmployeeCount must be between " + MIN_REQUIRED_COUNT + " and " + MAX_REQUIRED_COUNT);
        }
    }
}

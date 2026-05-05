package dk.ek.shift_happens.leavetype;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/leavetypes")
@RequiredArgsConstructor
public class LeaveTypeController {

    private final LeaveTypeRepository leaveTypeRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<LeaveType> getAll() {
        return leaveTypeRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public LeaveType getById(@PathVariable Integer id) {
        return leaveTypeRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveType create(@RequestBody LeaveType leaveType) {
        return leaveTypeRepository.save(leaveType);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public LeaveType update(@PathVariable Integer id, @RequestBody LeaveType details) {
        LeaveType existing =
                leaveTypeRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        existing.setLeaveTypeName(details.getLeaveTypeName());
        existing.setLeaveTypeDescription(details.getLeaveTypeDescription());
        existing.setRequiresApproval(details.getRequiresApproval());
        existing.setIsPaidLeave(details.getIsPaidLeave());
        return leaveTypeRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        leaveTypeRepository.deleteById(id);
    }
}

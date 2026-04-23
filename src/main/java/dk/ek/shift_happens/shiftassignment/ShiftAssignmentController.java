package dk.ek.shift_happens.shiftassignment;

import dk.ek.shift_happens.auth.AuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/shiftassignments")
@RequiredArgsConstructor
public class ShiftAssignmentController {

    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final AuthHelper authHelper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ShiftAssignment> getShiftAssignments(Authentication auth) {
        if (authHelper.isEmployee(auth)) {
            return shiftAssignmentRepository.findByEmployeeId(authHelper.currentEmployeeId(auth));
        }
        return this.shiftAssignmentRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ShiftAssignment getShiftAssignmentById(@PathVariable Integer id, Authentication auth) {
        ShiftAssignment assignment = shiftAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (authHelper.isEmployee(auth)
                && !assignment.getEmployeeId().equals(authHelper.currentEmployeeId(auth))) {
            throw authHelper.forbidden();
        }
        return assignment;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ShiftAssignment createShiftAssignment(@RequestBody ShiftAssignment shiftAssignment) {
        return this.shiftAssignmentRepository.save(shiftAssignment);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ShiftAssignment updateShiftAssignment(@PathVariable Integer id, @RequestBody ShiftAssignment shiftAssignmentDetails) {
        ShiftAssignment shiftAssignment = this.shiftAssignmentRepository.findById(id).orElseThrow();
        shiftAssignment.setShiftId(shiftAssignmentDetails.getShiftId());
        shiftAssignment.setEmployeeId(shiftAssignmentDetails.getEmployeeId());
        shiftAssignment.setAssignmentStatus(shiftAssignmentDetails.getAssignmentStatus());
        shiftAssignment.setAssignedDatetime(shiftAssignmentDetails.getAssignedDatetime());
        shiftAssignment.setCheckInDatetime(shiftAssignmentDetails.getCheckInDatetime());
        shiftAssignment.setCheckOutDatetime(shiftAssignmentDetails.getCheckOutDatetime());
        return this.shiftAssignmentRepository.save(shiftAssignment);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public void deleteShiftAssignment(@PathVariable Integer id) {
        this.shiftAssignmentRepository.deleteById(id);
    }
}

package dk.ek.shift_happens.shiftassignment;

import dk.ek.shift_happens.auth.AuthHelper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shiftassignments")
@RequiredArgsConstructor
public class ShiftAssignmentController {

    private final ShiftAssignmentService shiftAssignmentService;
    private final AuthHelper authHelper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ShiftAssignment> getShiftAssignments(Authentication auth) {
        if (authHelper.isEmployee(auth)) {
            return shiftAssignmentService.findByEmployeeId(authHelper.currentEmployeeId(auth));
        }
        return shiftAssignmentService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ShiftAssignment getShiftAssignmentById(@PathVariable Integer id, Authentication auth) {
        ShiftAssignment assignment = shiftAssignmentService.findById(id);
        if (authHelper.isEmployee(auth) && !assignment.getEmployeeId().equals(authHelper.currentEmployeeId(auth))) {
            throw authHelper.forbidden();
        }
        return assignment;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftAssignment createShiftAssignment(@RequestBody ShiftAssignment shiftAssignment) {
        return shiftAssignmentService.assign(shiftAssignment);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ShiftAssignment updateShiftAssignment(
            @PathVariable Integer id, @RequestBody ShiftAssignment shiftAssignmentDetails) {
        return shiftAssignmentService.update(id, shiftAssignmentDetails);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShiftAssignment(@PathVariable Integer id) {
        shiftAssignmentService.delete(id);
    }
}

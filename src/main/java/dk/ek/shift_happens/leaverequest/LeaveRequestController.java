package dk.ek.shift_happens.leaverequest;

import dk.ek.shift_happens.auth.AuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leaverequests")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;
    private final AuthHelper authHelper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LeaveRequest>> getLeaveRequests(Authentication auth) {
        if (authHelper.isEmployee(auth)) {
            return ResponseEntity.ok(
                    this.leaveRequestService.findByEmployeeId(authHelper.currentEmployeeId(auth)));
        }
        return ResponseEntity.ok(this.leaveRequestService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveRequest> getLeaveRequest(@PathVariable Integer id, Authentication auth) {
        return this.leaveRequestService.findById(id)
                .map(req -> {
                    if (authHelper.isEmployee(auth)
                            && !req.getEmployeeId().equals(authHelper.currentEmployeeId(auth))) {
                        throw authHelper.forbidden();
                    }
                    return ResponseEntity.ok(req);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ResponseEntity<LeaveRequest> createLeaveRequest(@RequestBody LeaveRequest leaveRequest) {
        return ResponseEntity.status(201).body(this.leaveRequestService.create(leaveRequest));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ResponseEntity<LeaveRequest> patchLeaveRequest(@PathVariable Integer id, @RequestBody LeaveRequest leaveRequest) {
        return this.leaveRequestService.patch(id, leaveRequest)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ResponseEntity<Void> deleteLeaveRequest(@PathVariable Integer id) {
        return this.leaveRequestService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}

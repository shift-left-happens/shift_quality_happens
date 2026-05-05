package dk.ek.shift_happens.leaveapproval;

import dk.ek.shift_happens.auth.AuthHelper;
import dk.ek.shift_happens.leaverequest.LeaveRequestService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/leaveapprovals")
@RequiredArgsConstructor
public class LeaveApprovalController {

    private final LeaveApprovalService leaveApprovalService;
    private final LeaveRequestService leaveRequestService;
    private final AuthHelper authHelper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LeaveApproval>> getLeaveApprovals(Authentication auth) {
        if (authHelper.isEmployee(auth)) {
            return ResponseEntity.ok(this.leaveApprovalService.findByRequestOwner(authHelper.currentEmployeeId(auth)));
        }
        return ResponseEntity.ok(this.leaveApprovalService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveApproval> getLeaveApproval(@PathVariable Integer id, Authentication auth) {
        return this.leaveApprovalService
                .findById(id)
                .map(approval -> {
                    if (authHelper.isEmployee(auth) && !ownsRequest(approval.getLeaveRequestId(), auth)) {
                        throw authHelper.forbidden();
                    }
                    return ResponseEntity.ok(approval);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/request/{leaveRequestId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LeaveApproval>> getApprovalsForRequest(
            @PathVariable Integer leaveRequestId, Authentication auth) {
        if (authHelper.isEmployee(auth) && !ownsRequest(leaveRequestId, auth)) {
            throw authHelper.forbidden();
        }
        return ResponseEntity.ok(this.leaveApprovalService.findByLeaveRequestId(leaveRequestId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ResponseEntity<LeaveApproval> createLeaveApproval(@RequestBody LeaveApproval leaveApproval) {
        return ResponseEntity.status(201).body(this.leaveApprovalService.approve(leaveApproval));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ResponseEntity<LeaveApproval> updateLeaveApproval(
            @PathVariable Integer id, @RequestBody LeaveApproval leaveApproval) {
        return this.leaveApprovalService
                .update(id, leaveApproval)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ResponseEntity<Void> deleteLeaveApproval(@PathVariable Integer id) {
        return this.leaveApprovalService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    private boolean ownsRequest(Integer leaveRequestId, Authentication auth) {
        return leaveRequestService
                .findById(leaveRequestId)
                .map(lr -> lr.getEmployeeId().equals(authHelper.currentEmployeeId(auth)))
                .orElse(false);
    }
}

package dk.ek.shift_happens.leaveapproval;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leaveapprovals")
@RequiredArgsConstructor
public class LeaveApprovalController {

    private final LeaveApprovalService leaveApprovalService;

    @GetMapping
    public ResponseEntity<List<LeaveApproval>> getLeaveApprovals() {
        return ResponseEntity.ok(this.leaveApprovalService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveApproval> getLeaveApproval(@PathVariable Integer id) {
        return this.leaveApprovalService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/request/{leaveRequestId}")
    public ResponseEntity<List<LeaveApproval>> getApprovalsForRequest(@PathVariable Integer leaveRequestId) {
        return ResponseEntity.ok(this.leaveApprovalService.findByLeaveRequestId(leaveRequestId));
    }

    @PostMapping
    public ResponseEntity<LeaveApproval> createLeaveApproval(@RequestBody LeaveApproval leaveApproval) {
        return ResponseEntity.status(201).body(this.leaveApprovalService.approve(leaveApproval));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeaveApproval(@PathVariable Integer id) {
        return this.leaveApprovalService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}

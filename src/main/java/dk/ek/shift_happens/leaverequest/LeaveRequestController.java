package dk.ek.shift_happens.leaverequest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leaverequests")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @GetMapping
    public ResponseEntity<List<LeaveRequest>> getLeaveRequests() {
        return ResponseEntity.ok(this.leaveRequestService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequest> getLeaveRequest(@PathVariable Integer id) {
        return this.leaveRequestService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<LeaveRequest> createLeaveRequest(@RequestBody LeaveRequest leaveRequest) {
        return ResponseEntity.status(201).body(this.leaveRequestService.create(leaveRequest));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<LeaveRequest> patchLeaveRequest(@PathVariable Integer id, @RequestBody LeaveRequest leaveRequest) {
        return this.leaveRequestService.patch(id, leaveRequest)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeaveRequest(@PathVariable Integer id) {
        return this.leaveRequestService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}

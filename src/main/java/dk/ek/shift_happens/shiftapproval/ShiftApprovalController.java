package dk.ek.shift_happens.shiftapproval;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shiftapprovals")
@RequiredArgsConstructor
public class ShiftApprovalController {

    private final ShiftApprovalRepository shiftApprovalRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public List<ShiftApproval> getShiftApprovals() {
        return this.shiftApprovalRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public Optional<ShiftApproval> getShiftApprovalById(@PathVariable Integer id) {
        return this.shiftApprovalRepository.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ShiftApproval createShiftApproval(@RequestBody ShiftApproval shiftApproval) {
        return this.shiftApprovalRepository.save(shiftApproval);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ShiftApproval updateShiftApproval(
            @PathVariable Integer id, @RequestBody ShiftApproval shiftApprovalDetails) {
        ShiftApproval shiftApproval = this.shiftApprovalRepository.findById(id).orElseThrow();
        shiftApproval.setShiftAssignmentId(shiftApprovalDetails.getShiftAssignmentId());
        shiftApproval.setApproverEmployeeId(shiftApprovalDetails.getApproverEmployeeId());
        shiftApproval.setDecision(shiftApprovalDetails.getDecision());
        shiftApproval.setApprovalComment(shiftApprovalDetails.getApprovalComment());
        shiftApproval.setDecisionDatetime(shiftApprovalDetails.getDecisionDatetime());
        return this.shiftApprovalRepository.save(shiftApproval);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public void deleteShiftApproval(@PathVariable Integer id) {
        this.shiftApprovalRepository.deleteById(id);
    }
}

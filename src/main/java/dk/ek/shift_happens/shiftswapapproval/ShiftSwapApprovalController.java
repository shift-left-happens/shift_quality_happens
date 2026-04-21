package dk.ek.shift_happens.shiftswapapproval;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/shiftswapapprovals")
@RequiredArgsConstructor
public class ShiftSwapApprovalController {

    private final ShiftSwapApprovalRepository shiftSwapApprovalRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public List<ShiftSwapApproval> getShiftSwapApprovals() {
        return this.shiftSwapApprovalRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public Optional<ShiftSwapApproval> getShiftSwapApprovalById(@PathVariable Integer id) {
        return this.shiftSwapApprovalRepository.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ShiftSwapApproval createShiftSwapApproval(@RequestBody ShiftSwapApproval shiftSwapApproval) {
        return this.shiftSwapApprovalRepository.save(shiftSwapApproval);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ShiftSwapApproval updateShiftSwapApproval(@PathVariable Integer id, @RequestBody ShiftSwapApproval shiftSwapApprovalDetails) {
        ShiftSwapApproval shiftSwapApproval = this.shiftSwapApprovalRepository.findById(id).orElseThrow();
        shiftSwapApproval.setShiftSwapId(shiftSwapApprovalDetails.getShiftSwapId());
        shiftSwapApproval.setApproverEmployeeId(shiftSwapApprovalDetails.getApproverEmployeeId());
        shiftSwapApproval.setDecision(shiftSwapApprovalDetails.getDecision());
        shiftSwapApproval.setShiftSwapComment(shiftSwapApprovalDetails.getShiftSwapComment());
        shiftSwapApproval.setDecisionDatetime(shiftSwapApprovalDetails.getDecisionDatetime());
        return this.shiftSwapApprovalRepository.save(shiftSwapApproval);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public void deleteShiftSwapApproval(@PathVariable Integer id) {
        this.shiftSwapApprovalRepository.deleteById(id);
    }
}

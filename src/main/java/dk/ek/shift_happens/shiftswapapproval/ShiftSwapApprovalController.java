package dk.ek.shift_happens.shiftswapapproval;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/shiftswapapprovals")
@RequiredArgsConstructor
public class ShiftSwapApprovalController {

    private final ShiftSwapApprovalRepository shiftSwapApprovalRepository;

    @GetMapping
    public List<ShiftSwapApproval> getShiftSwapApprovals() {
        return this.shiftSwapApprovalRepository.findAll();
    }

    @GetMapping("/{id}")
    public Optional<ShiftSwapApproval> getShiftSwapApprovalById(@PathVariable Integer id) {
        return this.shiftSwapApprovalRepository.findById(id);
    }

    @PostMapping
    public ShiftSwapApproval createShiftSwapApproval(@RequestBody ShiftSwapApproval shiftSwapApproval) {
        return this.shiftSwapApprovalRepository.save(shiftSwapApproval);
    }

    @PutMapping("/{id}")
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
    public void deleteShiftSwapApproval(@PathVariable Integer id) {
        this.shiftSwapApprovalRepository.deleteById(id);
    }
}

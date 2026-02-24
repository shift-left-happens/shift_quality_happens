package dk.ek.shift_happens.shiftswapapproval;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shiftswapapprovals")
@RequiredArgsConstructor
public class ShiftSwapApprovalController {

    private final ShiftSwapApprovalRepository shiftSwapApprovalRepository;

    @GetMapping
    public List<ShiftSwapApproval> getShiftSwapApprovals() {
        return this.shiftSwapApprovalRepository.findAll();
    }
}

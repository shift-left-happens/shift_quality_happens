package dk.ek.shift_happens.shiftapproval;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shiftapprovals")
@RequiredArgsConstructor
public class ShiftApprovalController {

    private final ShiftApprovalRepository shiftApprovalRepository;

    @GetMapping
    public List<ShiftApproval> getShiftApprovals() {
        return this.shiftApprovalRepository.findAll();
    }
}

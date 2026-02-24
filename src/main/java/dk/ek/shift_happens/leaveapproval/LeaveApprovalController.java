package dk.ek.shift_happens.leaveapproval;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/leaveapprovals")
@RequiredArgsConstructor
public class LeaveApprovalController {

    private final LeaveApprovalRepository leaveApprovalRepository;

    @GetMapping
    public List<LeaveApproval> getLeaveApprovals() {
        return this.leaveApprovalRepository.findAll();
    }
}

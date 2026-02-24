package dk.ek.shift_happens.leaverequest;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/leaverequests")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestRepository leaveRequestRepository;

    @GetMapping
    public List<LeaveRequest> getLeaveRequests() {
        return this.leaveRequestRepository.findAll();
    }
}

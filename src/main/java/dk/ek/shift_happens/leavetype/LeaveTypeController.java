package dk.ek.shift_happens.leavetype;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/leavetypes")
@RequiredArgsConstructor
public class LeaveTypeController {

    private final LeaveTypeRepository leaveTypeRepository;

    @GetMapping
    public List<LeaveType> getLeaveTypes() {
        return this.leaveTypeRepository.findAll();
    }
}

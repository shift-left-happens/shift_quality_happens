package dk.ek.shift_happens.view.employeeleaveoverview;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/views/employee-leave-overview")
@RequiredArgsConstructor
public class EmployeeLeaveOverviewController {

    private final EmployeeLeaveOverviewRepository employeeLeaveOverviewRepository;

    @GetMapping
    public List<EmployeeLeaveOverviewDto> getAllEmployeeLeaveOverview() {
        return this.employeeLeaveOverviewRepository.findAll()
                .stream()
                .map(EmployeeLeaveOverviewDto::from)
                .toList();
    }

    @GetMapping("/employee/{employeeId}")
    public List<EmployeeLeaveOverviewDto> getEmployeeLeaveOverviewByEmployeeId(@PathVariable Integer employeeId) {
        return this.employeeLeaveOverviewRepository.findByEmployeeId(employeeId)
                .stream()
                .map(EmployeeLeaveOverviewDto::from)
                .toList();
    }

    @GetMapping("/status/{status}")
    public List<EmployeeLeaveOverviewDto> getEmployeeLeaveOverviewByStatus(@PathVariable String status) {
        return this.employeeLeaveOverviewRepository.findByRequestStatus(status)
                .stream()
                .map(EmployeeLeaveOverviewDto::from)
                .toList();
    }

    @GetMapping("/leave-type/{leaveTypeName}")
    public List<EmployeeLeaveOverviewDto> getEmployeeLeaveOverviewByLeaveType(@PathVariable String leaveTypeName) {
        return this.employeeLeaveOverviewRepository.findByLeaveTypeName(leaveTypeName)
                .stream()
                .map(EmployeeLeaveOverviewDto::from)
                .toList();
    }
}

package dk.ek.shift_happens.view.employeeleaveoverview;

import dk.ek.shift_happens.auth.AuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/views/employee-leave-overview")
@RequiredArgsConstructor
public class EmployeeLeaveOverviewController {

    private final EmployeeLeaveOverviewRepository employeeLeaveOverviewRepository;
    private final AuthHelper authHelper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<EmployeeLeaveOverviewDto> getAllEmployeeLeaveOverview(Authentication auth) {
        if (authHelper.isEmployee(auth)) {
            return this.employeeLeaveOverviewRepository
                    .findByEmployeeId(authHelper.currentEmployeeId(auth))
                    .stream()
                    .map(EmployeeLeaveOverviewDto::from)
                    .toList();
        }
        return this.employeeLeaveOverviewRepository.findAll()
                .stream()
                .map(EmployeeLeaveOverviewDto::from)
                .toList();
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    public List<EmployeeLeaveOverviewDto> getEmployeeLeaveOverviewByEmployeeId(@PathVariable Integer employeeId,
                                                                               Authentication auth) {
        if (authHelper.isEmployee(auth) && !employeeId.equals(authHelper.currentEmployeeId(auth))) {
            throw authHelper.forbidden();
        }
        return this.employeeLeaveOverviewRepository.findByEmployeeId(employeeId)
                .stream()
                .map(EmployeeLeaveOverviewDto::from)
                .toList();
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public List<EmployeeLeaveOverviewDto> getEmployeeLeaveOverviewByStatus(@PathVariable String status) {
        return this.employeeLeaveOverviewRepository.findByRequestStatus(status)
                .stream()
                .map(EmployeeLeaveOverviewDto::from)
                .toList();
    }

    @GetMapping("/leave-type/{leaveTypeName}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public List<EmployeeLeaveOverviewDto> getEmployeeLeaveOverviewByLeaveType(@PathVariable String leaveTypeName) {
        return this.employeeLeaveOverviewRepository.findByLeaveTypeName(leaveTypeName)
                .stream()
                .map(EmployeeLeaveOverviewDto::from)
                .toList();
    }
}

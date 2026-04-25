package dk.ek.shift_happens.view.employeeshiftoverview;

import dk.ek.shift_happens.auth.AuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/views/employee-shift-overview")
@RequiredArgsConstructor
public class EmployeeShiftOverviewController {

    private final EmployeeShiftOverviewRepository employeeShiftOverviewRepository;
    private final AuthHelper authHelper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<EmployeeShiftOverviewDto> getEmployeeShiftOverview(Authentication auth) {
        if (authHelper.isEmployee(auth)) {
            return this.employeeShiftOverviewRepository
                    .findByEmployeeId(authHelper.currentEmployeeId(auth))
                    .stream()
                    .map(EmployeeShiftOverviewDto::from)
                    .toList();
        }
        return this.employeeShiftOverviewRepository.findAll()
                .stream()
                .map(EmployeeShiftOverviewDto::from)
                .toList();
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    public List<EmployeeShiftOverviewDto> getEmployeeShiftOverviewByEmployeeId(@PathVariable Integer employeeId,
                                                                               Authentication auth) {
        if (authHelper.isEmployee(auth) && !employeeId.equals(authHelper.currentEmployeeId(auth))) {
            throw authHelper.forbidden();
        }
        return this.employeeShiftOverviewRepository.findByEmployeeId(employeeId)
                .stream()
                .map(EmployeeShiftOverviewDto::from)
                .toList();
    }
}

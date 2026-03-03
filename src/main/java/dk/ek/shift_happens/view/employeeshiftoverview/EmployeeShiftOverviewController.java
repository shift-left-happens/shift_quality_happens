package dk.ek.shift_happens.view.employeeshiftoverview;

import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public List<EmployeeShiftOverviewDto> getEmployeeShiftOverview() {
        return this.employeeShiftOverviewRepository.findAll()
                .stream()
                .map(EmployeeShiftOverviewDto::from)
                .toList();
    }

    @GetMapping("/employee/{employeeId}")
    public List<EmployeeShiftOverviewDto> getEmployeeShiftOverviewByEmployeeId(@PathVariable Integer employeeId) {
        return this.employeeShiftOverviewRepository.findByEmployeeId(employeeId)
                .stream()
                .map(EmployeeShiftOverviewDto::from)
                .toList();
    }
}

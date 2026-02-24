package dk.ek.shift_happens.employeejobrole;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/employeejobroles")
@RequiredArgsConstructor
public class EmployeeJobRoleController {

    private final EmployeeJobRoleRepository employeeJobRoleRepository;

    @GetMapping
    public List<EmployeeJobRole> getEmployeeJobRoles() {
        return this.employeeJobRoleRepository.findAll();
    }
}

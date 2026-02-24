package dk.ek.shift_happens.employeecontract;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/employeecontracts")
@RequiredArgsConstructor
public class EmployeeContractController {

    private final EmployeeContractRepository employeeContractRepository;

    @GetMapping
    public List<EmployeeContract> getEmployeeContracts() {
        return this.employeeContractRepository.findAll();
    }
}

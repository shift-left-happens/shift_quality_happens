package dk.ek.shift_happens.shiftrequiredjobrole;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shiftrequiredjobroles")
@RequiredArgsConstructor
public class ShiftRequiredJobRoleController {

    private final ShiftRequiredJobRoleRepository shiftRequiredJobRoleRepository;

    @GetMapping
    public List<ShiftRequiredJobRole> getShiftRequiredJobRoles() {
        return this.shiftRequiredJobRoleRepository.findAll();
    }
}

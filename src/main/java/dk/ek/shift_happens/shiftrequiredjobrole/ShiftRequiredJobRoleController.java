package dk.ek.shift_happens.shiftrequiredjobrole;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/shiftrequiredjobroles")
@RequiredArgsConstructor
public class ShiftRequiredJobRoleController {

    private final ShiftRequiredJobRoleRepository shiftRequiredJobRoleRepository;

    @GetMapping
    public List<ShiftRequiredJobRole> getShiftRequiredJobRoles() {
        return this.shiftRequiredJobRoleRepository.findAll();
    }

    @GetMapping("/{id}")
    public Optional<ShiftRequiredJobRole> getShiftRequiredJobRoleById(@PathVariable Integer id) {
        return this.shiftRequiredJobRoleRepository.findById(id);
    }

    @PostMapping
    public ShiftRequiredJobRole createShiftRequiredJobRole(@RequestBody ShiftRequiredJobRole shiftRequiredJobRole) {
        return this.shiftRequiredJobRoleRepository.save(shiftRequiredJobRole);
    }

    @PutMapping("/{id}")
    public ShiftRequiredJobRole updateShiftRequiredJobRole(@PathVariable Integer id, @RequestBody ShiftRequiredJobRole shiftRequiredJobRoleDetails) {
        ShiftRequiredJobRole shiftRequiredJobRole = this.shiftRequiredJobRoleRepository.findById(id).orElseThrow();
        shiftRequiredJobRole.setShiftId(shiftRequiredJobRoleDetails.getShiftId());
        shiftRequiredJobRole.setJobRoleId(shiftRequiredJobRoleDetails.getJobRoleId());
        shiftRequiredJobRole.setRequiredEmployeeCount(shiftRequiredJobRoleDetails.getRequiredEmployeeCount());
        return this.shiftRequiredJobRoleRepository.save(shiftRequiredJobRole);
    }

    @DeleteMapping("/{id}")
    public void deleteShiftRequiredJobRole(@PathVariable Integer id) {
        this.shiftRequiredJobRoleRepository.deleteById(id);
    }
}

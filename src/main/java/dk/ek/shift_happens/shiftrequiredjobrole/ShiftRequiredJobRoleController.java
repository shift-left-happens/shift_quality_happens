package dk.ek.shift_happens.shiftrequiredjobrole;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shiftrequiredjobroles")
@RequiredArgsConstructor
public class ShiftRequiredJobRoleController {

    private final ShiftRequiredJobRoleRepository shiftRequiredJobRoleRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ShiftRequiredJobRole> getShiftRequiredJobRoles() {
        return this.shiftRequiredJobRoleRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Optional<ShiftRequiredJobRole> getShiftRequiredJobRoleById(@PathVariable Integer id) {
        return this.shiftRequiredJobRoleRepository.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ShiftRequiredJobRole createShiftRequiredJobRole(@RequestBody ShiftRequiredJobRole shiftRequiredJobRole) {
        return this.shiftRequiredJobRoleRepository.save(shiftRequiredJobRole);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ShiftRequiredJobRole updateShiftRequiredJobRole(
            @PathVariable Integer id, @RequestBody ShiftRequiredJobRole shiftRequiredJobRoleDetails) {
        ShiftRequiredJobRole shiftRequiredJobRole =
                this.shiftRequiredJobRoleRepository.findById(id).orElseThrow();
        shiftRequiredJobRole.setShiftId(shiftRequiredJobRoleDetails.getShiftId());
        shiftRequiredJobRole.setJobRoleId(shiftRequiredJobRoleDetails.getJobRoleId());
        shiftRequiredJobRole.setRequiredEmployeeCount(shiftRequiredJobRoleDetails.getRequiredEmployeeCount());
        return this.shiftRequiredJobRoleRepository.save(shiftRequiredJobRole);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public void deleteShiftRequiredJobRole(@PathVariable Integer id) {
        this.shiftRequiredJobRoleRepository.deleteById(id);
    }
}

package dk.ek.shift_happens.shiftrequiredjobrole;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shiftrequiredjobroles")
@RequiredArgsConstructor
public class ShiftRequiredJobRoleController {

    private final ShiftRequiredJobRoleService shiftRequiredJobRoleService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ShiftRequiredJobRole> getShiftRequiredJobRoles() {
        return shiftRequiredJobRoleService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ShiftRequiredJobRole getShiftRequiredJobRoleById(@PathVariable Integer id) {
        return shiftRequiredJobRoleService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftRequiredJobRole createShiftRequiredJobRole(@RequestBody ShiftRequiredJobRole shiftRequiredJobRole) {
        return shiftRequiredJobRoleService.create(shiftRequiredJobRole);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ShiftRequiredJobRole updateShiftRequiredJobRole(
            @PathVariable Integer id, @RequestBody ShiftRequiredJobRole shiftRequiredJobRoleDetails) {
        return shiftRequiredJobRoleService.update(id, shiftRequiredJobRoleDetails);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShiftRequiredJobRole(@PathVariable Integer id) {
        shiftRequiredJobRoleService.delete(id);
    }
}

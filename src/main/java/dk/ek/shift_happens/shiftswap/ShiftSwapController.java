package dk.ek.shift_happens.shiftswap;

import dk.ek.shift_happens.auth.AuthHelper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shiftswaps")
@RequiredArgsConstructor
public class ShiftSwapController {

    private final ShiftSwapService shiftSwapService;
    private final AuthHelper authHelper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ShiftSwap> getShiftSwaps(Authentication auth) {
        if (authHelper.isEmployee(auth)) {
            return shiftSwapService.findByEmployee(authHelper.currentEmployeeId(auth));
        }
        return shiftSwapService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ShiftSwap getShiftSwapById(@PathVariable Integer id, Authentication auth) {
        ShiftSwap swap = shiftSwapService.findById(id);
        if (authHelper.isEmployee(auth)) {
            Integer self = authHelper.currentEmployeeId(auth);
            if (!self.equals(swap.getEmployeeFromId()) && !self.equals(swap.getEmployeeToId())) {
                throw authHelper.forbidden();
            }
        }
        return swap;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftSwap createShiftSwap(@RequestBody ShiftSwap shiftSwap, Authentication auth) {
        if (authHelper.isEmployee(auth)) {
            Integer self = authHelper.currentEmployeeId(auth);
            if (!self.equals(shiftSwap.getEmployeeFromId())) {
                throw authHelper.forbidden();
            }
        }
        return shiftSwapService.create(shiftSwap);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ShiftSwap updateShiftSwap(@PathVariable Integer id, @RequestBody ShiftSwap shiftSwapDetails) {
        return shiftSwapService.update(id, shiftSwapDetails);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ShiftSwap cancelShiftSwap(@PathVariable Integer id, Authentication auth) {
        Integer cancellingEmployee = authHelper.isEmployee(auth) ? authHelper.currentEmployeeId(auth) : null;
        return shiftSwapService.cancel(id, cancellingEmployee);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShiftSwap(@PathVariable Integer id) {
        shiftSwapService.delete(id);
    }
}

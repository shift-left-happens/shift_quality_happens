package dk.ek.shift_happens.shiftswap;

import dk.ek.shift_happens.auth.AuthHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/shiftswaps")
@RequiredArgsConstructor
public class ShiftSwapController {

    private final ShiftSwapRepository shiftSwapRepository;
    private final AuthHelper authHelper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ShiftSwap> getShiftSwaps(Authentication auth) {
        if (authHelper.isEmployee(auth)) {
            Integer self = authHelper.currentEmployeeId(auth);
            return shiftSwapRepository.findByEmployeeFromIdOrEmployeeToId(self, self);
        }
        return this.shiftSwapRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ShiftSwap getShiftSwapById(@PathVariable Integer id, Authentication auth) {
        ShiftSwap swap = shiftSwapRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (authHelper.isEmployee(auth)) {
            Integer self = authHelper.currentEmployeeId(auth);
            if (!self.equals(swap.getEmployeeFromId()) && !self.equals(swap.getEmployeeToId())) {
                throw authHelper.forbidden();
            }
        }
        return swap;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ShiftSwap createShiftSwap(@RequestBody ShiftSwap shiftSwap) {
        return this.shiftSwapRepository.save(shiftSwap);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ShiftSwap updateShiftSwap(@PathVariable Integer id, @RequestBody ShiftSwap shiftSwapDetails) {
        ShiftSwap shiftSwap = this.shiftSwapRepository.findById(id).orElseThrow();
        shiftSwap.setOriginalShiftAssignmentId(shiftSwapDetails.getOriginalShiftAssignmentId());
        shiftSwap.setEmployeeFromId(shiftSwapDetails.getEmployeeFromId());
        shiftSwap.setEmployeeToId(shiftSwapDetails.getEmployeeToId());
        shiftSwap.setSwapStatus(shiftSwapDetails.getSwapStatus());
        shiftSwap.setRequestDatetime(shiftSwapDetails.getRequestDatetime());
        shiftSwap.setReason(shiftSwapDetails.getReason());
        return this.shiftSwapRepository.save(shiftSwap);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public void deleteShiftSwap(@PathVariable Integer id) {
        this.shiftSwapRepository.deleteById(id);
    }
}

package dk.ek.shift_happens.shift;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Shift> getShifts() {
        return shiftService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Shift getShiftById(@PathVariable Integer id) {
        return shiftService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public Shift createShift(@RequestBody Shift shift) {
        return shiftService.create(shift);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public Shift updateShift(@PathVariable Integer id, @RequestBody Shift shiftDetails) {
        return shiftService.update(id, shiftDetails);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public Shift cancelShift(@PathVariable Integer id) {
        return shiftService.cancel(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShift(@PathVariable Integer id) {
        shiftService.delete(id);
    }
}

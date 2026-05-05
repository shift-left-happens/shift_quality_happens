package dk.ek.shift_happens.shift;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftRepository shiftRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Shift> getShifts() {
        return this.shiftRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Optional<Shift> getShiftById(@PathVariable Integer id) {
        return this.shiftRepository.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public Shift createShift(@RequestBody Shift shift) {
        return this.shiftRepository.save(shift);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public Shift updateShift(@PathVariable Integer id, @RequestBody Shift shiftDetails) {
        Shift shift = this.shiftRepository.findById(id).orElseThrow();
        shift.setDepartmentId(shiftDetails.getDepartmentId());
        shift.setWorkLocationId(shiftDetails.getWorkLocationId());
        shift.setShiftName(shiftDetails.getShiftName());
        shift.setStartDatetime(shiftDetails.getStartDatetime());
        shift.setEndDatetime(shiftDetails.getEndDatetime());
        shift.setShiftStatus(shiftDetails.getShiftStatus());
        return this.shiftRepository.save(shift);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public void deleteShift(@PathVariable Integer id) {
        this.shiftRepository.deleteById(id);
    }
}

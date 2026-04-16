package dk.ek.shift_happens.shiftswap;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/shiftswaps")
@RequiredArgsConstructor
public class ShiftSwapController {

    private final ShiftSwapRepository shiftSwapRepository;

    @GetMapping
    public List<ShiftSwap> getShiftSwaps() {
        return this.shiftSwapRepository.findAll();
    }

    @GetMapping("/{id}")
    public Optional<ShiftSwap> getShiftSwapById(@PathVariable Integer id) {
        return this.shiftSwapRepository.findById(id);
    }

    @PostMapping
    public ShiftSwap createShiftSwap(@RequestBody ShiftSwap shiftSwap) {
        return this.shiftSwapRepository.save(shiftSwap);
    }

    @PutMapping("/{id}")
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
    public void deleteShiftSwap(@PathVariable Integer id) {
        this.shiftSwapRepository.deleteById(id);
    }
}

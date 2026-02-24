package dk.ek.shift_happens.shiftswap;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shiftswaps")
@RequiredArgsConstructor
public class ShiftSwapController {

    private final ShiftSwapRepository shiftSwapRepository;

    @GetMapping
    public List<ShiftSwap> getShiftSwaps() {
        return this.shiftSwapRepository.findAll();
    }
}

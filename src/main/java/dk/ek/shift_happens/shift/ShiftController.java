package dk.ek.shift_happens.shift;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftRepository shiftRepository;

    @GetMapping
    public List<Shift> getShifts() {
        return this.shiftRepository.findAll();
    }
}

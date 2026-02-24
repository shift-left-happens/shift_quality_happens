package dk.ek.shift_happens.shiftassignment;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/shiftassignments")
@RequiredArgsConstructor
public class ShiftAssignmentController {

    private final ShiftAssignmentRepository shiftAssignmentRepository;

    @GetMapping
    public List<ShiftAssignment> getShiftAssignments() {
        return this.shiftAssignmentRepository.findAll();
    }
}

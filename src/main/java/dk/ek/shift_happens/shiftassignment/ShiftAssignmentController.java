package dk.ek.shift_happens.shiftassignment;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/shiftassignments")
@RequiredArgsConstructor
public class ShiftAssignmentController {

    private final ShiftAssignmentRepository shiftAssignmentRepository;

    @GetMapping
    public List<ShiftAssignment> getShiftAssignments() {
        return this.shiftAssignmentRepository.findAll();
    }

    @GetMapping("/{id}")
    public Optional<ShiftAssignment> getShiftAssignmentById(@PathVariable Integer id) {
        return this.shiftAssignmentRepository.findById(id);
    }

    @PostMapping
    public ShiftAssignment createShiftAssignment(@RequestBody ShiftAssignment shiftAssignment) {
        return this.shiftAssignmentRepository.save(shiftAssignment);
    }

    @PutMapping("/{id}")
    public ShiftAssignment updateShiftAssignment(@PathVariable Integer id, @RequestBody ShiftAssignment shiftAssignmentDetails) {
        ShiftAssignment shiftAssignment = this.shiftAssignmentRepository.findById(id).orElseThrow();
        shiftAssignment.setShiftId(shiftAssignmentDetails.getShiftId());
        shiftAssignment.setEmployeeId(shiftAssignmentDetails.getEmployeeId());
        shiftAssignment.setAssignmentStatus(shiftAssignmentDetails.getAssignmentStatus());
        shiftAssignment.setAssignedDatetime(shiftAssignmentDetails.getAssignedDatetime());
        shiftAssignment.setCheckInDatetime(shiftAssignmentDetails.getCheckInDatetime());
        shiftAssignment.setCheckOutDatetime(shiftAssignmentDetails.getCheckOutDatetime());
        return this.shiftAssignmentRepository.save(shiftAssignment);
    }

    @DeleteMapping("/{id}")
    public void deleteShiftAssignment(@PathVariable Integer id) {
        this.shiftAssignmentRepository.deleteById(id);
    }
}

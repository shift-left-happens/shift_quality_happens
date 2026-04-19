package dk.ek.shift_happens.leavetype.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/mongo/leave_type")
@RequiredArgsConstructor
public class LeaveTypeMongoController {

    private final LeaveTypeMongoRepository leaveTypeMongoRepository;

    @GetMapping
    public List<LeaveTypeDocument> getAll() {
        return leaveTypeMongoRepository.findAll();
    }

    @GetMapping("/{id}")
    public LeaveTypeDocument getById(@PathVariable String id) {
        return leaveTypeMongoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public LeaveTypeDocument create(@RequestBody LeaveTypeDocument leaveType) {
        return leaveTypeMongoRepository.save(leaveType);
    }

    @PutMapping("/{id}")
    public LeaveTypeDocument update(@PathVariable String id, @RequestBody LeaveTypeDocument leaveType) {
        if (!leaveTypeMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        leaveType.setId(id);
        return leaveTypeMongoRepository.save(leaveType);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        if (!leaveTypeMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        leaveTypeMongoRepository.deleteById(id);
    }
}

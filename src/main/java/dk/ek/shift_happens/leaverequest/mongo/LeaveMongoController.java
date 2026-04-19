package dk.ek.shift_happens.leaverequest.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/mongo/leave")
@RequiredArgsConstructor
public class LeaveMongoController {

    private final LeaveMongoRepository leaveMongoRepository;

    @GetMapping
    public List<LeaveDocument> getAll() {
        return leaveMongoRepository.findAll();
    }

    @GetMapping("/{id}")
    public LeaveDocument getById(@PathVariable String id) {
        return leaveMongoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public LeaveDocument create(@RequestBody LeaveDocument leave) {
        return leaveMongoRepository.save(leave);
    }

    @PutMapping("/{id}")
    public LeaveDocument update(@PathVariable String id, @RequestBody LeaveDocument leave) {
        if (!leaveMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        leave.setId(id);
        return leaveMongoRepository.save(leave);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        if (!leaveMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        leaveMongoRepository.deleteById(id);
    }
}

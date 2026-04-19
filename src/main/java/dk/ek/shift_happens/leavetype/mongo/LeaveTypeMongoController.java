package dk.ek.shift_happens.leavetype.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}

package dk.ek.shift_happens.shift.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/mongo/shifts")
@RequiredArgsConstructor
public class ShiftMongoController {

    private final ShiftMongoRepository shiftMongoRepository;

    @GetMapping
    public List<ShiftDocument> getAll() {
        return shiftMongoRepository.findAll();
    }

    @GetMapping("/{id}")
    public ShiftDocument getById(@PathVariable String id) {
        return shiftMongoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

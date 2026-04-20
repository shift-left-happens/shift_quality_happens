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
    public ShiftDocument getById(@PathVariable Integer id) {
        return shiftMongoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public ShiftDocument create(@RequestBody ShiftDocument shift) {
        return shiftMongoRepository.save(shift);
    }

    @PutMapping("/{id}")
    public ShiftDocument update(@PathVariable Integer id, @RequestBody ShiftDocument shift) {
        if (!shiftMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        shift.setShiftId(id);
        return shiftMongoRepository.save(shift);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        if (!shiftMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        shiftMongoRepository.deleteById(id);
    }
}

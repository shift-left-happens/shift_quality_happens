package dk.ek.shift_happens.worklocation.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/mongo/work_location")
@RequiredArgsConstructor
public class WorkLocationMongoController {

    private final WorkLocationMongoRepository workLocationMongoRepository;

    @GetMapping
    public List<WorkLocationDocument> getAll() {
        return workLocationMongoRepository.findAll();
    }

    @GetMapping("/{id}")
    public WorkLocationDocument getById(@PathVariable String id) {
        return workLocationMongoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public WorkLocationDocument create(@RequestBody WorkLocationDocument workLocation) {
        return workLocationMongoRepository.save(workLocation);
    }

    @PutMapping("/{id}")
    public WorkLocationDocument update(@PathVariable String id, @RequestBody WorkLocationDocument workLocation) {
        if (!workLocationMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        workLocation.setId(id);
        return workLocationMongoRepository.save(workLocation);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        if (!workLocationMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        workLocationMongoRepository.deleteById(id);
    }
}

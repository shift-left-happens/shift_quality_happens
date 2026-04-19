package dk.ek.shift_happens.worklocation.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}

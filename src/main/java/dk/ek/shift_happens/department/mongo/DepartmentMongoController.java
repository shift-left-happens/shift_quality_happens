package dk.ek.shift_happens.department.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/mongo/departments")
@RequiredArgsConstructor
public class DepartmentMongoController {

    private final DepartmentMongoRepository departmentMongoRepository;

    @GetMapping
    public List<DepartmentDocument> getAll() {
        return departmentMongoRepository.findAll();
    }

    @GetMapping("/{id}")
    public DepartmentDocument getById(@PathVariable String id) {
        return departmentMongoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

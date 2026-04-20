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

    @PostMapping
    public DepartmentDocument create(@RequestBody DepartmentDocument department) {
        return departmentMongoRepository.save(department);
    }

    @PutMapping("/{id}")
    public DepartmentDocument update(@PathVariable String id, @RequestBody DepartmentDocument department) {
        if (!departmentMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        department.setId(id);
        return departmentMongoRepository.save(department);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        if (!departmentMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        departmentMongoRepository.deleteById(id);
    }
}

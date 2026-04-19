package dk.ek.shift_happens.employee.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/mongo/employees")
@RequiredArgsConstructor
public class EmployeeMongoController {

    private final EmployeeMongoRepository employeeMongoRepository;

    @GetMapping
    public List<EmployeeDocument> getAll() {
        return employeeMongoRepository.findAll();
    }

    @GetMapping("/{id}")
    public EmployeeDocument getById(@PathVariable Integer id) {
        return employeeMongoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public EmployeeDocument create(@RequestBody EmployeeDocument employee) {
        return employeeMongoRepository.save(employee);
    }

    @PutMapping("/{id}")
    public EmployeeDocument update(@PathVariable Integer id, @RequestBody EmployeeDocument employee) {
        if (!employeeMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        employee.setEmployeeId(id);
        return employeeMongoRepository.save(employee);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        if (!employeeMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        employeeMongoRepository.deleteById(id);
    }
}

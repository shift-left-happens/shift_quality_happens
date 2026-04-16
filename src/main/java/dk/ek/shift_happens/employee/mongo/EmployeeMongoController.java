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
    public EmployeeDocument getById(@PathVariable String id) {
        return employeeMongoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

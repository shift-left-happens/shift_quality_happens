package dk.ek.shift_happens.userrole.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/mongo/user_role")
@RequiredArgsConstructor
public class UserRoleMongoController {

    private final UserRoleMongoRepository userRoleMongoRepository;

    @GetMapping
    public List<UserRoleDocument> getAll() {
        return userRoleMongoRepository.findAll();
    }

    @GetMapping("/{id}")
    public UserRoleDocument getById(@PathVariable String id) {
        return userRoleMongoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public UserRoleDocument create(@RequestBody UserRoleDocument userRole) {
        return userRoleMongoRepository.save(userRole);
    }

    @PutMapping("/{id}")
    public UserRoleDocument update(@PathVariable String id, @RequestBody UserRoleDocument userRole) {
        if (!userRoleMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        userRole.setId(id);
        return userRoleMongoRepository.save(userRole);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        if (!userRoleMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        userRoleMongoRepository.deleteById(id);
    }
}

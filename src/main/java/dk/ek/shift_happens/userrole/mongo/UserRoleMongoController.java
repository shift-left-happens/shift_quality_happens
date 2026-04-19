package dk.ek.shift_happens.userrole.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}

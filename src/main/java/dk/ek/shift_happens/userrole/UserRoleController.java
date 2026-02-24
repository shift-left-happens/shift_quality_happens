package dk.ek.shift_happens.userrole;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/userroles")
@RequiredArgsConstructor
public class UserRoleController {

    private final UserRoleRepository userRoleRepository;

    @GetMapping
    public List<UserRole> getUserRoles() {
        return this.userRoleRepository.findAll();
    }
}

package dk.ek.shift_happens.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Integer employeeId;
    private String employeeNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String roleName;
}

package dk.ek.shift_happens.employee;

import lombok.Getter;

@Getter
public enum UserRole {
    Administrator("Administrator"),
    Manager("Manager"),
    Employee("Employee");

    private final String roleName;

    UserRole(String roleName) {
        this.roleName = roleName;
    }
}

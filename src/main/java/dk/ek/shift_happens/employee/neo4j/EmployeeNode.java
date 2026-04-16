package dk.ek.shift_happens.employee.neo4j;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDate;

@Node("Employee")
@Getter
@Setter
@NoArgsConstructor
public class EmployeeNode {

    @Id
    @GeneratedValue
    private Long id;

    private Integer employeeId;
    private String employeeNumber;
    private String firstName;
    private String lastName;
    private String email;
    private Integer fkUserRoleId;
    private String phoneNumber;
    private LocalDate hireDate;
    private String employmentStatus;
    private Integer primaryWorkLocationId;
}

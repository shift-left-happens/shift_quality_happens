package dk.ek.shift_happens.department.neo4j;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Department")
@Getter
@Setter
@NoArgsConstructor
public class DepartmentNode {

    @Id
    @GeneratedValue
    private Long id;

    private Integer departmentId;
    private String departmentName;
    private Boolean isActive;
}

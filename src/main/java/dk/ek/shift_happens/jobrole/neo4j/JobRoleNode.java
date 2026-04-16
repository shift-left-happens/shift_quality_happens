package dk.ek.shift_happens.jobrole.neo4j;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("JobRole")
@Getter
@Setter
@NoArgsConstructor
public class JobRoleNode {

    @Id
    @GeneratedValue
    private Long id;

    private Integer jobRoleId;
    private String roleName;
    private String jobRoleDescription;
    private Boolean isCertificationRequired;
}

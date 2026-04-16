package dk.ek.shift_happens.worklocation.neo4j;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("WorkLocation")
@Getter
@Setter
@NoArgsConstructor
public class WorkLocationNode {

    @Id
    @GeneratedValue
    private Long id;

    private Integer workLocationId;
    private String locationName;
    private String city;
    private String country;
    private String timezone;
    private Boolean isActive;
}

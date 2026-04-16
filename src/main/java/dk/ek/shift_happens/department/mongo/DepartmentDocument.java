package dk.ek.shift_happens.department.mongo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// MongoDB document for the 'departments' collection.
// Kept flat — departments are small reference data with no child entities to embed.
@Document(collection = "departments")
@Getter
@Setter
@NoArgsConstructor
public class DepartmentDocument {

    @Id
    private String id;

    private Integer departmentId;
    private String name;
    private Boolean isActive;
}

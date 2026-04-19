package dk.ek.shift_happens.jobrole.mongo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

// MongoDB document for the 'job_role' collection.
// Kept flat — job_role are small reference data with no child entities to embed.
@Document(collection = "job_role")
@Getter
@Setter
@NoArgsConstructor
public class JobRoleDocument {

    @Id
    private String id;

    private Integer jobRoleId;

    private String roleName;

    private String jobRoleDescription;

    private Boolean isCertificationRequired;
}

package dk.ek.shift_happens.userrole.mongo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// MongoDB document for the 'user_role' collection.
// Kept flat — user_role are small reference data with no child entities to embed.
@Document(collection = "user_role")
@Getter
@Setter
@NoArgsConstructor
public class UserRoleDocument {

    @Id
    private String id;

    private Integer userRoleId;
    private String userRoleName;
}

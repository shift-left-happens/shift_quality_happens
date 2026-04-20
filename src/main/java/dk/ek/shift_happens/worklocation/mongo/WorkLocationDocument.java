package dk.ek.shift_happens.worklocation.mongo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// MongoDB document for the 'work_location' collection.
// Kept flat — work_location are small reference data with no child entities to embed.
@Document(collection = "work_location")
@Getter
@Setter
@NoArgsConstructor
public class WorkLocationDocument {

    @Id
    private String id;

    private Integer workLocationId;
    private String locationName;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String country;
    private String timezone;
    private Boolean isActive;
}

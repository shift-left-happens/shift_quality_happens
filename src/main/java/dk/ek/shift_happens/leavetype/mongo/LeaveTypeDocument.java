package dk.ek.shift_happens.leavetype.mongo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

// MongoDB document for the 'leave_type' collection.
// Kept flat — leave_type are small reference data with no child entities to embed.
@Document(collection = "leave_type")
@Getter
@Setter
@NoArgsConstructor
public class LeaveTypeDocument {

    @Id
    private String id;
    private Integer leaveTypeId;
    private String leaveTypeName;
    private String leaveTypeDescription;
    private Boolean requiresApproval;
    private Boolean isPaidLeave;
}

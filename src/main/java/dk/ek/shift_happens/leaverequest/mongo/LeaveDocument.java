package dk.ek.shift_happens.leaverequest.mongo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// MongoDB document for the 'leave_requests' collection.
// One document per employee — all leave requests with embedded approvals.
// Denormalised from MySQL: leave_request + leave_approval + leave_type.
@Document(collection = "leave_requests")
@Getter
@Setter
@NoArgsConstructor
public class LeaveDocument {

    @Id
    private String id;

    private Integer employeeId;
    private List<LeaveRequestRef> requests;

    // --- embedded sub-documents ---

    @Getter @Setter @NoArgsConstructor
    public static class LeaveRequestRef {
        private Integer leaveRequestId;
        private Integer leaveTypeId;
        private String leaveTypeName;
        private LocalDate startDate;
        private LocalDate endDate;
        private String requestStatus;
        private String reason;
        private LocalDateTime requestedAt;
        private List<ApprovalRef> approvals;
    }

    @Getter @Setter @NoArgsConstructor
    public static class ApprovalRef {
        private Integer approverEmployeeId;
        private String decision;
        private String comment;
        private LocalDateTime decidedAt;
    }
}

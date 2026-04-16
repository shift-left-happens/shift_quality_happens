package dk.ek.shift_happens.shift.mongo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

// MongoDB document for the 'shifts' collection.
// Denormalised from MySQL: shift + shift_required_job_role + shift_assignment
//                          + shift_approval + shift_swap + shift_swap_approval.
// Everything needed to render a shift view is in one document.
@Document(collection = "shifts")
@Getter
@Setter
@NoArgsConstructor
public class ShiftDocument {

    @Id
    private String id;

    private Integer shiftId;
    private String shiftName;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String status;

    private DepartmentRef department;
    private WorkLocationRef workLocation;
    private List<RequiredRoleRef> requiredRoles;
    private List<AssignmentRef> assignments;
    private List<ApprovalRef> approvals;
    private List<SwapRequestRef> swapRequests;

    // --- embedded sub-documents ---

    @Getter @Setter @NoArgsConstructor
    public static class DepartmentRef {
        private Integer departmentId;
        private String name;
    }

    @Getter @Setter @NoArgsConstructor
    public static class WorkLocationRef {
        private Integer workLocationId;
        private String locationName;
    }

    @Getter @Setter @NoArgsConstructor
    public static class RequiredRoleRef {
        private Integer jobRoleId;
        private String roleName;
        private Integer requiredEmployeeCount;
    }

    @Getter @Setter @NoArgsConstructor
    public static class AssignmentRef {
        private Integer shiftAssignmentId;
        private Integer employeeId;
        private String employeeName;
        private String assignmentStatus;
        private LocalDateTime assignedAt;
        private LocalDateTime checkInAt;
        private LocalDateTime checkOutAt;
    }

    @Getter @Setter @NoArgsConstructor
    public static class ApprovalRef {
        private Integer shiftApprovalId;
        private Integer approverEmployeeId;
        private String decision;
        private String comment;
        private LocalDateTime decidedAt;
    }

    @Getter @Setter @NoArgsConstructor
    public static class SwapRequestRef {
        private Integer shiftSwapId;
        private Integer employeeFromId;
        private String employeeFromName;
        private Integer employeeToId;
        private String employeeToName;
        private String swapStatus;
        private String reason;
        private LocalDateTime requestedAt;
        private List<SwapApprovalRef> approvals;
    }

    @Getter @Setter @NoArgsConstructor
    public static class SwapApprovalRef {
        private Integer approverEmployeeId;
        private String decision;
        private String comment;
        private LocalDateTime decidedAt;
    }
}

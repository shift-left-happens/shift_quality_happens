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
    private Integer shiftId;
    private String shiftName;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String shiftStatus;

    private Department department;
    private WorkLocation workLocation;
    private List<RequiredJobRole> requiredJobRoles;
    private List<ShiftAssignment> shiftAssignments;

    // --- embedded sub-documents ---

    @Getter @Setter @NoArgsConstructor
    public static class Department {
        private Integer departmentId;
        private String departmentName;
    }

    @Getter @Setter @NoArgsConstructor
    public static class WorkLocation {
        private Integer workLocationId;
        private String locationName;
    }

    @Getter @Setter @NoArgsConstructor
    public static class RequiredJobRole {
        private Integer requiredEmployees;
        private List<JobRole> jobRoles;
    }

    @Getter @Setter @NoArgsConstructor
    public static class JobRole {
        private Integer jobRoleId;
        private String roleName;
    }

    @Getter @Setter @NoArgsConstructor
    public static class ShiftAssignment {
        private AssignedEmployee assignedEmployee;
        private String assignmentStatus;
        private LocalDateTime assignmentDate;
        private LocalDateTime checkInDate;
        private LocalDateTime checkOutDatetime;
        private List<ShiftApproval> shiftApprovals;
        private List<SwapRequest> swapRequests;
    }

    @Getter @Setter @NoArgsConstructor
    public static class AssignedEmployee {
        private Integer employeeId;
        private String firstName;
        private String lastName;
    }

    @Getter @Setter @NoArgsConstructor
    public static class ShiftApproval {
        private AssignedEmployee approverEmployee;
        private String decision;
        private String approvalComment;
        private LocalDateTime decisionDatetime;
    }

    @Getter @Setter @NoArgsConstructor
    public static class SwapRequest {
        private AssignedEmployee employeeFrom;
        private AssignedEmployee employeeTo;
        private String swapStatus;
        private LocalDateTime requestDatetime;
        private String reason;
        private List<SwapApproval> swapApprovals;
    }

    @Getter @Setter @NoArgsConstructor
    public static class SwapApproval {
        private AssignedEmployee approverEmployee;
        private String decision;
        private String swapComment;
        private LocalDateTime decisionDatetime;
    }
}

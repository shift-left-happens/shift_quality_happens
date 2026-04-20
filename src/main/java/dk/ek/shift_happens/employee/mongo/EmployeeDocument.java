package dk.ek.shift_happens.employee.mongo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// MongoDB document for the 'employees' collection.
// Denormalised from MySQL: employee + employee_contract + employee_job_role
//                          + work_location + user_role are all embedded.
// Reads are served from this single document with no joins required.
@Document(collection = "employees")
@Getter
@Setter
@NoArgsConstructor
public class EmployeeDocument {

    @Id
    private Integer employeeId;
    private String employeeNumber;

    private String firstName;
    private String lastName;

    private String email;
    private String loginPassword;
    private String phoneNumber;

    private LocalDate hireDate;
    private String employmentStatus;

    private WorkLocation primaryWorkLocation;
    private UserRole userRole;

    private List<EmployeeContract> employeeContracts;
    private List<JobRole> jobRoles;
    private List<LeaveRequest> leaveRequests;
    private List<LeaveLedgerEntry> leaveLedger;

    // --- sub documents ---

    @Getter @Setter @NoArgsConstructor
    public static class WorkLocation {
        private Integer workLocationId;
        private String locationName;
    }

    @Getter @Setter @NoArgsConstructor
    public static class UserRole {
        private Integer roleId;
        private String roleName;
    }

    @Getter @Setter @NoArgsConstructor
    public static class Department {
        private Integer departmentId;
        private String departmentName;
    }

    @Getter @Setter @NoArgsConstructor
    public static class EmployeeContract {
        private Department department;
        private String contractType;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer weeklyHours;
        private BigDecimal salaryAmount;
        private Boolean isActive;
    }

    @Getter @Setter @NoArgsConstructor
    public static class JobRole {
        private String jobRoleId;
        private String roleName;
        private LocalDate assignedDate;
        private LocalDate expiryDate;
        private String proficiencyLevel;
    }

    @Getter @Setter @NoArgsConstructor
    public static class LeaveRequest {
        private Integer leaveTypeId;
        private LocalDate startDate;
        private LocalDate endDate;
        private String requestStatus;
        private String reason;
        private LocalDateTime requestedDatetime;
        private List<Approval> approvals;
    }

    @Getter @Setter @NoArgsConstructor
    public static class Approval {
        private ApproverEmployee approverEmployee;
        private String decision;
        private String leaveComment;
        private LocalDateTime decisionDatetime;
    }

    @Getter @Setter @NoArgsConstructor
    public static class ApproverEmployee {
        private Integer employeeId;
        private String firstName;
        private String lastName;
    }

    @Getter @Setter @NoArgsConstructor
    public static class LeaveLedgerEntry {
        private Integer leaveTypeId;
        private BigDecimal changeAmountDays;
        private String transactionType;
        private String referenceEntityType;
        private Integer referenceEntityId;
        private LocalDateTime transactionDatetime;
    }
}

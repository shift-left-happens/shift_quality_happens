package dk.ek.shift_happens.employee.mongo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
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
    private String id;

    private Integer employeeId;
    private String employeeNumber;
    private Name name;
    private String email;
    private String phone;
    private String employmentStatus;
    private LocalDate hireDate;

    private DepartmentRef department;
    private List<WorkLocationRef> workLocations;
    private List<JobRoleRef> jobRoles;
    private UserRoleRef userRole;

    // --- embedded sub-documents ---

    @Getter @Setter @NoArgsConstructor
    public static class Name {
        private String first;
        private String last;
    }

    @Getter @Setter @NoArgsConstructor
    public static class DepartmentRef {
        private Integer departmentId;
        private String name;
    }

    @Getter @Setter @NoArgsConstructor
    public static class WorkLocationRef {
        private Integer workLocationId;
        private String locationName;
        private String city;
        private String country;
        private String timezone;
        private Boolean isPrimary;
    }

    @Getter @Setter @NoArgsConstructor
    public static class JobRoleRef {
        private Integer jobRoleId;
        private String roleName;
        private LocalDate assignedDate;
        private String proficiencyLevel;
    }

    @Getter @Setter @NoArgsConstructor
    public static class UserRoleRef {
        private Integer userRoleId;
        private String roleName;
    }
}

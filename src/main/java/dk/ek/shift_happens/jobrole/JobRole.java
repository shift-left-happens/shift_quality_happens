package dk.ek.shift_happens.jobrole;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "job_role")
public class JobRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_role_id")
    private Integer jobRoleId;

    @Column(name = "role_name", nullable = false)
    private String roleName;

    @Column(name = "job_role_description")
    private String jobRoleDescription;

    @Column(name = "is_certification_required")
    private Boolean isCertificationRequired;
}

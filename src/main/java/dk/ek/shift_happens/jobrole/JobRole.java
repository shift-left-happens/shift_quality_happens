package dk.ek.shift_happens.jobrole;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "jobrole")
public class JobRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "job_role_id")
    private Integer job_role_id;

    @Column(name = "role_name", nullable = false)
    private String role_name;

    @Column(name = "job_role_description")
    private String job_role_description;

    @Column(name = "is_certification_required")
    private Boolean is_certification_required;
}

package dk.ek.shift_happens.department;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "department") // use exact table name and schema
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Integer department_id;
    @Column(name = "department_name")
    private String department_name;
    @Column(name = "is_active")
    private Boolean is_active;
}

package dk.ek.shift_happens.worklocation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "worklocation")
public class WorkLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "work_location_id")
    private Integer work_location_id;

    @Column(name = "location_name")
    private String location_name;

    @Column(name = "address_line_1")
    private String address_line_1;

    @Column(name = "address_line_2")
    private String address_line_2;

    @Column(name = "city")
    private String city;

    @Column(name = "country")
    private String country;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "is_active")
    private Boolean is_active;
}

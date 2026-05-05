package dk.ek.shift_happens.shiftrequiredjobrole;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftRequiredJobRoleRepository extends JpaRepository<ShiftRequiredJobRole, Integer> {
    List<ShiftRequiredJobRole> findAll();
}

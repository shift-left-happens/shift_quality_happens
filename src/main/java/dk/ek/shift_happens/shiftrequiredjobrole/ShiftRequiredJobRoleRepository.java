package dk.ek.shift_happens.shiftrequiredjobrole;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftRequiredJobRoleRepository extends JpaRepository<ShiftRequiredJobRole, ShiftRequiredJobRoleId> {
    List<ShiftRequiredJobRole> findAll();
}

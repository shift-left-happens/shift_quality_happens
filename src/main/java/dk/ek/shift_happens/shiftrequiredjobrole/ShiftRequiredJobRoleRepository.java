package dk.ek.shift_happens.shiftrequiredjobrole;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftRequiredJobRoleRepository extends JpaRepository<ShiftRequiredJobRole, Integer> {
    List<ShiftRequiredJobRole> findAll();

    List<ShiftRequiredJobRole> findByShiftId(Integer shiftId);

    Optional<ShiftRequiredJobRole> findByShiftIdAndJobRoleId(Integer shiftId, Integer jobRoleId);

    boolean existsByShiftIdAndJobRoleId(Integer shiftId, Integer jobRoleId);
}

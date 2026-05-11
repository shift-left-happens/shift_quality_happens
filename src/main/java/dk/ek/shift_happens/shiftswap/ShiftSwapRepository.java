package dk.ek.shift_happens.shiftswap;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftSwapRepository extends JpaRepository<ShiftSwap, Integer> {
    List<ShiftSwap> findAll();

    List<ShiftSwap> findByEmployeeFromIdOrEmployeeToId(Integer employeeFromId, Integer employeeToId);

    List<ShiftSwap> findByOriginalShiftAssignmentIdAndSwapStatusIgnoreCase(
            Integer originalShiftAssignmentId, String swapStatus);
}

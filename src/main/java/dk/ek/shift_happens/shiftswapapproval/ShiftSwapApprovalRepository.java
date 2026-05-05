package dk.ek.shift_happens.shiftswapapproval;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftSwapApprovalRepository extends JpaRepository<ShiftSwapApproval, Integer> {
    List<ShiftSwapApproval> findAll();
}

package dk.ek.shift_happens.shiftswapapproval;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftSwapApprovalRepository extends JpaRepository<ShiftSwapApproval, Integer> {
    List<ShiftSwapApproval> findAll();
}

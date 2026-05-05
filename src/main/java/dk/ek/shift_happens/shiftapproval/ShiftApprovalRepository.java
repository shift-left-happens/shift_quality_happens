package dk.ek.shift_happens.shiftapproval;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftApprovalRepository extends JpaRepository<ShiftApproval, Integer> {
    List<ShiftApproval> findAll();
}

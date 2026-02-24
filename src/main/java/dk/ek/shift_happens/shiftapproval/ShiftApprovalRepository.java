package dk.ek.shift_happens.shiftapproval;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftApprovalRepository extends JpaRepository<ShiftApproval, Integer> {
    List<ShiftApproval> findAll();
}

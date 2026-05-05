package dk.ek.shift_happens.leaveledger;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaveLedgerRepository extends JpaRepository<LeaveLedger, Integer> {
    List<LeaveLedger> findAll();

    List<LeaveLedger> findByEmployeeId(Integer employeeId);
}

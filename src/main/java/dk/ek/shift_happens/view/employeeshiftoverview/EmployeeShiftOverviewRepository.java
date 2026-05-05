package dk.ek.shift_happens.view.employeeshiftoverview;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeShiftOverviewRepository extends JpaRepository<EmployeeShiftOverviewView, Integer> {

    List<EmployeeShiftOverviewView> findByEmployeeId(Integer employeeId);
}

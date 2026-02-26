package dk.ek.shift_happens.view.employeeshiftoverview;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeShiftOverviewRepository extends JpaRepository<EmployeeShiftOverviewView, Integer> {

    List<EmployeeShiftOverviewView> findByEmployeeId(Integer employeeId);
}

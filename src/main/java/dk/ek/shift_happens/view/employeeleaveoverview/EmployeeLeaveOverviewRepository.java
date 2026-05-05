package dk.ek.shift_happens.view.employeeleaveoverview;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeLeaveOverviewRepository extends JpaRepository<EmployeeLeaveOverviewView, Integer> {

    List<EmployeeLeaveOverviewView> findByEmployeeId(Integer employeeId);

    List<EmployeeLeaveOverviewView> findByRequestStatus(String requestStatus);

    List<EmployeeLeaveOverviewView> findByLeaveTypeName(String leaveTypeName);
}

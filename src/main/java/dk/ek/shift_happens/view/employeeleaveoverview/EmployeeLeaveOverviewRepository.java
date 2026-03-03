package dk.ek.shift_happens.view.employeeleaveoverview;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeLeaveOverviewRepository extends JpaRepository<EmployeeLeaveOverviewView, Integer> {

    List<EmployeeLeaveOverviewView> findByEmployeeId(Integer employeeId);

    List<EmployeeLeaveOverviewView> findByRequestStatus(String requestStatus);

    List<EmployeeLeaveOverviewView> findByLeaveTypeName(String leaveTypeName);
}

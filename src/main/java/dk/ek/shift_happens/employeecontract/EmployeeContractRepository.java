package dk.ek.shift_happens.employeecontract;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeContractRepository extends JpaRepository<EmployeeContract, Integer> {
    List<EmployeeContract> findAll();

    List<EmployeeContract> findByEmployeeId(Integer employeeId);
}

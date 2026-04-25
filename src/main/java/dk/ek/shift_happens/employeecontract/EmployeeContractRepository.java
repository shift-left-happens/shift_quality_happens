package dk.ek.shift_happens.employeecontract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeContractRepository extends JpaRepository<EmployeeContract, Integer> {
    List<EmployeeContract> findAll();
    List<EmployeeContract> findByEmployeeId(Integer employeeId);
}

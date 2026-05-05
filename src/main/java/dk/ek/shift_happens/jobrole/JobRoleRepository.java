package dk.ek.shift_happens.jobrole;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobRoleRepository extends JpaRepository<JobRole, Integer> {
    List<JobRole> findAll();
}

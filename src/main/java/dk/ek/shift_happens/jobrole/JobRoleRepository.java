package dk.ek.shift_happens.jobrole;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRoleRepository extends JpaRepository<JobRole, Integer> {
    List<JobRole> findAll();
}

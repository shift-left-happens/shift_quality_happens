package dk.ek.shift_happens.worklocation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkLocationRepository extends JpaRepository<WorkLocation, Integer> {
    List<WorkLocation> findAll();
}

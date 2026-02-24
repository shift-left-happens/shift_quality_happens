package dk.ek.shift_happens.worklocation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkLocationRepository extends JpaRepository<WorkLocation, Integer> {
    List<WorkLocation> findAll();
}

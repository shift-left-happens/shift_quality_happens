package dk.ek.shift_happens.shiftswap;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftSwapRepository extends JpaRepository<ShiftSwap, Integer> {
    List<ShiftSwap> findAll();
}

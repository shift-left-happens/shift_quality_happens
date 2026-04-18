package dk.ek.shift_happens.shiftswap.neo4j;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/neo4j/shiftswaps")
@RequiredArgsConstructor
public class ShiftSwapNeo4jController {

    private final ShiftSwapNeo4jRepository shiftSwapNeo4jRepository;

    @GetMapping
    public List<ShiftSwapNode> getAll() {
        return shiftSwapNeo4jRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShiftSwapNode> getById(@PathVariable Long id) {
        return shiftSwapNeo4jRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

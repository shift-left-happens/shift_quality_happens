package dk.ek.shift_happens.worklocation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/worklocations")
@RequiredArgsConstructor
public class WorkLocationController {

    private final WorkLocationRepository workLocationRepository;

    @GetMapping
    public List<WorkLocation> getWorkLocations() {
        return this.workLocationRepository.findAll();
    }
}

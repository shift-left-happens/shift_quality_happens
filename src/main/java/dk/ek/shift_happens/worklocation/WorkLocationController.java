package dk.ek.shift_happens.worklocation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/worklocations")
@RequiredArgsConstructor
public class WorkLocationController {

    private final WorkLocationRepository workLocationRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<WorkLocation> getAll() {
        return workLocationRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public WorkLocation getById(@PathVariable Integer id) {
        return workLocationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkLocation create(@RequestBody WorkLocation workLocation) {
        return workLocationRepository.save(workLocation);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public WorkLocation update(@PathVariable Integer id, @RequestBody WorkLocation details) {
        WorkLocation existing = workLocationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        existing.setLocationName(details.getLocationName());
        existing.setAddressLine1(details.getAddressLine1());
        existing.setAddressLine2(details.getAddressLine2());
        existing.setCity(details.getCity());
        existing.setCountry(details.getCountry());
        existing.setTimezone(details.getTimezone());
        existing.setIsActive(details.getIsActive());
        return workLocationRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        workLocationRepository.deleteById(id);
    }
}

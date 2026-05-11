package dk.ek.shift_happens.shiftswapapproval;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/shiftswapapprovals")
@RequiredArgsConstructor
public class ShiftSwapApprovalController {

    private final ShiftSwapApprovalService shiftSwapApprovalService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public List<ShiftSwapApproval> getShiftSwapApprovals() {
        return shiftSwapApprovalService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ShiftSwapApproval getShiftSwapApprovalById(@PathVariable Integer id) {
        return shiftSwapApprovalService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftSwapApproval createShiftSwapApproval(@RequestBody ShiftSwapApproval shiftSwapApproval) {
        return shiftSwapApprovalService.approve(shiftSwapApproval);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public ShiftSwapApproval updateShiftSwapApproval(
            @PathVariable Integer id, @RequestBody ShiftSwapApproval shiftSwapApprovalDetails) {
        return shiftSwapApprovalService
                .update(id, shiftSwapApprovalDetails)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval not found"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShiftSwapApproval(@PathVariable Integer id) {
        if (!shiftSwapApprovalService.delete(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}

package dk.ek.shift_happens.leaveledger;

import dk.ek.shift_happens.auth.AuthHelper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/leaveledgers")
@RequiredArgsConstructor
public class LeaveLedgerController {

    private final LeaveLedgerRepository leaveLedgerRepository;
    private final AuthHelper authHelper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<LeaveLedger> getAll(Authentication auth) {
        if (authHelper.isEmployee(auth)) {
            return leaveLedgerRepository.findByEmployeeId(authHelper.currentEmployeeId(auth));
        }
        return leaveLedgerRepository.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public LeaveLedger getById(@PathVariable Integer id, Authentication auth) {
        LeaveLedger ledger =
                leaveLedgerRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (authHelper.isEmployee(auth) && !ledger.getEmployeeId().equals(authHelper.currentEmployeeId(auth))) {
            throw authHelper.forbidden();
        }
        return ledger;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveLedger create(@RequestBody LeaveLedger ledger) {
        return leaveLedgerRepository.save(ledger);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    public LeaveLedger update(@PathVariable Integer id, @RequestBody LeaveLedger details) {
        LeaveLedger existing =
                leaveLedgerRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        existing.setEmployeeId(details.getEmployeeId());
        existing.setLeaveTypeId(details.getLeaveTypeId());
        existing.setChangeAmountDays(details.getChangeAmountDays());
        existing.setTransactionType(details.getTransactionType());
        existing.setReferenceEntityType(details.getReferenceEntityType());
        existing.setReferenceEntityId(details.getReferenceEntityId());
        existing.setTransactionDatetime(details.getTransactionDatetime());
        return leaveLedgerRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','MANAGER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        leaveLedgerRepository.deleteById(id);
    }
}

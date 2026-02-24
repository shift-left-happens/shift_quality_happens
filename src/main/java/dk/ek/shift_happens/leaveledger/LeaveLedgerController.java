package dk.ek.shift_happens.leaveledger;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/leaveledgers")
@RequiredArgsConstructor
public class LeaveLedgerController {

    private final LeaveLedgerRepository leaveLedgerRepository;

    @GetMapping
    public List<LeaveLedger> getLeaveLedgers() {
        return this.leaveLedgerRepository.findAll();
    }
}

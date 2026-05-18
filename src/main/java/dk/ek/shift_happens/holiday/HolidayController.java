package dk.ek.shift_happens.holiday;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only endpoint exposing upcoming public holidays from the external
 * Nager.Date service. Consumed by the dashboard's "Upcoming public holidays"
 * widget so managers can see which days to plan around.
 */
@RestController
@RequestMapping("/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Holiday> getUpcoming(
            @RequestParam(required = false) String countryCode, @RequestParam(required = false) Integer limit) {
        return holidayService.getUpcomingHolidays(countryCode, limit);
    }
}

package dk.ek.shift_happens.holiday;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dk.ek.shift_happens.auth.CustomUserDetailsService;
import dk.ek.shift_happens.auth.JwtService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web-layer tests for {@link HolidayController}.
 *
 * <p>Unlike {@code HolidayServiceTest} (which exercises the holiday logic), this
 * verifies the wiring around it: routing, query-parameter binding, JSON output,
 * the security rule, and how {@link HolidayExceptionHandler} maps failures to
 * HTTP status codes. The {@link HolidayService} itself is mocked.
 *
 * <p>The JWT collaborators ({@link JwtService}, {@link CustomUserDetailsService})
 * are mocked so the real {@code SecurityConfig} can load without a database, and
 * the secrets it validates on startup are supplied via {@link TestPropertySource}.
 */
@WebMvcTest(controllers = HolidayController.class, excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import(HolidayExceptionHandler.class)
@TestPropertySource(
        properties = {"jwt.secret=test-secret-test-secret-test-secret-0123456789", "security.pepper=test-pepper"})
class HolidayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HolidayService holidayService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private static Holiday holiday(String date, String name) {
        return new Holiday(LocalDate.parse(date), name, name, "DK", true);
    }

    @Test
    @WithMockUser
    void returnsHolidaysAsJsonWhenAuthenticated() throws Exception {
        when(holidayService.getUpcomingHolidays(any(), any()))
                .thenReturn(List.of(holiday("2026-06-05", "Constitution Day")));

        mockMvc.perform(get("/holidays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Constitution Day"))
                .andExpect(jsonPath("$[0].date").value("2026-06-05"));
    }

    @Test
    @WithMockUser
    void bindsQueryParametersToTheService() throws Exception {
        when(holidayService.getUpcomingHolidays(eq("US"), eq(3))).thenReturn(List.of());

        mockMvc.perform(get("/holidays").param("countryCode", "US").param("limit", "3"))
                .andExpect(status().isOk());

        verify(holidayService).getUpcomingHolidays("US", 3);
    }

    @Test
    @WithMockUser
    void passesNullsWhenParametersAreOmitted() throws Exception {
        when(holidayService.getUpcomingHolidays(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/holidays")).andExpect(status().isOk());

        verify(holidayService).getUpcomingHolidays(null, null);
    }

    @Test
    void rejectsUnauthenticatedRequestWith401() throws Exception {
        mockMvc.perform(get("/holidays")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void mapsInvalidCountryCodeTo400() throws Exception {
        when(holidayService.getUpcomingHolidays(any(), any()))
                .thenThrow(new IllegalArgumentException("countryCode must be a two-letter ISO country code"));

        mockMvc.perform(get("/holidays").param("countryCode", "Denmark"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("countryCode must be a two-letter ISO country code"));
    }

    @Test
    @WithMockUser
    void mapsUpstreamFailureTo503() throws Exception {
        when(holidayService.getUpcomingHolidays(any(), any()))
                .thenThrow(
                        new HolidayApiException("Could not reach the public holiday service.", new RuntimeException()));

        mockMvc.perform(get("/holidays"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Could not reach the public holiday service."));
    }
}

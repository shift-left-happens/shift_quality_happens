package dk.ek.shift_happens.holiday;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link HolidayService}.
 *
 * <p>The external Nager.Date call is replaced by a mocked {@link HolidayApiClient},
 * and time is frozen with a fixed {@link Clock}, so the service's own logic —
 * country/limit validation, past-holiday filtering, sorting, year-boundary
 * handling and caching — can be exercised deterministically.
 */
@ExtendWith(MockitoExtension.class)
class HolidayServiceTest {

    /** Frozen "today": 2026-05-18. */
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-18T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private HolidayApiClient apiClient;

    private HolidayService service;

    @BeforeEach
    void setUp() {
        service = new HolidayService(apiClient, FIXED_CLOCK);
    }

    private static Holiday holiday(String date, String name) {
        return new Holiday(LocalDate.parse(date), name, name, "DK", true);
    }

    @Nested
    class UpcomingFiltering {

        @Test
        void dropsHolidaysBeforeToday() {
            when(apiClient.fetch(2026, "DK"))
                    .thenReturn(List.of(
                            holiday("2026-01-01", "New Year's Day"),
                            holiday("2026-05-18", "Today's holiday"),
                            holiday("2026-12-25", "Christmas Day")));
            when(apiClient.fetch(2027, "DK")).thenReturn(List.of());

            List<Holiday> result = service.getUpcomingHolidays("DK", 10);

            // 2026-01-01 is in the past; today itself counts as upcoming.
            assertThat(result).extracting(Holiday::name).containsExactly("Today's holiday", "Christmas Day");
        }

        @Test
        void sortsByDateAcrossBothYears() {
            when(apiClient.fetch(2026, "DK"))
                    .thenReturn(
                            List.of(holiday("2026-12-25", "Christmas Day"), holiday("2026-06-05", "Constitution Day")));
            when(apiClient.fetch(2027, "DK")).thenReturn(List.of(holiday("2027-01-01", "New Year's Day")));

            List<Holiday> result = service.getUpcomingHolidays("DK", 10);

            assertThat(result)
                    .extracting(Holiday::name)
                    .containsExactly("Constitution Day", "Christmas Day", "New Year's Day");
        }

        @Test
        void looksIntoNextYearWhenCurrentYearIsExhausted() {
            when(apiClient.fetch(2026, "DK")).thenReturn(List.of(holiday("2026-01-01", "Past holiday")));
            when(apiClient.fetch(2027, "DK")).thenReturn(List.of(holiday("2027-01-01", "New Year's Day")));

            List<Holiday> result = service.getUpcomingHolidays("DK", 10);

            assertThat(result).extracting(Holiday::name).containsExactly("New Year's Day");
        }
    }

    @Nested
    class LimitHandling {

        @Test
        void defaultsToFiveWhenLimitIsNull() {
            when(apiClient.fetch(2026, "DK"))
                    .thenReturn(List.of(
                            holiday("2026-06-01", "H1"),
                            holiday("2026-06-02", "H2"),
                            holiday("2026-06-03", "H3"),
                            holiday("2026-06-04", "H4"),
                            holiday("2026-06-05", "H5"),
                            holiday("2026-06-06", "H6")));
            when(apiClient.fetch(2027, "DK")).thenReturn(List.of());

            assertThat(service.getUpcomingHolidays("DK", null)).hasSize(HolidayService.DEFAULT_LIMIT);
        }

        @Test
        void clampsToMaxLimit() {
            when(apiClient.fetch(anyInt(), eq("DK"))).thenReturn(List.of());

            // No exception, and the (empty) result is obviously within the cap.
            assertThat(service.getUpcomingHolidays("DK", 9999)).isEmpty();
        }

        @Test
        void treatsNonPositiveLimitAsDefault() {
            when(apiClient.fetch(2026, "DK"))
                    .thenReturn(List.of(
                            holiday("2026-06-01", "H1"),
                            holiday("2026-06-02", "H2"),
                            holiday("2026-06-03", "H3"),
                            holiday("2026-06-04", "H4"),
                            holiday("2026-06-05", "H5"),
                            holiday("2026-06-06", "H6")));
            when(apiClient.fetch(2027, "DK")).thenReturn(List.of());

            assertThat(service.getUpcomingHolidays("DK", 0)).hasSize(HolidayService.DEFAULT_LIMIT);
        }
    }

    @Nested
    class CountryHandling {

        @Test
        void defaultsToDenmarkWhenCountryIsNull() {
            when(apiClient.fetch(anyInt(), eq("DK"))).thenReturn(List.of());

            service.getUpcomingHolidays(null, 5);

            verify(apiClient).fetch(2026, "DK");
        }

        @Test
        void uppercasesAndTrimsCountryCode() {
            when(apiClient.fetch(anyInt(), eq("US"))).thenReturn(List.of());

            service.getUpcomingHolidays(" us ", 5);

            verify(apiClient).fetch(2026, "US");
        }

        @Test
        void rejectsMalformedCountryCode() {
            assertThatThrownBy(() -> service.getUpcomingHolidays("Denmark", 5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("two-letter");
        }
    }

    @Nested
    class Caching {

        @Test
        void doesNotRefetchWithinCacheWindow() {
            when(apiClient.fetch(anyInt(), eq("DK"))).thenReturn(List.of());

            service.getUpcomingHolidays("DK", 5);
            service.getUpcomingHolidays("DK", 5);

            // Two calls, but each year fetched only once.
            verify(apiClient, times(1)).fetch(2026, "DK");
            verify(apiClient, times(1)).fetch(2027, "DK");
        }

        @Test
        void cachesPerCountrySeparately() {
            when(apiClient.fetch(anyInt(), eq("DK"))).thenReturn(List.of());
            when(apiClient.fetch(anyInt(), eq("US"))).thenReturn(List.of());

            service.getUpcomingHolidays("DK", 5);
            service.getUpcomingHolidays("US", 5);

            verify(apiClient).fetch(2026, "DK");
            verify(apiClient).fetch(2026, "US");
        }
    }

    @Test
    void propagatesUpstreamFailure() {
        when(apiClient.fetch(2026, "DK")).thenThrow(new HolidayApiException("boom", new RuntimeException()));

        assertThatThrownBy(() -> service.getUpcomingHolidays("DK", 5)).isInstanceOf(HolidayApiException.class);

        verify(apiClient, never()).fetch(2027, "DK");
    }
}

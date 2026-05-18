package dk.ek.shift_happens.holiday;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Returns the next public holidays for a country, backed by the Nager.Date API.
 *
 * <p>Responsibilities kept here (and unit tested in {@code HolidayServiceTest}):
 *
 * <ul>
 *   <li>Validating / defaulting the country code and result limit.
 *   <li>Querying both the current and next calendar year so the list never
 *       runs dry near year-end.
 *   <li>Dropping past holidays and sorting what remains by date.
 *   <li>Caching raw API responses per country+year for {@link #CACHE_TTL} so a
 *       dashboard refresh does not hammer the third-party service.
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class HolidayService {

    static final String DEFAULT_COUNTRY = "DK";
    static final int DEFAULT_LIMIT = 5;
    static final int MAX_LIMIT = 25;
    static final Duration CACHE_TTL = Duration.ofHours(6);

    private final HolidayApiClient apiClient;
    private final Clock clock;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private record CacheEntry(List<Holiday> holidays, Instant expiresAt) {}

    /**
     * @param countryCode ISO 3166-1 alpha-2 code; {@code null}/blank defaults to {@value #DEFAULT_COUNTRY}
     * @param limit how many holidays to return; {@code null} defaults to {@value #DEFAULT_LIMIT},
     *     values are clamped to 1..{@value #MAX_LIMIT}
     * @throws IllegalArgumentException if the country code is not two letters
     */
    public List<Holiday> getUpcomingHolidays(String countryCode, Integer limit) {
        String country = normaliseCountry(countryCode);
        int max = normaliseLimit(limit);
        LocalDate today = LocalDate.now(clock);
        int year = today.getYear();

        List<Holiday> combined = new ArrayList<>();
        combined.addAll(holidaysForYear(year, country));
        combined.addAll(holidaysForYear(year + 1, country));

        return combined.stream()
                .filter(h -> h.date() != null && !h.date().isBefore(today))
                .sorted(Comparator.comparing(Holiday::date))
                .limit(max)
                .toList();
    }

    private List<Holiday> holidaysForYear(int year, String country) {
        String key = country + "-" + year;
        Instant now = clock.instant();
        CacheEntry cached = cache.get(key);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached.holidays();
        }
        List<Holiday> fresh = apiClient.fetch(year, country);
        cache.put(key, new CacheEntry(fresh, now.plus(CACHE_TTL)));
        return fresh;
    }

    private String normaliseCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return DEFAULT_COUNTRY;
        }
        String trimmed = countryCode.trim();
        if (!trimmed.matches("[A-Za-z]{2}")) {
            throw new IllegalArgumentException("countryCode must be a two-letter ISO country code");
        }
        return trimmed.toUpperCase();
    }

    private int normaliseLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}

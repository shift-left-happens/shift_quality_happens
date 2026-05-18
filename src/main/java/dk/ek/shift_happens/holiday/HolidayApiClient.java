package dk.ek.shift_happens.holiday;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Thin wrapper around the outbound HTTP call to the Nager.Date API.
 *
 * <p>Keeping the raw HTTP here (rather than in {@link HolidayService}) means the
 * service's date-filtering and caching logic can be unit tested with a plain
 * mock, without standing up an HTTP stack.
 */
@Component
public class HolidayApiClient {

    static final String BASE_URL = "https://date.nager.at/api/v3";

    private final RestClient restClient;

    public HolidayApiClient(RestClient.Builder builder) {
        this.restClient = builder.baseUrl(BASE_URL).build();
    }

    /**
     * Fetches every public holiday for the given year and ISO country code.
     *
     * @throws HolidayApiException if the service is unreachable or errors
     */
    public List<Holiday> fetch(int year, String countryCode) {
        try {
            Holiday[] body = restClient
                    .get()
                    .uri("/PublicHolidays/{year}/{country}", year, countryCode)
                    .retrieve()
                    .body(Holiday[].class);
            return body == null ? List.of() : List.of(body);
        } catch (RestClientException ex) {
            throw new HolidayApiException("Could not reach the public holiday service. Please try again later.", ex);
        }
    }
}

package dk.ek.shift_happens.holiday;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Unit tests for {@link HolidayApiClient}.
 *
 * <p>Where {@link HolidayServiceTest} mocks this client away, these tests do the
 * opposite: a {@link MockRestServiceServer} is bound to a real {@link RestClient}
 * so the production code's actual JSON deserialization path — raw HTTP body to
 * {@code Holiday[]} via Jackson — is exercised against canned Nager.Date responses.
 */
class HolidayApiClientTest {

    private MockRestServiceServer server;
    private HolidayApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new HolidayApiClient(builder);
    }

    @Test
    void parsesRealisticNagerResponseIncludingUnmappedFields() {
        // Body shaped like a genuine Nager.Date response: the extra fields
        // (fixed, counties, launchYear, types) are NOT mapped onto Holiday and
        // must be swallowed by @JsonIgnoreProperties(ignoreUnknown = true).
        String json =
                """
                [
                  {
                    "date": "2026-06-05",
                    "localName": "Grundlovsdag",
                    "name": "Constitution Day",
                    "countryCode": "DK",
                    "fixed": false,
                    "global": true,
                    "counties": null,
                    "launchYear": null,
                    "types": ["Public"]
                  },
                  {
                    "date": "2026-12-25",
                    "localName": "1. juledag",
                    "name": "Christmas Day",
                    "countryCode": "DK",
                    "fixed": true,
                    "global": true,
                    "counties": null,
                    "launchYear": 1900,
                    "types": ["Public"]
                  }
                ]
                """;
        server.expect(requestTo(HolidayApiClient.BASE_URL + "/PublicHolidays/2026/DK"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        List<Holiday> result = client.fetch(2026, "DK");

        server.verify();
        assertThat(result).hasSize(2);
        assertThat(result.getFirst())
                .isEqualTo(new Holiday(LocalDate.of(2026, 6, 5), "Grundlovsdag", "Constitution Day", "DK", true));
        assertThat(result)
                .extracting(Holiday::name)
                .containsExactly("Constitution Day", "Christmas Day");
    }

    @Test
    void buildsUrlFromYearAndCountryPathVariables() {
        server.expect(requestTo(HolidayApiClient.BASE_URL + "/PublicHolidays/2030/US"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        client.fetch(2030, "US");

        // verify() fails if the URL/method did not match the single expectation.
        server.verify();
    }

    @Test
    void returnsEmptyListForEmptyJsonArray() {
        server.expect(requestTo(HolidayApiClient.BASE_URL + "/PublicHolidays/2026/DK"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.fetch(2026, "DK")).isEmpty();
    }

    @Test
    void returnsEmptyListWhenResponseHasNoBody() {
        // 204 yields a null body from RestClient — exercises the
        // `body == null ? List.of()` guard in HolidayApiClient.
        server.expect(requestTo(HolidayApiClient.BASE_URL + "/PublicHolidays/2026/DK"))
                .andRespond(withNoContent());

        assertThat(client.fetch(2026, "DK")).isEmpty();
    }

    @Test
    void wrapsHttpErrorInHolidayApiException() {
        server.expect(requestTo(HolidayApiClient.BASE_URL + "/PublicHolidays/2026/DK"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.fetch(2026, "DK"))
                .isInstanceOf(HolidayApiException.class)
                .hasMessageContaining("Could not reach the public holiday service");
    }

    @Test
    void wrapsMalformedJsonInHolidayApiException() {
        server.expect(requestTo(HolidayApiClient.BASE_URL + "/PublicHolidays/2026/DK"))
                .andRespond(withSuccess("{ this is not a holiday array", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.fetch(2026, "DK")).isInstanceOf(HolidayApiException.class);
    }
}

package dk.ek.shift_happens.holiday;

/**
 * Raised when the external Nager.Date public-holiday service cannot be reached
 * or returns an unexpected response. Mapped to HTTP 503 by
 * {@link HolidayExceptionHandler} so a failing third party never takes the
 * rest of the dashboard down with it.
 */
public class HolidayApiException extends RuntimeException {
    public HolidayApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

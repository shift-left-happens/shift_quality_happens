package dk.ek.shift_happens.holiday;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

/**
 * A single public holiday as returned by the Nager.Date API
 * (https://date.nager.at). Only the fields the frontend widget needs are
 * mapped; any extra fields in the response are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Holiday(LocalDate date, String localName, String name, String countryCode, boolean global) {}

import { apiRequest } from './client';
import type { Holiday } from './types';

/**
 * Upcoming public holidays, sourced from the backend's Nager.Date proxy
 * (`GET /holidays`). Defaults to Denmark, five results.
 */
export function listUpcomingHolidays(
  countryCode = 'DK',
  limit = 5
): Promise<Holiday[]> {
  const params = new URLSearchParams({
    countryCode,
    limit: String(limit),
  });
  return apiRequest<Holiday[]>(`/holidays?${params.toString()}`);
}

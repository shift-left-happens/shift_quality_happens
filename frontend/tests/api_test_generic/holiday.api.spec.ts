import { test, expect } from '@playwright/test';
import { loginAndGetToken, authHeaders, DEFAULT_ADMIN_EMAIL, DEFAULT_PASSWORD } from '../pages/helper/api-helpers';

test.describe('Holiday API', () => {
    let adminToken: string;
    const API_URL = process.env.API_URL || 'http://localhost:8080';
    const external_url = 'https://date.nager.at/api/v3';

    test.beforeAll(async ({ request }) => {
        // Log in as admin to get a token
        const email = process.env.TEST_ADMIN_EMAIL || DEFAULT_ADMIN_EMAIL;
        const password = process.env.TEST_USER_PASSWORD || DEFAULT_PASSWORD;
        const loginResponse = await loginAndGetToken(request, email, password);
        adminToken = loginResponse.token;
        expect(adminToken).toBeDefined();
    });


    const testCases = [
        {
            name: 'Nager.at',
            headers: () => ({}), //No token needed
            url: `${external_url}/PublicHolidays/2026/DK`,
        },
        {
            name: 'Shift happens',
            headers: (token: string) => authHeaders(token),
            url: `${API_URL}/holidays`,
        },
    ];

    for (const tc of testCases) {
        test(`should fetch upcoming holidays - ${tc.name}`, async ({ request }) => {
            const headers = tc.headers(adminToken);

            const response = await request.get(tc.url, {
                headers
            });

            expect(response.status()).toBe(200);

            const holidays = await response.json();
            expect(Array.isArray(holidays)).toBe(true);
        });
    }

    test('should fetch upcoming holidays without parameters', async ({ request }) => {
        const response = await request.get(`${API_URL}/holidays`, {
            headers: authHeaders(adminToken)
        });

        expect(response.status()).toBe(200);
        const holidays = await response.json();
        expect(Array.isArray(holidays)).toBe(true);
        
        // If there are holidays, check structure
        if (holidays.length > 0) {
            expect(holidays[0]).toHaveProperty('date');
            expect(holidays[0]).toHaveProperty('localName');
            expect(holidays[0]).toHaveProperty('name');
            expect(holidays[0]).toHaveProperty('countryCode');
        }
    });

    test('should fetch upcoming holidays with countryCode and limit', async ({ request }) => {
        const countryCode = 'DK';
        const limit = 5;
        const response = await request.get(`${API_URL}/holidays`, {
            params: {
                countryCode,
                limit
            },
            headers: authHeaders(adminToken)
        });

        expect(response.status()).toBe(200);
        const holidays = await response.json();
        expect(Array.isArray(holidays)).toBe(true);
        
        if (holidays.length > 0) {
            expect(holidays.length).toBeLessThanOrEqual(limit);
            for (const holiday of holidays) {
                expect(holiday.countryCode).toBe(countryCode);
            }
        }
    });

    test('should reject request without authentication', async ({ request }) => {
        const response = await request.get(`${API_URL}/holidays`);
        // SecurityConfig or PreAuthorize should return 401 or 403
        expect([401, 403]).toContain(response.status());
    });
});

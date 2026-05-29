import { test, expect } from '@playwright/test';
import { API_URL, login, authHeaders, DEFAULT_ADMIN_EMAIL, DEFAULT_PASSWORD, fmt } from '../pages/helper/api-helpers';
import { buildEmployeePayload } from '../pages/helper/employee-helpers';

test.describe('ShiftSwap Extra API', () => {
  let adminToken: string;
  const api_url = API_URL;

  test.beforeAll(async ({ request }) => {
    const loginResponse = await login(request, DEFAULT_ADMIN_EMAIL, DEFAULT_PASSWORD);
    adminToken = loginResponse.token;
  });

  function authHeader() {
    return authHeaders(adminToken);
  }

  test('should perform PUT update on ShiftSwap', async ({ request }) => {
    // 1. Setup: Create two temporary employees, a shift, and an assignment
    let empAId: number | undefined;
    let empBId: number | undefined;
    let shiftId: number | undefined;
    let assignId: number | undefined;
    let swapId: number | undefined;

    try {
      // Create Employee A (From)
      const empARes = await request.post(`${api_url}/employees`, {
        headers: authHeader(),
        data: buildEmployeePayload({ email: `emp.a.${Date.now()}@hospital.dk`, employeeNumber: `EMPA-${Date.now()}` })
      });
      expect(empARes.status()).toBe(201);
      const empA = await empARes.json();
      empAId = empA.employeeId;

      // Create Employee B (To)
      const empBRes = await request.post(`${api_url}/employees`, {
        headers: authHeader(),
        data: buildEmployeePayload({ email: `emp.b.${Date.now()}@hospital.dk`, employeeNumber: `EMPB-${Date.now()}` })
      });
      expect(empBRes.status()).toBe(201);
      const empB = await empBRes.json();
      empBId = empB.employeeId;

      // Create a Shift
      const start = new Date(Date.now() + 10 * 24 * 60 * 60 * 1000); // 10 days future
      const end = new Date(start.getTime() + 8 * 60 * 60 * 1000);
      
      // Fetch valid department and location first to be safe
      const deptRes = await request.get(`${api_url}/departments`, { headers: authHeader() });
      const depts = await deptRes.json();
      const deptId = depts[0].departmentId;

      const locRes = await request.get(`${api_url}/worklocations`, { headers: authHeader() });
      const locs = await locRes.json();
      const locId = locs[0].workLocationId;

      const shiftRes = await request.post(`${api_url}/shifts`, {
        headers: authHeader(),
        data: {
          workLocationId: locId,
          departmentId: deptId,
          startDatetime: fmt(start),
          endDatetime: fmt(end),
          shiftStatus: 'Open',
          shiftName: 'API Test Shift'
        }
      });
      expect(shiftRes.status()).toBe(201);
      const shift = await shiftRes.json();
      shiftId = shift.shiftId;

      // Create an Assignment for Employee A
      const assignRes = await request.post(`${api_url}/shiftassignments`, {
        headers: authHeader(),
        data: {
          shiftId: shiftId,
          employeeId: empAId,
          assignmentStatus: 'Assigned',
          assignedDatetime: fmt(new Date(Date.now() - 10000))
        }
      });
      expect(assignRes.status()).toBe(201);
      const assign = await assignRes.json();
      assignId = assign.shiftAssignmentId;

      // Create a Pending ShiftSwap
      const swapPayload = {
        employeeFromId: empAId,
        employeeToId: empBId,
        originalShiftAssignmentId: assignId,
        reason: 'Testing PUT update',
        requestDatetime: fmt(new Date(Date.now() - 5000)),
        swapStatus: 'Pending'
      };
      const swapRes = await request.post(`${api_url}/shiftswaps`, {
        headers: authHeader(),
        data: swapPayload
      });
      expect(swapRes.status()).toBe(201);
      const swap = await swapRes.json();
      swapId = swap.shiftSwapId;

      // 2. Perform the actual PUT update
      // Note: swapStatus cannot stay the same (backend rejects same-status transitions),
      // so we explicitly set a new status and update the reason.
      const updatePayload = {
        ...swap,
        reason: 'Testing PUT update - UPDATED',
        swapStatus: 'Approved'
      };

      const putRes = await request.put(`${api_url}/shiftswaps/${swapId}`, {
        headers: authHeader(),
        data: updatePayload
      });
      
      expect(putRes.status()).toBe(200);
      if (putRes.status() !== 200) {
        console.log(`[DEBUG_LOG] PUT /shiftswaps/${swapId} failed with ${putRes.status()}`);
        console.log(`[DEBUG_LOG] Response: ${await putRes.text()}`);
      }
      const updated = await putRes.json();
      expect(updated.reason).toBe('Testing PUT update - UPDATED');

    } finally {
      // Cleanup in correct hierarchical order
      if (swapId) {
        await request.delete(`${api_url}/shiftswaps/${swapId}`, { headers: authHeader() });
      }
      if (assignId) {
        await request.delete(`${api_url}/shiftassignments/${assignId}`, { headers: authHeader() });
      }
      if (shiftId) {
        await request.delete(`${api_url}/shifts/${shiftId}`, { headers: authHeader() });
      }
      if (empAId) {
        await request.delete(`${api_url}/employees/${empAId}`, { headers: authHeader() });
      }
      if (empBId) {
        await request.delete(`${api_url}/employees/${empBId}`, { headers: authHeader() });
      }
    }
  });
});

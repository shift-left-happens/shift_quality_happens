import { test, expect } from '@playwright/test';
import { API_URL, login, authHeaders, DEFAULT_ADMIN_EMAIL, DEFAULT_PASSWORD, fmt } from '../pages/helper/api-helpers';
import { buildEmployeePayload } from '../pages/helper/employee-helpers';

test.describe('ShiftSwapApproval API', () => {
  let adminToken: string;
  let adminEmployeeId: number;
  const api_url = API_URL;

  test.beforeAll(async ({ request }) => {
    const loginResponse = await login(request, DEFAULT_ADMIN_EMAIL, DEFAULT_PASSWORD);
    adminToken = loginResponse.token;
    adminEmployeeId = loginResponse.employeeId;
  });

  function authHeader() {
    return authHeaders(adminToken);
  }

  test('should perform full ShiftSwapApproval lifecycle', async ({ request }) => {
    // 1. Setup: Create employees, shift, assignment, and swap
    let empAId: number | undefined;
    let empBId: number | undefined;
    let shiftId: number | undefined;
    let assignId: number | undefined;
    let swapId: number | undefined;
    let approvalId: number | undefined;

    try {
      // Create Employee A (From)
      const empARes = await request.post(`${api_url}/employees`, {
        headers: authHeader(),
        data: buildEmployeePayload({ email: `approval.emp.a.${Date.now()}@hospital.dk`, employeeNumber: `APPA-${Date.now()}` })
      });
      expect(empARes.status()).toBe(201);
      const empA = await empARes.json();
      empAId = empA.employeeId;

      // Create Employee B (To)
      const empBRes = await request.post(`${api_url}/employees`, {
        headers: authHeader(),
        data: buildEmployeePayload({ email: `approval.emp.b.${Date.now()}@hospital.dk`, employeeNumber: `APPB-${Date.now()}` })
      });
      expect(empBRes.status()).toBe(201);
      const empB = await empBRes.json();
      empBId = empB.employeeId;

      // Create a Shift
      const start = new Date(Date.now() + 15 * 24 * 60 * 60 * 1000); 
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
          shiftName: 'Approval Test Shift'
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
        reason: 'Testing Approval lifecycle',
        requestDatetime: fmt(new Date(Date.now() - 10000)),
        swapStatus: 'Pending'
      };
      const swapRes = await request.post(`${api_url}/shiftswaps`, {
        headers: authHeader(),
        data: swapPayload
      });
      expect(swapRes.status()).toBe(201);
      const swap = await swapRes.json();
      swapId = swap.shiftSwapId;

      // 2. Create Approval (POST)
      const createPayload = {
        shiftSwapId: swapId,
        approverEmployeeId: adminEmployeeId,
        decision: 'Approved',
        shiftSwapComment: 'API Test Approval',
        decisionDatetime: fmt(new Date(Date.now() - 5000))
      };

      const createRes = await request.post(`${api_url}/shiftswapapprovals`, {
        headers: authHeader(),
        data: createPayload
      });
      expect(createRes.status()).toBe(201);
      const created = await createRes.json();
      approvalId = created.shiftSwapApprovalId;

      // 3. Read (GET all & GET by ID)
      const getAllRes = await request.get(`${api_url}/shiftswapapprovals`, { headers: authHeader() });
      expect(getAllRes.status()).toBe(200);
      
      const getByIdRes = await request.get(`${api_url}/shiftswapapprovals/${approvalId}`, { headers: authHeader() });
      expect(getByIdRes.status()).toBe(200);

      // 4. Update (PUT)
      const updatePayload = {
        ...created,
        shiftSwapComment: 'Updated Comment'
      };
      const updateRes = await request.put(`${api_url}/shiftswapapprovals/${approvalId}`, {
        headers: authHeader(),
        data: updatePayload
      });
      expect(updateRes.status()).toBe(200);

      // 5. Delete (DELETE)
      const deleteRes = await request.delete(`${api_url}/shiftswapapprovals/${approvalId}`, { headers: authHeader() });
      expect(deleteRes.status()).toBe(204);

      // 6. Verify Delete
      const verifyRes = await request.get(`${api_url}/shiftswapapprovals/${approvalId}`, { headers: authHeader() });
      expect([404, 401]).toContain(verifyRes.status());

    } finally {
      // Cleanup in correct hierarchical order
      if (approvalId) {
        await request.delete(`${api_url}/shiftswapapprovals/${approvalId}`, { headers: authHeader() });
      }
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

package dk.ek.shift_happens.leaveapproval;

import dk.ek.shift_happens.employee.Employee;
import dk.ek.shift_happens.employee.EmployeeRepository;
import dk.ek.shift_happens.leaverequest.LeaveRequest;
import dk.ek.shift_happens.leaverequest.LeaveRequestRepository;
import dk.ek.shift_happens.userrole.UserRole;
import dk.ek.shift_happens.userrole.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveApprovalServiceTest {

    @Mock
    private LeaveApprovalRepository leaveApprovalRepository;
    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private UserRoleRepository userRoleRepository;

    @InjectMocks
    private LeaveApprovalService leaveApprovalService;

    @Test
    void approve_allowsAdministrator() {
        LeaveRequest request = pendingRequest();
        Employee approver = employeeWithRole(1);
        UserRole role = roleNamed(1, "Administrator");
        LeaveApproval approval = new LeaveApproval();
        approval.setLeaveRequestId(5);
        approval.setApproverEmployeeId(99);
        approval.setDecision("APPROVED");

        when(leaveRequestRepository.findById(5)).thenReturn(Optional.of(request));
        when(employeeRepository.findById(99)).thenReturn(Optional.of(approver));
        when(userRoleRepository.findById(1)).thenReturn(Optional.of(role));
        when(leaveApprovalRepository.save(any(LeaveApproval.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveApproval saved = leaveApprovalService.approve(approval);

        assertNotNull(saved.getDecisionDatetime());
        assertEquals("APPROVED", request.getRequestStatus());
        verify(leaveApprovalRepository).save(approval);
        verify(leaveRequestRepository).save(request);
    }

    @Test
    void approve_allowsManager() {
        LeaveRequest request = pendingRequest();
        Employee approver = employeeWithRole(3);
        UserRole role = roleNamed(3, "Manager");
        LeaveApproval approval = new LeaveApproval();
        approval.setLeaveRequestId(5);
        approval.setApproverEmployeeId(100);
        approval.setDecision("REJECTED");

        when(leaveRequestRepository.findById(5)).thenReturn(Optional.of(request));
        when(employeeRepository.findById(100)).thenReturn(Optional.of(approver));
        when(userRoleRepository.findById(3)).thenReturn(Optional.of(role));
        when(leaveApprovalRepository.save(any(LeaveApproval.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveApproval saved = leaveApprovalService.approve(approval);

        assertEquals("REJECTED", saved.getDecision());
        assertEquals("REJECTED", request.getRequestStatus());
    }

    @Test
    void approve_rejectsRegularEmployee() {
        LeaveRequest request = pendingRequest();
        Employee approver = employeeWithRole(2);
        UserRole role = roleNamed(2, "Employee");
        LeaveApproval approval = new LeaveApproval();
        approval.setLeaveRequestId(5);
        approval.setApproverEmployeeId(101);
        approval.setDecision("APPROVED");

        when(leaveRequestRepository.findById(5)).thenReturn(Optional.of(request));
        when(employeeRepository.findById(101)).thenReturn(Optional.of(approver));
        when(userRoleRepository.findById(2)).thenReturn(Optional.of(role));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> leaveApprovalService.approve(approval));

        assertTrue(ex.getMessage().contains("Administrator") || ex.getMessage().contains("Manager"));
        verify(leaveApprovalRepository, never()).save(any());
    }

    private LeaveRequest pendingRequest() {
        LeaveRequest request = new LeaveRequest();
        request.setLeaveRequestId(5);
        request.setEmployeeId(10);
        request.setLeaveTypeId(2);
        request.setStartDate(LocalDate.of(2026, 5, 1));
        request.setEndDate(LocalDate.of(2026, 5, 3));
        request.setRequestStatus("PENDING");
        return request;
    }

    private Employee employeeWithRole(Integer roleId) {
        Employee employee = new Employee();
        employee.setEmployeeId(99);
        employee.setFkUserRoleId(roleId);
        employee.setEmail("approver@example.com");
        employee.setLoginPassword("secret123");
        return employee;
    }

    private UserRole roleNamed(Integer id, String name) {
        UserRole role = new UserRole();
        role.setUserRoleId(id);
        role.setUserRoleName(name);
        return role;
    }
}

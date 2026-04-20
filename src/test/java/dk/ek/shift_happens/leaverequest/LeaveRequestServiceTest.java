package dk.ek.shift_happens.leaverequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    @Test
    void create_setsPendingStatusAndTimestamp() {
        LeaveRequest request = new LeaveRequest();
        request.setEmployeeId(10);
        request.setLeaveTypeId(2);
        request.setStartDate(LocalDate.of(2026, 5, 1));
        request.setEndDate(LocalDate.of(2026, 5, 3));
        request.setReason("Family trip");

        when(leaveRequestRepository.save(any(LeaveRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LeaveRequest saved = leaveRequestService.create(request);

        assertEquals("PENDING", saved.getRequestStatus());
        assertNotNull(saved.getRequestedDatetime());
        verify(leaveRequestRepository).save(request);
    }

    @Test
    void create_rejectsInvalidDateRange() {
        LeaveRequest request = new LeaveRequest();
        request.setEmployeeId(10);
        request.setLeaveTypeId(2);
        request.setStartDate(LocalDate.of(2026, 5, 10));
        request.setEndDate(LocalDate.of(2026, 5, 3));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> leaveRequestService.create(request));

        assertTrue(ex.getMessage().contains("start"));
        verify(leaveRequestRepository, never()).save(any());
    }
}

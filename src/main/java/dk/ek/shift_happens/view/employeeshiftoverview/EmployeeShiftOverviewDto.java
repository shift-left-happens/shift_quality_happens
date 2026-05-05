package dk.ek.shift_happens.view.employeeshiftoverview;

import java.time.LocalDateTime;

public record EmployeeShiftOverviewDto(
        Integer shiftAssignmentId,
        String employeeNumber,
        String firstName,
        String lastName,
        String email,
        String shiftName,
        LocalDateTime startDatetime,
        LocalDateTime endDatetime,
        String shiftStatus,
        String departmentName,
        String locationName,
        String assignmentStatus,
        LocalDateTime assignedDatetime,
        LocalDateTime checkInDatetime,
        LocalDateTime checkOutDatetime) {
    public static EmployeeShiftOverviewDto from(EmployeeShiftOverviewView view) {
        return new EmployeeShiftOverviewDto(
                view.getShiftAssignmentId(),
                view.getEmployeeNumber(),
                view.getFirstName(),
                view.getLastName(),
                view.getEmail(),
                view.getShiftName(),
                view.getStartDatetime(),
                view.getEndDatetime(),
                view.getShiftStatus(),
                view.getDepartmentName(),
                view.getLocationName(),
                view.getAssignmentStatus(),
                view.getAssignedDatetime(),
                view.getCheckInDatetime(),
                view.getCheckOutDatetime());
    }
}

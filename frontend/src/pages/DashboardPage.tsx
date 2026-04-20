import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import { listEmployees } from '../api/employees';
import { listDepartments } from '../api/departments';
import { listShifts } from '../api/shifts';
import { listShiftAssignments } from '../api/shiftAssignments';
import { listLeaveRequests } from '../api/leaveRequests';
import type { Shift, ShiftAssignment } from '../api/types';
import { canWrite } from '../auth/roles';

interface DashboardData {
  employees: number | null;
  departments: number | null;
  shiftsThisWeek: number | null;
  assignments: number | null;
  pendingLeave: number | null;
  nextShift: { shift: Shift; assignment: ShiftAssignment } | null;
}

function greeting(): string {
  const hour = new Date().getHours();
  if (hour < 5) return 'Good evening';
  if (hour < 12) return 'Good morning';
  if (hour < 18) return 'Good afternoon';
  return 'Good evening';
}

function startOfWeek(date: Date): Date {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  const offset = (d.getDay() + 6) % 7;
  d.setDate(d.getDate() - offset);
  return d;
}

export default function DashboardPage() {
  const { user } = useAuth();
  const mayWrite = canWrite(user?.roleName);
  const isEmployee = user?.roleName === 'Employee';
  const employeeId = user?.employeeId;

  const [data, setData] = useState<DashboardData>({
    employees: null,
    departments: null,
    shiftsThisWeek: null,
    assignments: null,
    pendingLeave: null,
    nextShift: null,
  });

  useEffect(() => {
    let cancelled = false;
    const now = new Date();
    const weekStart = startOfWeek(now);
    const weekEnd = new Date(weekStart);
    weekEnd.setDate(weekEnd.getDate() + 7);

    Promise.all([
      mayWrite ? listEmployees().then((es) => es.length) : Promise.resolve(null),
      mayWrite ? listDepartments().then((ds) => ds.length) : Promise.resolve(null),
      listShifts(),
      listShiftAssignments(),
      mayWrite
        ? listLeaveRequests().then(
            (rs) =>
              rs.filter(
                (r) =>
                  (r.requestStatus ?? '').toLowerCase() === 'pending'
              ).length
          )
        : Promise.resolve(null),
    ])
      .then(([employees, departments, shifts, assignments, pendingLeave]) => {
        if (cancelled) return;
        const shiftsThisWeek = shifts.filter((s) => {
          if (!s.startDatetime) return false;
          const t = new Date(s.startDatetime).getTime();
          return t >= weekStart.getTime() && t < weekEnd.getTime();
        }).length;

        const myAssignments = assignments.filter(
          (a) => a.employeeId === employeeId
        );
        const assignmentsCount = mayWrite
          ? assignments.length
          : myAssignments.length;

        let nextShift: DashboardData['nextShift'] = null;
        if (employeeId !== undefined) {
          const shiftById = new Map(shifts.map((s) => [s.shiftId, s]));
          const upcoming = myAssignments
            .map((a) => ({ assignment: a, shift: shiftById.get(a.shiftId) }))
            .filter(
              (x): x is { assignment: ShiftAssignment; shift: Shift } =>
                x.shift !== undefined &&
                x.shift.startDatetime !== null &&
                new Date(x.shift.startDatetime).getTime() >= now.getTime()
            )
            .sort((a, b) =>
              (a.shift.startDatetime ?? '').localeCompare(
                b.shift.startDatetime ?? ''
              )
            );
          nextShift = upcoming[0] ?? null;
        }

        setData({
          employees,
          departments,
          shiftsThisWeek,
          assignments: assignmentsCount,
          pendingLeave,
          nextShift,
        });
      })
      .catch(() => {
        /* leave cards blank */
      });

    return () => {
      cancelled = true;
    };
  }, [mayWrite, employeeId]);

  const firstName = user?.firstName ?? '';

  const nextShiftLine = useMemo(() => {
    if (!data.nextShift) return null;
    const { shift } = data.nextShift;
    const start = shift.startDatetime ? new Date(shift.startDatetime) : null;
    if (!start) return null;
    const when = start.toLocaleString(undefined, {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
    return `${shift.shiftName ?? 'Shift'} · ${when}`;
  }, [data.nextShift]);

  return (
    <div className="page">
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">
          {greeting()}
          {firstName ? `, ${firstName}` : ''}.
        </h1>
        <p className="mt-1 text-slate-500">
          {isEmployee
            ? "Here's a look at your upcoming week."
            : "Here's what's happening with your team this week."}
        </p>
      </div>

      <div className="stat-grid">
        <Link to="/shifts" className="stat-card">
          <span className="stat-label">Shifts this week</span>
          <span className="stat-value">{data.shiftsThisWeek ?? '—'}</span>
          <span className="stat-hint">Scheduled in the current week</span>
        </Link>

        <Link
          to={mayWrite ? '/shift-assignments' : '/my-schedule'}
          className="stat-card"
        >
          <span className="stat-label">
            {mayWrite ? 'Assignments' : 'My assignments'}
          </span>
          <span className="stat-value">{data.assignments ?? '—'}</span>
          <span className="stat-hint">
            {mayWrite ? 'All shift assignments' : 'Assigned to you'}
          </span>
        </Link>

        {mayWrite && (
          <Link to="/leave-requests" className="stat-card">
            <span className="stat-label">Pending leave</span>
            <span className="stat-value">{data.pendingLeave ?? '—'}</span>
            <span className="stat-hint">Awaiting your review</span>
          </Link>
        )}

        {mayWrite && (
          <Link to="/employees" className="stat-card">
            <span className="stat-label">Employees</span>
            <span className="stat-value">{data.employees ?? '—'}</span>
            <span className="stat-hint">People on your team</span>
          </Link>
        )}

        {mayWrite && (
          <Link to="/departments" className="stat-card">
            <span className="stat-label">Departments</span>
            <span className="stat-value">{data.departments ?? '—'}</span>
            <span className="stat-hint">Active and inactive</span>
          </Link>
        )}
      </div>

      {isEmployee && (
        <section className="page-section">
          <h2 className="page-section-title">Your next shift</h2>
          {nextShiftLine ? (
            <Link to="/my-schedule" className="stat-card" style={{ maxWidth: 520 }}>
              <span className="stat-label">Coming up</span>
              <span className="stat-value" style={{ fontSize: '1.4rem' }}>
                {nextShiftLine}
              </span>
              <span className="stat-hint">Tap to see your full schedule</span>
            </Link>
          ) : (
            <div className="empty-state">
              <p>No upcoming shifts on your calendar.</p>
            </div>
          )}
        </section>
      )}

      <section className="page-section">
        <h2 className="page-section-title">Quick actions</h2>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {mayWrite && (
            <QuickAction
              to="/shifts/new"
              title="Create a shift"
              description="Add a new shift to the planner"
            />
          )}
          <QuickAction
            to="/shifts"
            title="Plan shifts"
            description="Open the weekly planner"
          />
          <QuickAction
            to="/my-schedule"
            title="My schedule"
            description="View your upcoming shifts"
          />
          <QuickAction
            to="/leave-requests/new"
            title="Request time off"
            description="Submit a new leave request"
          />
          {mayWrite && (
            <QuickAction
              to="/shift-assignments/new"
              title="Assign a shift"
              description="Put someone on a shift"
            />
          )}
          <QuickAction
            to="/leave-requests"
            title="Leave requests"
            description="Track time off and approvals"
          />
        </div>
      </section>
    </div>
  );
}

interface QuickActionProps {
  to: string;
  title: string;
  description: string;
}

function QuickAction({ to, title, description }: QuickActionProps) {
  return (
    <Link
      to={to}
      className="group flex items-center justify-between gap-3 rounded-[10px] border border-slate-200 bg-white px-4 py-3.5 shadow-card transition hover:-translate-y-[1px] hover:border-brand/40 hover:shadow-card-hover hover:no-underline"
    >
      <div className="min-w-0">
        <div className="font-semibold text-slate-900">{title}</div>
        <div className="text-sm text-slate-500">{description}</div>
      </div>
      <span className="text-slate-400 transition group-hover:translate-x-0.5 group-hover:text-brand">
        →
      </span>
    </Link>
  );
}

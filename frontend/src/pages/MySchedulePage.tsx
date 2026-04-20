import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { listShiftAssignments } from '../api/shiftAssignments';
import { listShifts } from '../api/shifts';
import { listDepartments } from '../api/departments';
import type {
  Department,
  Shift,
  ShiftAssignment,
} from '../api/types';
import { ApiError } from '../api/types';
import { useAuth } from '../auth/useAuth';

const DEPT_COLORS = [
  '#2563eb',
  '#7c3aed',
  '#db2777',
  '#ea580c',
  '#059669',
  '#0891b2',
  '#dc2626',
  '#4f46e5',
];

function deptColor(id: number | undefined): string {
  if (id === undefined) return '#64748b';
  return DEPT_COLORS[Math.abs(id) % DEPT_COLORS.length];
}

function formatStatus(status: string | null): string {
  if (!status) return 'Unknown';
  return status
    .toLowerCase()
    .split('_')
    .map((p) => p.charAt(0).toUpperCase() + p.slice(1))
    .join(' ');
}

function statusPillClass(status: string | null): string {
  const s = (status ?? '').toLowerCase();
  if (s.includes('complete') || s.includes('checked_out')) return 'pill pill-success';
  if (s.includes('cancel') || s.includes('no_show')) return 'pill pill-danger';
  if (s.includes('checked_in') || s.includes('active')) return 'pill pill-brand';
  return 'pill pill-neutral';
}

function hoursBetween(start: string | null, end: string | null): number {
  if (!start || !end) return 0;
  const s = new Date(start).getTime();
  const e = new Date(end).getTime();
  if (Number.isNaN(s) || Number.isNaN(e) || e <= s) return 0;
  return (e - s) / 3_600_000;
}

function formatHours(h: number): string {
  if (!h) return '—';
  const rounded = Math.round(h * 10) / 10;
  return `${rounded}h`;
}

function startOfDay(d: Date): Date {
  const n = new Date(d);
  n.setHours(0, 0, 0, 0);
  return n;
}

export default function MySchedulePage() {
  const { user } = useAuth();
  const employeeId = user?.employeeId;

  const [assignments, setAssignments] = useState<ShiftAssignment[] | null>(null);
  const [shifts, setShifts] = useState<Shift[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([listShiftAssignments(), listShifts(), listDepartments()])
      .then(([a, s, d]) => {
        if (cancelled) return;
        setAssignments(a);
        setShifts(s);
        setDepartments(d);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : 'Failed to load schedule');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const shiftById = useMemo(() => {
    const m = new Map<number, Shift>();
    for (const s of shifts) m.set(s.shiftId, s);
    return m;
  }, [shifts]);

  const deptById = useMemo(() => {
    const m = new Map<number, Department>();
    for (const d of departments) m.set(d.departmentId, d);
    return m;
  }, [departments]);

  const mine = useMemo(() => {
    if (!assignments || employeeId === undefined) return [];
    return assignments
      .filter((a) => a.employeeId === employeeId)
      .map((a) => ({ assignment: a, shift: shiftById.get(a.shiftId) }))
      .filter((x) => x.shift)
      .sort((x, y) => {
        const xs = x.shift?.startDatetime ?? '';
        const ys = y.shift?.startDatetime ?? '';
        return xs.localeCompare(ys);
      });
  }, [assignments, shiftById, employeeId]);

  const today = startOfDay(new Date()).getTime();
  const upcoming = mine.filter(
    (x) => x.shift?.startDatetime && new Date(x.shift.startDatetime).getTime() >= today
  );
  const past = mine
    .filter(
      (x) =>
        x.shift?.startDatetime &&
        new Date(x.shift.startDatetime).getTime() < today
    )
    .reverse();

  const upcomingHours = upcoming.reduce(
    (sum, x) => sum + hoursBetween(x.shift?.startDatetime ?? null, x.shift?.endDatetime ?? null),
    0
  );

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">My Schedule</h1>
          <p className="page-subtitle">
            Your upcoming shifts, and what you've worked recently.
          </p>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {!assignments && !error && <div className="page-muted">Loading…</div>}

      {assignments && (
        <>
          <div className="stat-grid">
            <div className="stat-card">
              <div className="stat-label">Upcoming shifts</div>
              <div className="stat-value">{upcoming.length}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Upcoming hours</div>
              <div className="stat-value">{formatHours(upcomingHours)}</div>
            </div>
            <div className="stat-card">
              <div className="stat-label">Past shifts</div>
              <div className="stat-value">{past.length}</div>
            </div>
          </div>

          <section className="page-section">
            <h2 className="page-subtitle" style={{ marginBottom: '0.75rem' }}>
              Upcoming
            </h2>
            {upcoming.length === 0 ? (
              <div className="empty-state">
                <p>You don't have any upcoming shifts.</p>
              </div>
            ) : (
              <div className="schedule-list">
                {upcoming.map(({ assignment, shift }) => {
                  if (!shift) return null;
                  const dept = deptById.get(shift.departmentId);
                  const start = shift.startDatetime
                    ? new Date(shift.startDatetime)
                    : null;
                  const end = shift.endDatetime
                    ? new Date(shift.endDatetime)
                    : null;
                  return (
                    <div
                      key={assignment.shiftAssignmentId}
                      className="schedule-card"
                      style={{
                        ['--block-accent' as string]: deptColor(shift.departmentId),
                      }}
                    >
                      <div className="schedule-card-date">
                        <div className="schedule-card-dow">
                          {start?.toLocaleDateString(undefined, {
                            weekday: 'short',
                          })}
                        </div>
                        <div className="schedule-card-day">
                          {start?.getDate()}
                        </div>
                        <div className="schedule-card-month">
                          {start?.toLocaleDateString(undefined, { month: 'short' })}
                        </div>
                      </div>
                      <div className="schedule-card-body">
                        <div className="schedule-card-title">
                          {shift.shiftName ?? `Shift #${shift.shiftId}`}
                        </div>
                        <div className="schedule-card-meta">
                          {dept?.departmentName ?? '—'}
                        </div>
                        <div className="schedule-card-time">
                          {start?.toLocaleTimeString([], {
                            hour: '2-digit',
                            minute: '2-digit',
                          })}
                          {end
                            ? ` – ${end.toLocaleTimeString([], {
                                hour: '2-digit',
                                minute: '2-digit',
                              })}`
                            : ''}
                          {' · '}
                          {formatHours(
                            hoursBetween(shift.startDatetime, shift.endDatetime)
                          )}
                        </div>
                      </div>
                      <div className="schedule-card-status">
                        <span
                          className={statusPillClass(assignment.assignmentStatus)}
                        >
                          <span className="pill-dot" />
                          {formatStatus(assignment.assignmentStatus)}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </section>

          {past.length > 0 && (
            <section className="page-section">
              <h2 className="page-subtitle" style={{ marginBottom: '0.75rem' }}>
                Recent
              </h2>
              <div className="schedule-list">
                {past.slice(0, 10).map(({ assignment, shift }) => {
                  if (!shift) return null;
                  const dept = deptById.get(shift.departmentId);
                  const start = shift.startDatetime
                    ? new Date(shift.startDatetime)
                    : null;
                  const end = shift.endDatetime
                    ? new Date(shift.endDatetime)
                    : null;
                  return (
                    <div
                      key={assignment.shiftAssignmentId}
                      className="schedule-card schedule-card--muted"
                      style={{
                        ['--block-accent' as string]: deptColor(shift.departmentId),
                      }}
                    >
                      <div className="schedule-card-date">
                        <div className="schedule-card-dow">
                          {start?.toLocaleDateString(undefined, {
                            weekday: 'short',
                          })}
                        </div>
                        <div className="schedule-card-day">
                          {start?.getDate()}
                        </div>
                        <div className="schedule-card-month">
                          {start?.toLocaleDateString(undefined, { month: 'short' })}
                        </div>
                      </div>
                      <div className="schedule-card-body">
                        <div className="schedule-card-title">
                          {shift.shiftName ?? `Shift #${shift.shiftId}`}
                        </div>
                        <div className="schedule-card-meta">
                          {dept?.departmentName ?? '—'}
                        </div>
                        <div className="schedule-card-time">
                          {start?.toLocaleTimeString([], {
                            hour: '2-digit',
                            minute: '2-digit',
                          })}
                          {end
                            ? ` – ${end.toLocaleTimeString([], {
                                hour: '2-digit',
                                minute: '2-digit',
                              })}`
                            : ''}
                        </div>
                      </div>
                      <div className="schedule-card-status">
                        <span
                          className={statusPillClass(assignment.assignmentStatus)}
                        >
                          <span className="pill-dot" />
                          {formatStatus(assignment.assignmentStatus)}
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            </section>
          )}

          <div className="page-muted" style={{ marginTop: '1rem' }}>
            Questions about your schedule? <Link to="/leave-requests">Request time off</Link>.
          </div>
        </>
      )}
    </div>
  );
}

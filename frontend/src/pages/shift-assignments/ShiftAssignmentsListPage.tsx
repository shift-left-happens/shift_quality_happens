import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  deleteShiftAssignment,
  listShiftAssignments,
} from '../../api/shiftAssignments';
import { listEmployees } from '../../api/employees';
import { listShifts } from '../../api/shifts';
import { listDepartments } from '../../api/departments';
import type {
  Department,
  Employee,
  Shift,
  ShiftAssignment,
} from '../../api/types';
import { ApiError } from '../../api/types';
import { useAuth } from '../../auth/useAuth';
import { canWrite } from '../../auth/roles';

const AVATAR_COLORS = [
  '#6366f1',
  '#ec4899',
  '#f97316',
  '#14b8a6',
  '#8b5cf6',
  '#ef4444',
  '#f59e0b',
  '#10b981',
  '#0ea5e9',
  '#d946ef',
];

function avatarColor(id: number): string {
  return AVATAR_COLORS[Math.abs(id) % AVATAR_COLORS.length];
}

function employeeInitials(e: Employee | undefined): string {
  if (!e) return '?';
  const f = e.firstName?.[0] ?? '';
  const l = e.lastName?.[0] ?? '';
  return (f + l).toUpperCase() || '?';
}

function statusPillClass(status: string | null): string {
  const s = (status ?? '').toLowerCase();
  if (s.includes('complete') || s.includes('checked_out')) return 'pill pill-success';
  if (s.includes('cancel') || s.includes('no_show')) return 'pill pill-danger';
  if (s.includes('checked_in') || s.includes('active')) return 'pill pill-brand';
  return 'pill pill-neutral';
}

function formatStatus(status: string | null): string {
  if (!status) return 'Unknown';
  return status
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

function formatRange(start: string | null, end: string | null): string {
  if (!start) return '—';
  const s = new Date(start);
  if (Number.isNaN(s.getTime())) return '—';
  const date = s.toLocaleDateString(undefined, {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  });
  const sTime = s.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  if (!end) return `${date} · ${sTime}`;
  const e = new Date(end);
  const eTime = e.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  return `${date} · ${sTime} – ${eTime}`;
}

export default function ShiftAssignmentsListPage() {
  const { user } = useAuth();
  const mayWrite = canWrite(user?.roleName);

  const [assignments, setAssignments] = useState<ShiftAssignment[] | null>(null);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [shifts, setShifts] = useState<Shift[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      listShiftAssignments(),
      listEmployees(),
      listShifts(),
      listDepartments(),
    ])
      .then(([a, e, s, d]) => {
        if (cancelled) return;
        setAssignments(a);
        setEmployees(e);
        setShifts(s);
        setDepartments(d);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(
          err instanceof ApiError ? err.message : 'Failed to load shift assignments'
        );
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const empById = useMemo(() => {
    const m = new Map<number, Employee>();
    for (const e of employees) m.set(e.employeeId, e);
    return m;
  }, [employees]);

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

  async function handleDelete(id: number, label: string) {
    if (!confirm(`Delete assignment for "${label}"?`)) return;
    try {
      await deleteShiftAssignment(id);
      setAssignments(
        (prev) => prev?.filter((a) => a.shiftAssignmentId !== id) ?? null
      );
    } catch (err) {
      alert(err instanceof ApiError ? err.message : 'Delete failed');
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Shift Assignments</h1>
          <p className="page-subtitle">
            Who's scheduled where — and whether they've clocked in.
          </p>
        </div>
        {mayWrite && (
          <Link to="/shift-assignments/new" className="btn btn-primary">
            New assignment
          </Link>
        )}
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {!assignments && !error && <div className="page-muted">Loading…</div>}

      {assignments && (
        <>
          <div className="data-table-toolbar">
            <span className="data-table-count">
              {assignments.length}{' '}
              {assignments.length === 1 ? 'assignment' : 'assignments'}
            </span>
          </div>

          <table className="data-table">
            <thead>
              <tr>
                <th>Employee</th>
                <th>Shift</th>
                <th>When</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {assignments.length === 0 && (
                <tr>
                  <td colSpan={5} className="page-muted">
                    No shift assignments yet.
                  </td>
                </tr>
              )}
              {assignments.map((a) => {
                const emp = empById.get(a.employeeId);
                const shift = shiftById.get(a.shiftId);
                const dept = shift ? deptById.get(shift.departmentId) : undefined;
                const empLabel = emp
                  ? `${emp.firstName ?? ''} ${emp.lastName ?? ''}`.trim()
                  : `#${a.employeeId}`;
                return (
                  <tr key={a.shiftAssignmentId}>
                    <td>
                      <Link
                        to={`/shift-assignments/${a.shiftAssignmentId}`}
                        className="name-cell hover:no-underline"
                      >
                        <span
                          className="avatar"
                          style={{ background: avatarColor(a.employeeId) }}
                        >
                          {employeeInitials(emp)}
                        </span>
                        <span>
                          <span className="name-cell-primary">{empLabel}</span>
                          <span className="name-cell-secondary">
                            {emp?.email ?? ''}
                          </span>
                        </span>
                      </Link>
                    </td>
                    <td>
                      <div className="name-cell-primary">
                        {shift?.shiftName ?? `Shift #${a.shiftId}`}
                      </div>
                      <div className="name-cell-secondary">
                        {dept?.departmentName ?? ''}
                      </div>
                    </td>
                    <td className="text-slate-500">
                      {formatRange(
                        shift?.startDatetime ?? null,
                        shift?.endDatetime ?? null
                      )}
                    </td>
                    <td>
                      <span className={statusPillClass(a.assignmentStatus)}>
                        <span className="pill-dot" />
                        {formatStatus(a.assignmentStatus)}
                      </span>
                    </td>
                    <td className="data-table-actions">
                      {mayWrite && (
                        <>
                          <Link
                            to={`/shift-assignments/${a.shiftAssignmentId}`}
                            className="btn btn-sm"
                          >
                            Edit
                          </Link>
                          <button
                            type="button"
                            className="btn btn-sm btn-danger"
                            onClick={() =>
                              handleDelete(a.shiftAssignmentId, empLabel)
                            }
                          >
                            Delete
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </>
      )}
    </div>
  );
}

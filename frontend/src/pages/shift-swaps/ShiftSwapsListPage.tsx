import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  listShiftSwaps,
  cancelShiftSwap,
  deleteShiftSwap,
} from '../../api/shiftSwaps';
import { createShiftSwapApproval } from '../../api/shiftSwapApprovals';
import { listShiftAssignments } from '../../api/shiftAssignments';
import { listShifts } from '../../api/shifts';
import { listEmployees } from '../../api/employees';
import type {
  Employee,
  Shift,
  ShiftAssignment,
  ShiftSwap,
} from '../../api/types';
import { ApiError } from '../../api/types';
import { useAuth } from '../../auth/useAuth';
import { canWrite } from '../../auth/roles';

function statusPillClass(status: string | null): string {
  const s = (status ?? '').toLowerCase();
  if (s === 'approved' || s === 'completed') return 'pill pill-success';
  if (s === 'rejected' || s === 'denied' || s === 'declined' || s === 'cancelled')
    return 'pill pill-danger';
  if (s === 'pending') return 'pill pill-brand';
  return 'pill pill-neutral';
}

function formatStatus(status: string | null): string {
  if (!status) return 'Unknown';
  return status.charAt(0).toUpperCase() + status.slice(1).toLowerCase();
}

function employeeName(e: Employee | undefined, id: number): string {
  if (!e) return `#${id}`;
  return `${e.firstName ?? ''} ${e.lastName ?? ''}`.trim() || `#${id}`;
}

function shiftLabel(shift: Shift | undefined): string {
  if (!shift) return '—';
  const start = shift.startDatetime ? new Date(shift.startDatetime) : null;
  const when =
    start && !Number.isNaN(start.getTime())
      ? start.toLocaleDateString(undefined, {
          month: 'short',
          day: 'numeric',
          year: 'numeric',
        })
      : '';
  const name = shift.shiftName ?? `Shift #${shift.shiftId}`;
  return when ? `${name} · ${when}` : name;
}

export default function ShiftSwapsListPage() {
  const { user } = useAuth();
  const mayWrite = canWrite(user?.roleName);
  const isEmployee = user?.roleName === 'Employee';

  const [swaps, setSwaps] = useState<ShiftSwap[] | null>(null);
  const [assignments, setAssignments] = useState<ShiftAssignment[]>([]);
  const [shifts, setShifts] = useState<Shift[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([
      listShiftSwaps(),
      listShiftAssignments(),
      listShifts(),
      listEmployees(),
    ])
      .then(([sw, as, ss, es]) => {
        if (cancelled) return;
        setSwaps(sw);
        setAssignments(as);
        setShifts(ss);
        setEmployees(es);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(
          err instanceof ApiError ? err.message : 'Failed to load shift swaps'
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

  const assignmentById = useMemo(() => {
    const m = new Map<number, ShiftAssignment>();
    for (const a of assignments) m.set(a.shiftAssignmentId, a);
    return m;
  }, [assignments]);

  const shiftById = useMemo(() => {
    const m = new Map<number, Shift>();
    for (const s of shifts) m.set(s.shiftId, s);
    return m;
  }, [shifts]);

  async function handleCancel(id: number) {
    if (!confirm('Cancel this swap request?')) return;
    try {
      const updated = await cancelShiftSwap(id);
      setSwaps((prev) =>
        prev?.map((s) => (s.shiftSwapId === id ? updated : s)) ?? null
      );
    } catch (err) {
      alert(err instanceof ApiError ? err.message : 'Cancel failed');
    }
  }

  async function handleDelete(id: number) {
    if (!confirm('Delete this swap request?')) return;
    try {
      await deleteShiftSwap(id);
      setSwaps((prev) => prev?.filter((s) => s.shiftSwapId !== id) ?? null);
    } catch (err) {
      alert(err instanceof ApiError ? err.message : 'Delete failed');
    }
  }

  async function handleDecision(swapId: number, decision: 'Approved' | 'Declined') {
    if (user?.employeeId === undefined) return;
    const verb = decision === 'Approved' ? 'Approve' : 'Decline';
    if (!confirm(`${verb} this swap request?`)) return;
    try {
      await createShiftSwapApproval({
        shiftSwapId: swapId,
        approverEmployeeId: user.employeeId,
        decision,
        shiftSwapComment: null,
        decisionDatetime: null,
      });
      // Approval mutates the swap (status, and on approval the underlying
      // assignment), so re-fetch to reflect the new state.
      const fresh = await listShiftSwaps();
      setSwaps(fresh);
    } catch (err) {
      alert(err instanceof ApiError ? err.message : `${verb} failed`);
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Shift Swaps</h1>
          <p className="page-subtitle">
            {isEmployee
              ? 'Request to hand one of your shifts to a colleague.'
              : 'Review shift swap requests across the team.'}
          </p>
        </div>
        <Link to="/shift-swaps/new" className="btn btn-primary">
          New swap request
        </Link>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {!swaps && !error && <div className="page-muted">Loading…</div>}

      {swaps && (
        <>
          <div className="data-table-toolbar">
            <span className="data-table-count">
              {swaps.length} {swaps.length === 1 ? 'request' : 'requests'}
            </span>
          </div>

          <table className="data-table">
            <thead>
              <tr>
                <th>Shift</th>
                <th>From</th>
                <th>To</th>
                <th>Status</th>
                <th>Reason</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {swaps.length === 0 && (
                <tr>
                  <td colSpan={6} className="page-muted">
                    No shift swap requests yet.
                  </td>
                </tr>
              )}
              {swaps.map((s) => {
                const assignment = assignmentById.get(
                  s.originalShiftAssignmentId
                );
                const shift = assignment
                  ? shiftById.get(assignment.shiftId)
                  : undefined;
                const isPending = (s.swapStatus ?? '').toLowerCase() === 'pending';
                const isRequester = s.employeeFromId === user?.employeeId;
                const isParty =
                  isRequester || s.employeeToId === user?.employeeId;
                // Managers/admins review swaps they are not a party to;
                // the requester can withdraw their own pending request.
                const canReview = mayWrite && isPending && !isParty;
                const canCancel = isPending && isRequester;
                return (
                  <tr key={s.shiftSwapId}>
                    <td>
                      <span className="name-cell-primary">
                        {shiftLabel(shift)}
                      </span>
                    </td>
                    <td>{employeeName(empById.get(s.employeeFromId), s.employeeFromId)}</td>
                    <td>{employeeName(empById.get(s.employeeToId), s.employeeToId)}</td>
                    <td>
                      <span className={statusPillClass(s.swapStatus)}>
                        <span className="pill-dot" />
                        {formatStatus(s.swapStatus)}
                      </span>
                    </td>
                    <td className="text-slate-500">{s.reason ?? '—'}</td>
                    <td className="data-table-actions">
                      {canReview && (
                        <>
                          <button
                            type="button"
                            className="btn btn-sm btn-primary"
                            onClick={() =>
                              handleDecision(s.shiftSwapId, 'Approved')
                            }
                          >
                            Approve
                          </button>
                          <button
                            type="button"
                            className="btn btn-sm"
                            onClick={() =>
                              handleDecision(s.shiftSwapId, 'Declined')
                            }
                          >
                            Decline
                          </button>
                        </>
                      )}
                      {canCancel && (
                        <button
                          type="button"
                          className="btn btn-sm"
                          onClick={() => handleCancel(s.shiftSwapId)}
                        >
                          Cancel
                        </button>
                      )}
                      {mayWrite && (
                        <button
                          type="button"
                          className="btn btn-sm btn-danger"
                          onClick={() => handleDelete(s.shiftSwapId)}
                        >
                          Delete
                        </button>
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

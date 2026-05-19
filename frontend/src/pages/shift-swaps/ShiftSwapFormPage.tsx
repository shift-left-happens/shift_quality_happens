import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { createShiftSwap } from '../../api/shiftSwaps';
import { listShiftAssignments } from '../../api/shiftAssignments';
import { listShifts } from '../../api/shifts';
import { listEmployees } from '../../api/employees';
import type {
  Employee,
  Shift,
  ShiftAssignment,
} from '../../api/types';
import { ApiError } from '../../api/types';
import { useAuth } from '../../auth/useAuth';

/** A request datetime safely in the past, formatted as the backend expects. */
function requestDatetimeNow(): string {
  return new Date(Date.now() - 60_000).toISOString().slice(0, 19);
}

export default function ShiftSwapFormPage() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const isEmployee = user?.roleName === 'Employee';

  const [assignments, setAssignments] = useState<ShiftAssignment[]>([]);
  const [shifts, setShifts] = useState<Shift[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [assignmentId, setAssignmentId] = useState(0);
  const [employeeToId, setEmployeeToId] = useState(0);
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([listShiftAssignments(), listShifts(), listEmployees()])
      .then(([as, ss, es]) => {
        if (cancelled) return;
        setAssignments(as);
        setShifts(ss);
        setEmployees(es);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : 'Failed to load');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
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

  const empById = useMemo(() => {
    const m = new Map<number, Employee>();
    for (const e of employees) m.set(e.employeeId, e);
    return m;
  }, [employees]);

  // Employees may only swap away their own assignments; managers/admins
  // can raise a swap for anyone.
  const myAssignments = useMemo(() => {
    if (isEmployee && user?.employeeId !== undefined) {
      return assignments.filter((a) => a.employeeId === user.employeeId);
    }
    return assignments;
  }, [assignments, isEmployee, user?.employeeId]);

  // Default to the first available assignment once data has loaded.
  useEffect(() => {
    if (assignmentId === 0 && myAssignments.length > 0) {
      setAssignmentId(myAssignments[0].shiftAssignmentId);
    }
  }, [assignmentId, myAssignments]);

  const selectedAssignment = myAssignments.find(
    (a) => a.shiftAssignmentId === assignmentId
  );
  const employeeFromId = selectedAssignment?.employeeId ?? 0;

  function assignmentLabel(a: ShiftAssignment): string {
    const shift = shiftById.get(a.shiftId);
    const name = shift?.shiftName ?? `Shift #${a.shiftId}`;
    const start = shift?.startDatetime ? new Date(shift.startDatetime) : null;
    const when =
      start && !Number.isNaN(start.getTime())
        ? start.toLocaleDateString(undefined, {
            month: 'short',
            day: 'numeric',
          })
        : '';
    const emp = empById.get(a.employeeId);
    const who = emp ? `${emp.firstName ?? ''} ${emp.lastName ?? ''}`.trim() : '';
    return [name, when, who].filter(Boolean).join(' · ');
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (assignmentId === 0 || employeeFromId === 0) {
      setError('Select the shift assignment to swap.');
      return;
    }
    if (employeeToId === 0) {
      setError('Select the employee to swap with.');
      return;
    }
    if (employeeToId === employeeFromId) {
      setError('You cannot swap a shift with the same employee.');
      return;
    }

    setSubmitting(true);
    try {
      await createShiftSwap({
        originalShiftAssignmentId: assignmentId,
        employeeFromId,
        employeeToId,
        swapStatus: null,
        requestDatetime: requestDatetimeNow(),
        reason: reason.trim() || null,
      });
      navigate('/shift-swaps');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Save failed');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <div className="page page-muted">Loading…</div>;

  return (
    <div className="page">
      <h1 className="page-title">New swap request</h1>
      {error && <div className="alert alert-error">{error}</div>}

      {myAssignments.length === 0 ? (
        <div className="card card--padded page-muted">
          There are no shift assignments to swap.
        </div>
      ) : (
        <form className="form-grid" onSubmit={handleSubmit}>
          <label className="form-field">
            <span>Shift to swap</span>
            <select
              required
              value={assignmentId}
              onChange={(e) => setAssignmentId(Number(e.target.value))}
            >
              <option value={0} disabled>
                Select…
              </option>
              {myAssignments.map((a) => (
                <option key={a.shiftAssignmentId} value={a.shiftAssignmentId}>
                  {assignmentLabel(a)}
                </option>
              ))}
            </select>
          </label>

          <label className="form-field">
            <span>Swap with</span>
            <select
              required
              value={employeeToId}
              onChange={(e) => setEmployeeToId(Number(e.target.value))}
            >
              <option value={0} disabled>
                Select…
              </option>
              {employees
                .filter((e) => e.employeeId !== employeeFromId)
                .map((e) => (
                  <option key={e.employeeId} value={e.employeeId}>
                    {e.firstName} {e.lastName}
                    {e.email ? ` · ${e.email}` : ''}
                  </option>
                ))}
            </select>
          </label>

          <label className="form-field">
            <span>Reason</span>
            <input
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Why is this swap needed?"
            />
          </label>

          <div className="form-actions">
            <button
              type="button"
              className="btn"
              onClick={() => navigate('/shift-swaps')}
            >
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Saving…' : 'Submit request'}
            </button>
          </div>
        </form>
      )}
    </div>
  );
}

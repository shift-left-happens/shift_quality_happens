import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  createShiftAssignment,
  deleteShiftAssignment,
  getShiftAssignment,
  updateShiftAssignment,
} from '../../api/shiftAssignments';
import { listEmployees } from '../../api/employees';
import { listShifts } from '../../api/shifts';
import type {
  Employee,
  NewShiftAssignment,
  Shift,
} from '../../api/types';
import { ApiError } from '../../api/types';
import { useAuth } from '../../auth/useAuth';
import { canWrite } from '../../auth/roles';

const STATUSES = [
  'ASSIGNED',
  'CONFIRMED',
  'CHECKED_IN',
  'CHECKED_OUT',
  'COMPLETED',
  'CANCELLED',
  'NO_SHOW',
] as const;

function nowLocalDatetime(): string {
  const d = new Date();
  d.setSeconds(0, 0);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(
    d.getHours()
  )}:${pad(d.getMinutes())}`;
}

function buildEmpty(): NewShiftAssignment {
  return {
    shiftId: 0,
    employeeId: 0,
    assignmentStatus: 'ASSIGNED',
    assignedDatetime: `${nowLocalDatetime()}:00`,
    checkInDatetime: null,
    checkOutDatetime: null,
  };
}

function formatShiftLabel(s: Shift): string {
  const start = s.startDatetime ? new Date(s.startDatetime) : null;
  const when =
    start && !Number.isNaN(start.getTime())
      ? start.toLocaleString(undefined, {
          month: 'short',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
        })
      : '';
  return `${s.shiftName ?? `Shift #${s.shiftId}`}${when ? ` · ${when}` : ''}`;
}

export default function ShiftAssignmentFormPage() {
  const { id } = useParams<{ id: string }>();
  const isNew = id === undefined;
  const numericId = isNew ? null : Number(id);

  const navigate = useNavigate();
  const { user } = useAuth();
  const mayWrite = canWrite(user?.roleName);

  const [form, setForm] = useState<NewShiftAssignment>(buildEmpty);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [shifts, setShifts] = useState<Shift[]>([]);
  const [loading, setLoading] = useState(!isNew);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([listEmployees(), listShifts()])
      .then(([es, ss]) => {
        if (cancelled) return;
        setEmployees(es);
        setShifts(ss);
        setForm((f) => ({
          ...f,
          employeeId: f.employeeId || es[0]?.employeeId || 0,
          shiftId: f.shiftId || ss[0]?.shiftId || 0,
        }));
      })
      .catch(() => {
        /* handled by main error state if needed */
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (isNew || numericId === null) return;
    let cancelled = false;
    setLoading(true);
    getShiftAssignment(numericId)
      .then((a) => {
        if (cancelled) return;
        setForm({
          shiftId: a.shiftId,
          employeeId: a.employeeId,
          assignmentStatus: a.assignmentStatus,
          assignedDatetime: a.assignedDatetime,
          checkInDatetime: a.checkInDatetime,
          checkOutDatetime: a.checkOutDatetime,
        });
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
  }, [isNew, numericId]);

  function update<K extends keyof NewShiftAssignment>(
    key: K,
    value: NewShiftAssignment[K]
  ) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function updateDatetime(
    key: 'assignedDatetime' | 'checkInDatetime' | 'checkOutDatetime',
    raw: string
  ) {
    setForm((prev) => ({ ...prev, [key]: raw ? `${raw}:00` : null }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!mayWrite) return;
    setError(null);
    setSubmitting(true);
    try {
      if (isNew) await createShiftAssignment(form);
      else if (numericId !== null) await updateShiftAssignment(numericId, form);
      navigate('/shift-assignments');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Save failed');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete() {
    if (isNew || numericId === null) return;
    if (!confirm('Delete this assignment?')) return;
    try {
      await deleteShiftAssignment(numericId);
      navigate('/shift-assignments');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Delete failed');
    }
  }

  if (loading) return <div className="page page-muted">Loading…</div>;

  return (
    <div className="page">
      <h1 className="page-title">
        {isNew ? 'New shift assignment' : 'Edit shift assignment'}
      </h1>
      {error && <div className="alert alert-error">{error}</div>}
      <form className="form-grid" onSubmit={handleSubmit}>
        <label className="form-field">
          <span>Employee</span>
          <select
            required
            value={form.employeeId}
            onChange={(e) => update('employeeId', Number(e.target.value))}
            disabled={!mayWrite}
          >
            <option value={0} disabled>
              Select…
            </option>
            {employees.map((e) => (
              <option key={e.employeeId} value={e.employeeId}>
                {e.firstName} {e.lastName}
                {e.email ? ` · ${e.email}` : ''}
              </option>
            ))}
          </select>
        </label>

        <label className="form-field">
          <span>Shift</span>
          <select
            required
            value={form.shiftId}
            onChange={(e) => update('shiftId', Number(e.target.value))}
            disabled={!mayWrite}
          >
            <option value={0} disabled>
              Select…
            </option>
            {shifts.map((s) => (
              <option key={s.shiftId} value={s.shiftId}>
                {formatShiftLabel(s)}
              </option>
            ))}
          </select>
        </label>

        <label className="form-field">
          <span>Status</span>
          <select
            value={form.assignmentStatus ?? ''}
            onChange={(e) => update('assignmentStatus', e.target.value)}
            disabled={!mayWrite}
          >
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {s
                  .toLowerCase()
                  .split('_')
                  .map((p) => p.charAt(0).toUpperCase() + p.slice(1))
                  .join(' ')}
              </option>
            ))}
          </select>
        </label>

        <label className="form-field">
          <span>Assigned at</span>
          <input
            type="datetime-local"
            value={(form.assignedDatetime ?? '').slice(0, 16)}
            onChange={(e) => updateDatetime('assignedDatetime', e.target.value)}
            disabled={!mayWrite}
          />
        </label>

        <label className="form-field">
          <span>Check-in</span>
          <input
            type="datetime-local"
            value={(form.checkInDatetime ?? '').slice(0, 16)}
            onChange={(e) => updateDatetime('checkInDatetime', e.target.value)}
            disabled={!mayWrite}
          />
        </label>

        <label className="form-field">
          <span>Check-out</span>
          <input
            type="datetime-local"
            value={(form.checkOutDatetime ?? '').slice(0, 16)}
            onChange={(e) => updateDatetime('checkOutDatetime', e.target.value)}
            disabled={!mayWrite}
          />
        </label>

        <div className="form-actions">
          {!isNew && mayWrite && (
            <button
              type="button"
              className="btn btn-danger"
              onClick={handleDelete}
              style={{ marginRight: 'auto' }}
            >
              Delete
            </button>
          )}
          <button
            type="button"
            className="btn"
            onClick={() => navigate('/shift-assignments')}
          >
            Cancel
          </button>
          {mayWrite && (
            <button
              type="submit"
              className="btn btn-primary"
              disabled={submitting}
            >
              {submitting ? 'Saving…' : isNew ? 'Create' : 'Save changes'}
            </button>
          )}
        </div>
      </form>
    </div>
  );
}

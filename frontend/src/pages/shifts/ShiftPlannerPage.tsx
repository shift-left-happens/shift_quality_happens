import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { listShifts } from '../../api/shifts';
import { listShiftAssignments } from '../../api/shiftAssignments';
import { listEmployees } from '../../api/employees';
import { listDepartments } from '../../api/departments';
import type {
  Employee,
  Shift,
  ShiftAssignment,
  Department,
} from '../../api/types';
import { ApiError } from '../../api/types';
import { useAuth } from '../../auth/useAuth';
import { canWrite } from '../../auth/roles';

const DAY_LABELS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

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

const DEPT_COLORS = [
  '#2563eb',
  '#f59e0b',
  '#10b981',
  '#ef4444',
  '#8b5cf6',
  '#0ea5e9',
  '#ec4899',
  '#14b8a6',
];

function startOfWeek(date: Date): Date {
  const d = new Date(date);
  d.setHours(0, 0, 0, 0);
  const weekday = d.getDay();
  const offset = (weekday + 6) % 7;
  d.setDate(d.getDate() - offset);
  return d;
}

function addDays(date: Date, n: number): Date {
  const d = new Date(date);
  d.setDate(d.getDate() + n);
  return d;
}

function isSameDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  );
}

function formatTime(iso: string | null): string {
  if (!iso) return '';
  const d = new Date(iso);
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function formatWeekRange(start: Date, end: Date): string {
  const sameMonth = start.getMonth() === end.getMonth();
  const startStr = start.toLocaleDateString(undefined, {
    day: 'numeric',
    month: sameMonth ? undefined : 'short',
  });
  const endStr = end.toLocaleDateString(undefined, {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
  return `${startStr} – ${endStr}`;
}

function isoWeekNumber(date: Date): number {
  const tmp = new Date(
    Date.UTC(date.getFullYear(), date.getMonth(), date.getDate())
  );
  const dayNum = tmp.getUTCDay() || 7;
  tmp.setUTCDate(tmp.getUTCDate() + 4 - dayNum);
  const yearStart = new Date(Date.UTC(tmp.getUTCFullYear(), 0, 1));
  return Math.ceil(((tmp.getTime() - yearStart.getTime()) / 86400000 + 1) / 7);
}

function initials(e: Employee): string {
  const f = e.firstName?.[0] ?? '';
  const l = e.lastName?.[0] ?? '';
  return (f + l).toUpperCase() || '?';
}

function avatarColor(id: number): string {
  return AVATAR_COLORS[Math.abs(id) % AVATAR_COLORS.length];
}

function deptColor(id: number): string {
  return DEPT_COLORS[Math.abs(id) % DEPT_COLORS.length];
}

function hoursBetween(start: string | null, end: string | null): number {
  if (!start || !end) return 0;
  const s = new Date(start).getTime();
  const e = new Date(end).getTime();
  if (Number.isNaN(s) || Number.isNaN(e)) return 0;
  return Math.max(0, (e - s) / 3_600_000);
}

function formatHours(h: number): string {
  if (h === 0) return '0h';
  const rounded = Math.round(h * 10) / 10;
  return Number.isInteger(rounded) ? `${rounded}h` : `${rounded.toFixed(1)}h`;
}

/** Selected department filter: a department id, or 'all' for no filter. */
type DepartmentFilter = number | 'all';

export default function ShiftPlannerPage() {
  const { user } = useAuth();
  const mayWrite = canWrite(user?.roleName);
  const navigate = useNavigate();

  const [weekStart, setWeekStart] = useState<Date>(() => startOfWeek(new Date()));
  const [shifts, setShifts] = useState<Shift[]>([]);
  const [assignments, setAssignments] = useState<ShiftAssignment[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [departmentFilter, setDepartmentFilter] = useState<DepartmentFilter>('all');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([
      listShifts(),
      listShiftAssignments(),
      listEmployees(),
      listDepartments(),
    ])
      .then(([ss, as, es, ds]) => {
        if (cancelled) return;
        setShifts(ss);
        setAssignments(as);
        setEmployees(es);
        setDepartments(ds);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : 'Failed to load planner');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const weekDays = useMemo(
    () => Array.from({ length: 7 }, (_, i) => addDays(weekStart, i)),
    [weekStart]
  );
  const weekEnd = weekDays[6];

  const shiftById = useMemo(() => {
    const map = new Map<number, Shift>();
    shifts.forEach((s) => map.set(s.shiftId, s));
    return map;
  }, [shifts]);

  const departmentById = useMemo(() => {
    const map = new Map<number, Department>();
    departments.forEach((d) => map.set(d.departmentId, d));
    return map;
  }, [departments]);

  const sortedDepartments = useMemo(
    () =>
      [...departments].sort((a, b) =>
        (a.departmentName ?? '').localeCompare(b.departmentName ?? '')
      ),
    [departments]
  );

  const rows = useMemo(() => {
    const cellsByEmployee = new Map<
      number,
      { day: Date; shift: Shift; assignment: ShiftAssignment }[]
    >();

    for (const a of assignments) {
      const shift = shiftById.get(a.shiftId);
      if (!shift?.startDatetime) continue;
      if (departmentFilter !== 'all' && shift.departmentId !== departmentFilter) {
        continue;
      }
      const start = new Date(shift.startDatetime);
      const day = weekDays.find((d) => isSameDay(d, start));
      if (!day) continue;
      const list = cellsByEmployee.get(a.employeeId) ?? [];
      list.push({ day, shift, assignment: a });
      cellsByEmployee.set(a.employeeId, list);
    }

    return employees
      .filter((e) => cellsByEmployee.has(e.employeeId))
      .map((e) => {
        const cells = cellsByEmployee.get(e.employeeId) ?? [];
        const totalHours = cells.reduce(
          (sum, c) => sum + hoursBetween(c.shift.startDatetime, c.shift.endDatetime),
          0
        );
        return { employee: e, cells, totalHours };
      });
  }, [assignments, employees, shiftById, weekDays, departmentFilter]);

  /**
   * Shifts in the visible week that have no assignment, grouped by day.
   * These never show up in the employee rows, so without this lane a newly
   * created shift would be invisible until someone is assigned to it.
   */
  const unassignedByDay = useMemo(() => {
    const assignedShiftIds = new Set(assignments.map((a) => a.shiftId));
    const byDay = new Map<string, { day: Date; shift: Shift }[]>();
    for (const shift of shifts) {
      if (assignedShiftIds.has(shift.shiftId)) continue;
      if (!shift.startDatetime) continue;
      if (departmentFilter !== 'all' && shift.departmentId !== departmentFilter) {
        continue;
      }
      const start = new Date(shift.startDatetime);
      const day = weekDays.find((d) => isSameDay(d, start));
      if (!day) continue;
      const key = day.toDateString();
      const list = byDay.get(key) ?? [];
      list.push({ day, shift });
      byDay.set(key, list);
    }
    return byDay;
  }, [shifts, assignments, weekDays, departmentFilter]);

  const unassignedCount = useMemo(() => {
    let n = 0;
    for (const list of unassignedByDay.values()) n += list.length;
    return n;
  }, [unassignedByDay]);

  const weekStats = useMemo(() => {
    const shiftIds = new Set<number>();
    let hours = 0;
    const add = (shift: Shift) => {
      if (shiftIds.has(shift.shiftId)) return;
      shiftIds.add(shift.shiftId);
      hours += hoursBetween(shift.startDatetime, shift.endDatetime);
    };
    for (const row of rows) for (const cell of row.cells) add(cell.shift);
    for (const list of unassignedByDay.values()) for (const u of list) add(u.shift);
    return { shifts: shiftIds.size, hours };
  }, [rows, unassignedByDay]);

  const today = new Date();
  const weekNum = isoWeekNumber(weekStart);

  function goPrev() {
    setWeekStart((w) => addDays(w, -7));
  }
  function goNext() {
    setWeekStart((w) => addDays(w, 7));
  }
  function goToday() {
    setWeekStart(startOfWeek(new Date()));
  }

  function onBlockClick(shiftId: number) {
    navigate(`/shifts/${shiftId}`);
  }

  function onAssignClick(shiftId: number) {
    if (!mayWrite) return;
    navigate(`/shift-assignments/new?shiftId=${shiftId}`);
  }

  function onCellClick(employeeId: number, day: Date) {
    if (!mayWrite) return;
    const params = new URLSearchParams({
      employeeId: String(employeeId),
      date: day.toISOString().slice(0, 10),
    });
    navigate(`/shifts/new?${params.toString()}`);
  }

  const hasContent = rows.length > 0 || unassignedCount > 0;

  return (
    <div className="page">
      <div className="page-header">
        <h1 className="page-title">Shifts</h1>
        {mayWrite && (
          <Link to="/shifts/new" className="btn btn-primary">
            New shift
          </Link>
        )}
      </div>

      <div className="planner-toolbar">
        <div className="planner-nav">
          <button type="button" className="planner-nav-btn" onClick={goPrev} aria-label="Previous week">
            ←
          </button>
          <button type="button" className="planner-nav-btn planner-nav-btn--text" onClick={goToday}>
            Today
          </button>
          <button type="button" className="planner-nav-btn" onClick={goNext} aria-label="Next week">
            →
          </button>
          <span className="planner-nav-label">
            {formatWeekRange(weekStart, weekEnd)}
          </span>
        </div>

        <label className="planner-filter">
          <span className="planner-filter-label">Department</span>
          <select
            className="planner-filter-select"
            value={departmentFilter === 'all' ? 'all' : String(departmentFilter)}
            onChange={(e) =>
              setDepartmentFilter(
                e.target.value === 'all' ? 'all' : Number(e.target.value)
              )
            }
          >
            <option value="all">All departments</option>
            {sortedDepartments.map((d) => (
              <option key={d.departmentId} value={d.departmentId}>
                {d.departmentName ?? `Department #${d.departmentId}`}
              </option>
            ))}
          </select>
        </label>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <div className="card card--padded page-muted">Loading…</div>}

      {!loading && !error && (
        <div className="planner-card">
          <div className="planner-weekbar">
            <div className="planner-weekbar-left">
              <span className="planner-week-badge">Week {weekNum}</span>
              <span className="planner-week-range">
                {formatWeekRange(weekStart, weekEnd)}
              </span>
            </div>
            <div className="planner-week-stats">
              <span className="planner-week-stat">
                {weekStats.shifts} {weekStats.shifts === 1 ? 'shift' : 'shifts'}
              </span>
              {unassignedCount > 0 && (
                <span className="planner-week-stat planner-week-stat--warn">
                  {unassignedCount} unassigned
                </span>
              )}
              <span className="planner-week-stat">
                {formatHours(weekStats.hours)}
              </span>
            </div>
          </div>

          <div className="planner">
            <div className="planner-cell planner-header planner-header--team">
              Team
            </div>
            {weekDays.map((d) => {
              const todayCol = isSameDay(d, today);
              return (
                <div
                  key={d.toISOString()}
                  className={
                    'planner-cell planner-header' +
                    (todayCol ? ' planner-header--today' : '')
                  }
                >
                  <span className="planner-header-label">
                    {DAY_LABELS[(d.getDay() + 6) % 7]}
                  </span>
                  <span className="planner-header-day">{d.getDate()}</span>
                </div>
              );
            })}

            {unassignedCount > 0 && (
              <UnassignedRow
                unassignedByDay={unassignedByDay}
                unassignedCount={unassignedCount}
                weekDays={weekDays}
                today={today}
                mayWrite={mayWrite}
                departmentById={departmentById}
                onAssignClick={onAssignClick}
              />
            )}

            {rows.map(({ employee, cells, totalHours }) => (
              <PlannerRow
                key={employee.employeeId}
                employee={employee}
                cells={cells}
                totalHours={totalHours}
                weekDays={weekDays}
                today={today}
                mayWrite={mayWrite}
                departmentById={departmentById}
                onBlockClick={onBlockClick}
                onCellClick={onCellClick}
              />
            ))}
          </div>

          {!hasContent && (
            <div className="planner-empty">
              No shifts scheduled this week.
            </div>
          )}
        </div>
      )}
    </div>
  );
}

interface UnassignedRowProps {
  unassignedByDay: Map<string, { day: Date; shift: Shift }[]>;
  unassignedCount: number;
  weekDays: Date[];
  today: Date;
  mayWrite: boolean;
  departmentById: Map<number, Department>;
  onAssignClick: (shiftId: number) => void;
}

function UnassignedRow({
  unassignedByDay,
  unassignedCount,
  weekDays,
  today,
  mayWrite,
  departmentById,
  onAssignClick,
}: UnassignedRowProps) {
  return (
    <>
      <div className="planner-cell planner-employee planner-unassigned-head">
        <div className="planner-unassigned-icon">!</div>
        <div className="planner-employee-meta">
          <span className="planner-employee-name">Unassigned</span>
          <span className="planner-employee-hours">
            {unassignedCount} {unassignedCount === 1 ? 'shift' : 'shifts'} need staffing
          </span>
        </div>
      </div>
      {weekDays.map((d) => {
        const items = unassignedByDay.get(d.toDateString()) ?? [];
        const todayCol = isSameDay(d, today);
        return (
          <div
            key={d.toISOString()}
            className={'planner-cell' + (todayCol ? ' planner-cell--today' : '')}
          >
            {items.map(({ shift }) => {
              const dept = departmentById.get(shift.departmentId);
              const accent = deptColor(shift.departmentId);
              return (
                <div
                  key={shift.shiftId}
                  className="planner-block planner-block--unassigned"
                  style={{ ['--block-accent' as string]: accent }}
                  role={mayWrite ? 'button' : undefined}
                  onClick={() => onAssignClick(shift.shiftId)}
                >
                  <div className="planner-block-time">
                    {formatTime(shift.startDatetime)} –{' '}
                    {formatTime(shift.endDatetime)}
                  </div>
                  {shift.shiftName && (
                    <div className="planner-block-name">{shift.shiftName}</div>
                  )}
                  {dept && (
                    <div className="planner-block-dept">
                      <span className="planner-block-dept-dot" />
                      {dept.departmentName}
                    </div>
                  )}
                  {mayWrite && (
                    <div className="planner-block-assign">+ Assign employee</div>
                  )}
                </div>
              );
            })}
          </div>
        );
      })}
    </>
  );
}

interface PlannerRowProps {
  employee: Employee;
  cells: { day: Date; shift: Shift; assignment: ShiftAssignment }[];
  totalHours: number;
  weekDays: Date[];
  today: Date;
  mayWrite: boolean;
  departmentById: Map<number, Department>;
  onBlockClick: (shiftId: number) => void;
  onCellClick: (employeeId: number, day: Date) => void;
}

function PlannerRow({
  employee,
  cells,
  totalHours,
  weekDays,
  today,
  mayWrite,
  departmentById,
  onBlockClick,
  onCellClick,
}: PlannerRowProps) {
  const cellsByDay = new Map<string, PlannerRowProps['cells']>();
  for (const c of cells) {
    const key = c.day.toDateString();
    const list = cellsByDay.get(key) ?? [];
    list.push(c);
    cellsByDay.set(key, list);
  }

  return (
    <>
      <div className="planner-cell planner-employee">
        <div
          className="planner-avatar"
          style={{ background: avatarColor(employee.employeeId) }}
        >
          {initials(employee)}
        </div>
        <div className="planner-employee-meta">
          <span className="planner-employee-name">
            {employee.firstName} {employee.lastName}
          </span>
          <span className="planner-employee-hours">
            {formatHours(totalHours)} this week
          </span>
        </div>
      </div>
      {weekDays.map((d) => {
        const items = cellsByDay.get(d.toDateString()) ?? [];
        const todayCol = isSameDay(d, today);
        const cellClass =
          'planner-cell' +
          (mayWrite ? ' planner-row-add' : '') +
          (todayCol ? ' planner-cell--today' : '');
        return (
          <div
            key={d.toISOString()}
            className={cellClass}
            onClick={(e) => {
              if (e.target !== e.currentTarget) return;
              onCellClick(employee.employeeId, d);
            }}
          >
            {items.map(({ shift, assignment }) => {
              const statusClass = shift.shiftStatus
                ? ` planner-block--status-${shift.shiftStatus.toLowerCase()}`
                : '';
              const dept = departmentById.get(shift.departmentId);
              const accent = deptColor(shift.departmentId);
              return (
                <div
                  key={assignment.shiftAssignmentId}
                  className={`planner-block${statusClass}`}
                  style={{ ['--block-accent' as string]: accent }}
                  onClick={(e) => {
                    e.stopPropagation();
                    onBlockClick(shift.shiftId);
                  }}
                >
                  <div className="planner-block-time">
                    {formatTime(shift.startDatetime)} –{' '}
                    {formatTime(shift.endDatetime)}
                  </div>
                  {shift.shiftName && (
                    <div className="planner-block-name">{shift.shiftName}</div>
                  )}
                  {dept && (
                    <div className="planner-block-dept">
                      <span className="planner-block-dept-dot" />
                      {dept.departmentName}
                    </div>
                  )}
                </div>
              );
            })}
            {mayWrite && items.length === 0 && (
              <span className="planner-empty-hint">+</span>
            )}
          </div>
        );
      })}
    </>
  );
}

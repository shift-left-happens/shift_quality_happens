import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listEmployees, deleteEmployee } from '../../api/employees';
import { listUserRoles } from '../../api/userRoles';
import type { Employee, UserRole } from '../../api/types';
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

function initials(e: Employee): string {
  const f = e.firstName?.[0] ?? '';
  const l = e.lastName?.[0] ?? '';
  return (f + l).toUpperCase() || '?';
}

function rolePillClass(name: string): string {
  const n = name.toLowerCase();
  if (n.includes('admin')) return 'pill pill-admin';
  if (n.includes('manager')) return 'pill pill-manager';
  return 'pill pill-neutral';
}

function statusPillClass(status: string | null): string {
  if (!status) return 'pill pill-neutral';
  const s = status.toLowerCase();
  if (s === 'active' || s === 'employed') return 'pill pill-success';
  if (s === 'inactive' || s === 'terminated') return 'pill pill-danger';
  return 'pill pill-neutral';
}

export default function EmployeesListPage() {
  const { user } = useAuth();
  const mayWrite = canWrite(user?.roleName);
  const [employees, setEmployees] = useState<Employee[] | null>(null);
  const [roles, setRoles] = useState<UserRole[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([listEmployees(), listUserRoles()])
      .then(([es, rs]) => {
        if (cancelled) return;
        setEmployees(es);
        setRoles(rs);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : 'Failed to load employees');
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const roleName = (id: number) =>
    roles.find((r) => r.userRoleId === id)?.userRoleName ?? `#${id}`;

  async function handleDelete(id: number, label: string) {
    if (!confirm(`Delete employee "${label}"? This cannot be undone.`)) return;
    try {
      await deleteEmployee(id);
      setEmployees((prev) => prev?.filter((e) => e.employeeId !== id) ?? null);
    } catch (err) {
      alert(err instanceof ApiError ? err.message : 'Delete failed');
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Employees</h1>
          <p className="page-subtitle">Manage the people on your team.</p>
        </div>
        {mayWrite && (
          <Link to="/employees/new" className="btn btn-primary">
            New employee
          </Link>
        )}
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {!employees && !error && <div className="page-muted">Loading…</div>}

      {employees && (
        <>
          <div className="data-table-toolbar">
            <span className="data-table-count">
              {employees.length} {employees.length === 1 ? 'person' : 'people'}
            </span>
          </div>

          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Number</th>
                <th>Role</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {employees.length === 0 && (
                <tr>
                  <td colSpan={5} className="page-muted">
                    No employees yet.
                  </td>
                </tr>
              )}
              {employees.map((e) => {
                const rName = roleName(e.fkUserRoleId);
                return (
                  <tr key={e.employeeId}>
                    <td>
                      <Link
                        to={`/employees/${e.employeeId}`}
                        className="name-cell hover:no-underline"
                      >
                        <span
                          className="avatar"
                          style={{ background: avatarColor(e.employeeId) }}
                        >
                          {initials(e)}
                        </span>
                        <span>
                          <span className="name-cell-primary">
                            {e.firstName} {e.lastName}
                          </span>
                          <span className="name-cell-secondary">{e.email}</span>
                        </span>
                      </Link>
                    </td>
                    <td className="text-slate-500">{e.employeeNumber ?? '—'}</td>
                    <td>
                      <span className={rolePillClass(rName)}>{rName}</span>
                    </td>
                    <td>
                      <span className={statusPillClass(e.employmentStatus)}>
                        <span className="pill-dot" />
                        {e.employmentStatus ?? 'Unknown'}
                      </span>
                    </td>
                    <td className="data-table-actions">
                      {mayWrite && (
                        <>
                          <Link
                            to={`/employees/${e.employeeId}`}
                            className="btn btn-sm"
                          >
                            Edit
                          </Link>
                          <button
                            type="button"
                            className="btn btn-sm btn-danger"
                            onClick={() =>
                              handleDelete(
                                e.employeeId,
                                `${e.firstName} ${e.lastName}`
                              )
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

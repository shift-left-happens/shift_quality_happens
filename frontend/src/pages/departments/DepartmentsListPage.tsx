import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listDepartments, deleteDepartment } from '../../api/departments';
import type { Department } from '../../api/types';
import { ApiError } from '../../api/types';
import { useAuth } from '../../auth/useAuth';
import { canWrite } from '../../auth/roles';

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

function deptColor(id: number): string {
  return DEPT_COLORS[Math.abs(id) % DEPT_COLORS.length];
}

export default function DepartmentsListPage() {
  const { user } = useAuth();
  const mayWrite = canWrite(user?.roleName);
  const [departments, setDepartments] = useState<Department[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listDepartments()
      .then((ds) => {
        if (!cancelled) setDepartments(ds);
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : 'Failed to load departments');
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  async function handleDelete(id: number, label: string) {
    if (!confirm(`Delete department "${label}"?`)) return;
    try {
      await deleteDepartment(id);
      setDepartments((prev) => prev?.filter((d) => d.departmentId !== id) ?? null);
    } catch (err) {
      alert(err instanceof ApiError ? err.message : 'Delete failed');
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Departments</h1>
          <p className="page-subtitle">
            Organize your team by area of responsibility.
          </p>
        </div>
        {mayWrite && (
          <Link to="/departments/new" className="btn btn-primary">
            New department
          </Link>
        )}
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {!departments && !error && <div className="page-muted">Loading…</div>}

      {departments && (
        <>
          <div className="data-table-toolbar">
            <span className="data-table-count">
              {departments.length}{' '}
              {departments.length === 1 ? 'department' : 'departments'}
            </span>
          </div>

          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Status</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {departments.length === 0 && (
                <tr>
                  <td colSpan={3} className="page-muted">
                    No departments yet.
                  </td>
                </tr>
              )}
              {departments.map((d) => (
                <tr key={d.departmentId}>
                  <td>
                    <Link
                      to={`/departments/${d.departmentId}`}
                      className="name-cell hover:no-underline"
                    >
                      <span
                        className="inline-block h-2.5 w-2.5 rounded-full"
                        style={{ background: deptColor(d.departmentId) }}
                      />
                      <span className="name-cell-primary">
                        {d.departmentName ?? '—'}
                      </span>
                    </Link>
                  </td>
                  <td>
                    <span
                      className={
                        d.isActive ? 'pill pill-success' : 'pill pill-neutral'
                      }
                    >
                      <span className="pill-dot" />
                      {d.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td className="data-table-actions">
                    {mayWrite && (
                      <>
                        <Link
                          to={`/departments/${d.departmentId}`}
                          className="btn btn-sm"
                        >
                          Edit
                        </Link>
                        <button
                          type="button"
                          className="btn btn-sm btn-danger"
                          onClick={() =>
                            handleDelete(d.departmentId, d.departmentName ?? '—')
                          }
                        >
                          Delete
                        </button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </div>
  );
}

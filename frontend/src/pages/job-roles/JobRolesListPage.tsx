import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { listJobRoles, deleteJobRole } from '../../api/jobRoles';
import type { JobRole } from '../../api/types';
import { ApiError } from '../../api/types';
import { useAuth } from '../../auth/useAuth';
import { canWrite } from '../../auth/roles';

export default function JobRolesListPage() {
  const { user } = useAuth();
  const mayWrite = canWrite(user?.roleName);
  const [jobRoles, setJobRoles] = useState<JobRole[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    listJobRoles()
      .then((rs) => {
        if (!cancelled) setJobRoles(rs);
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.message : 'Failed to load job roles');
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  async function handleDelete(id: number, label: string) {
    if (!confirm(`Delete job role "${label}"?`)) return;
    try {
      await deleteJobRole(id);
      setJobRoles((prev) => prev?.filter((r) => r.jobRoleId !== id) ?? null);
    } catch (err) {
      alert(err instanceof ApiError ? err.message : 'Delete failed');
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Job Roles</h1>
          <p className="page-subtitle">
            Define the roles employees can hold and shifts can require.
          </p>
        </div>
        {mayWrite && (
          <Link to="/job-roles/new" className="btn btn-primary">
            New job role
          </Link>
        )}
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {!jobRoles && !error && <div className="page-muted">Loading…</div>}

      {jobRoles && (
        <>
          <div className="data-table-toolbar">
            <span className="data-table-count">
              {jobRoles.length} {jobRoles.length === 1 ? 'job role' : 'job roles'}
            </span>
          </div>

          <table className="data-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Description</th>
                <th>Certification</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {jobRoles.length === 0 && (
                <tr>
                  <td colSpan={4} className="page-muted">
                    No job roles yet.
                  </td>
                </tr>
              )}
              {jobRoles.map((r) => (
                <tr key={r.jobRoleId}>
                  <td>
                    <Link
                      to={`/job-roles/${r.jobRoleId}`}
                      className="name-cell hover:no-underline"
                    >
                      <span className="name-cell-primary">
                        {r.roleName ?? '—'}
                      </span>
                    </Link>
                  </td>
                  <td className="text-slate-500">
                    {r.jobRoleDescription ?? '—'}
                  </td>
                  <td>
                    <span
                      className={
                        r.isCertificationRequired
                          ? 'pill pill-brand'
                          : 'pill pill-neutral'
                      }
                    >
                      <span className="pill-dot" />
                      {r.isCertificationRequired ? 'Required' : 'Not required'}
                    </span>
                  </td>
                  <td className="data-table-actions">
                    {mayWrite && (
                      <>
                        <Link
                          to={`/job-roles/${r.jobRoleId}`}
                          className="btn btn-sm"
                        >
                          Edit
                        </Link>
                        <button
                          type="button"
                          className="btn btn-sm btn-danger"
                          onClick={() =>
                            handleDelete(r.jobRoleId, r.roleName ?? '—')
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

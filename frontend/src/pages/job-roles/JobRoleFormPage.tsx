import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { createJobRole, getJobRole, updateJobRole } from '../../api/jobRoles';
import type { NewJobRole } from '../../api/types';
import { ApiError } from '../../api/types';
import { useAuth } from '../../auth/useAuth';
import { canWrite } from '../../auth/roles';

const EMPTY_FORM: NewJobRole = {
  roleName: '',
  jobRoleDescription: '',
  isCertificationRequired: false,
};

export default function JobRoleFormPage() {
  const { id } = useParams<{ id: string }>();
  const isNew = id === 'new' || id === undefined;
  const numericId = isNew ? null : Number(id);

  const navigate = useNavigate();
  const { user } = useAuth();
  const mayWrite = canWrite(user?.roleName);

  const [form, setForm] = useState<NewJobRole>(EMPTY_FORM);
  const [loading, setLoading] = useState(!isNew);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (isNew || numericId === null) return;
    let cancelled = false;
    setLoading(true);
    getJobRole(numericId)
      .then((r) => {
        if (cancelled) return;
        setForm({
          roleName: r.roleName,
          jobRoleDescription: r.jobRoleDescription,
          isCertificationRequired: r.isCertificationRequired ?? false,
        });
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(err instanceof ApiError ? err.message : 'Failed to load job role');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [isNew, numericId]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!mayWrite) return;
    setError(null);
    setSubmitting(true);
    try {
      if (isNew) await createJobRole(form);
      else if (numericId !== null) await updateJobRole(numericId, form);
      navigate('/job-roles');
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Save failed');
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) return <div className="page page-muted">Loading…</div>;

  return (
    <div className="page">
      <h1 className="page-title">{isNew ? 'New job role' : 'Edit job role'}</h1>
      {error && <div className="alert alert-error">{error}</div>}
      <form className="form-grid" onSubmit={handleSubmit}>
        <label className="form-field">
          <span>Name</span>
          <input
            required
            value={form.roleName ?? ''}
            onChange={(e) =>
              setForm((f) => ({ ...f, roleName: e.target.value }))
            }
            disabled={!mayWrite}
          />
        </label>
        <label className="form-field">
          <span>Description</span>
          <input
            value={form.jobRoleDescription ?? ''}
            onChange={(e) =>
              setForm((f) => ({ ...f, jobRoleDescription: e.target.value }))
            }
            disabled={!mayWrite}
          />
        </label>
        <label className="form-field">
          <span>Certification required</span>
          <select
            value={form.isCertificationRequired ? 'yes' : 'no'}
            onChange={(e) =>
              setForm((f) => ({
                ...f,
                isCertificationRequired: e.target.value === 'yes',
              }))
            }
            disabled={!mayWrite}
          >
            <option value="no">No</option>
            <option value="yes">Yes</option>
          </select>
        </label>
        <div className="form-actions">
          <button
            type="button"
            className="btn"
            onClick={() => navigate('/job-roles')}
          >
            Cancel
          </button>
          {mayWrite && (
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Saving…' : isNew ? 'Create' : 'Save changes'}
            </button>
          )}
        </div>
      </form>
    </div>
  );
}

import { useEffect, useState } from 'react';
import { listUpcomingHolidays } from '../api/holidays';
import type { Holiday } from '../api/types';

type WidgetState =
  | { status: 'loading' }
  | { status: 'error' }
  | { status: 'ready'; holidays: Holiday[] };

function formatDate(iso: string): string {
  const d = new Date(`${iso}T00:00:00`);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString(undefined, {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  });
}

function countdown(iso: string): string {
  const target = new Date(`${iso}T00:00:00`);
  if (Number.isNaN(target.getTime())) return '';
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const days = Math.round((target.getTime() - today.getTime()) / 86_400_000);
  if (days <= 0) return 'Today';
  if (days === 1) return 'Tomorrow';
  if (days < 7) return `in ${days} days`;
  if (days < 14) return 'in 1 week';
  return `in ${Math.round(days / 7)} weeks`;
}

/**
 * Dashboard widget listing the next public holidays, fetched from the
 * backend's external-API proxy. Fails quietly — if the third-party service
 * is down, the rest of the dashboard is unaffected.
 */
export default function UpcomingHolidaysWidget() {
  const [state, setState] = useState<WidgetState>({ status: 'loading' });

  useEffect(() => {
    let cancelled = false;
    listUpcomingHolidays('DK', 5)
      .then((holidays) => {
        if (!cancelled) setState({ status: 'ready', holidays });
      })
      .catch(() => {
        if (!cancelled) setState({ status: 'error' });
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <section className="page-section">
      <h2 className="page-section-title">Upcoming public holidays</h2>

      {state.status === 'loading' && (
        <div className="empty-state">
          <p>Loading holidays…</p>
        </div>
      )}

      {state.status === 'error' && (
        <div className="empty-state">
          <p>Holiday calendar is unavailable right now.</p>
        </div>
      )}

      {state.status === 'ready' && state.holidays.length === 0 && (
        <div className="empty-state">
          <p>No upcoming public holidays found.</p>
        </div>
      )}

      {state.status === 'ready' && state.holidays.length > 0 && (
        <ul className="flex flex-col gap-2" style={{ maxWidth: 520 }}>
          {state.holidays.map((h) => (
            <li
              key={`${h.date}-${h.name}`}
              className="flex items-center justify-between gap-3 rounded-[10px] border border-slate-200 bg-white px-4 py-3 shadow-card"
            >
              <div className="min-w-0">
                <div className="truncate font-semibold text-slate-900">
                  {h.localName ?? h.name ?? 'Public holiday'}
                </div>
                {h.localName && h.name && h.localName !== h.name && (
                  <div className="truncate text-sm text-slate-500">
                    {h.name}
                  </div>
                )}
              </div>
              <div className="shrink-0 text-right">
                <div className="text-sm font-medium text-slate-700">
                  {formatDate(h.date)}
                </div>
                <div className="text-xs text-slate-400">
                  {countdown(h.date)}
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

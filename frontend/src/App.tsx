import { useEffect, useMemo, useState } from 'react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

type DashboardOverview = {
  taskCount: number;
  avgLeadTimeSeconds: number;
  avgRiskScore: number;
  avgReworkRate: number;
  snapshots: SnapshotPoint[];
};

type SnapshotPoint = {
  date: string;
  prefix: string;
  avgLeadTime: number;
  avgRiskScore: number;
  driftRate: number;
  reworkRate: number;
};

export function App() {
  const [overview, setOverview] = useState<DashboardOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    fetch('/api/kpi/dashboard/overview?days=14', { signal: controller.signal })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`Failed to load dashboard: ${response.status}`);
        }
        return response.json() as Promise<DashboardOverview>;
      })
      .then((payload) => {
        setOverview(payload);
        setError(null);
      })
      .catch((fetchError) => {
        if (fetchError instanceof Error && fetchError.name === 'AbortError') {
          return;
        }
        setError(fetchError instanceof Error ? fetchError.message : 'Unknown error');
      })
      .finally(() => {
        setLoading(false);
      });

    return () => controller.abort();
  }, []);

  const chartData = useMemo(() => {
    if (!overview) {
      return [];
    }
    const byDate = new Map<string, { riskTotal: number; leadTotal: number; count: number }>();
    for (const snapshot of overview.snapshots) {
      const existing = byDate.get(snapshot.date) ?? { riskTotal: 0, leadTotal: 0, count: 0 };
      existing.riskTotal += snapshot.avgRiskScore ?? 0;
      existing.leadTotal += snapshot.avgLeadTime ?? 0;
      existing.count += 1;
      byDate.set(snapshot.date, existing);
    }

    return Array.from(byDate.entries()).map(([date, aggregate]) => ({
      date,
      risk: Number((aggregate.riskTotal / aggregate.count).toFixed(2)),
      leadHours: Number(((aggregate.leadTotal / aggregate.count) / 3600).toFixed(2)),
    }));
  }, [overview]);

  const riskClass =
    (overview?.avgRiskScore ?? 0) <= 30
      ? 'text-emerald-600'
      : (overview?.avgRiskScore ?? 0) <= 60
        ? 'text-amber-600'
        : 'text-rose-600';

  return (
    <main className="min-h-screen bg-slate-50 p-6 text-slate-900">
      <section className="mx-auto max-w-6xl rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <h1 className="text-2xl font-semibold">Snayvik KPI Governance Dashboard</h1>
        <p className="mt-2 text-sm text-slate-600">Lead time, risk, rework and snapshot trends from async KPI computation.</p>

        <div className="mt-6 grid gap-4 md:grid-cols-4">
          <article className="rounded-lg border border-slate-200 p-4">
            <p className="text-xs uppercase tracking-wide text-slate-500">Tracked Tasks</p>
            <p className="mt-1 text-2xl font-semibold">{overview?.taskCount ?? 0}</p>
          </article>
          <article className="rounded-lg border border-slate-200 p-4">
            <p className="text-xs uppercase tracking-wide text-slate-500">Avg Lead Time</p>
            <p className="mt-1 text-2xl font-semibold">{(((overview?.avgLeadTimeSeconds ?? 0) / 3600).toFixed(1))}h</p>
          </article>
          <article className="rounded-lg border border-slate-200 p-4">
            <p className="text-xs uppercase tracking-wide text-slate-500">Avg Rework</p>
            <p className="mt-1 text-2xl font-semibold">{((overview?.avgReworkRate ?? 0) * 100).toFixed(1)}%</p>
          </article>
          <article className="rounded-lg border border-slate-200 p-4">
            <p className="text-xs uppercase tracking-wide text-slate-500">Avg Risk</p>
            <p className={`mt-1 text-2xl font-semibold ${riskClass}`}>{(overview?.avgRiskScore ?? 0).toFixed(2)}</p>
          </article>
        </div>

        {loading ? <p className="mt-4 text-sm text-slate-500">Loading dashboard data...</p> : null}
        {error ? <p className="mt-4 text-sm text-rose-600">{error}</p> : null}

        <div className="mt-6 h-80 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="date" stroke="#475569" />
              <YAxis yAxisId="risk" stroke="#475569" />
              <YAxis yAxisId="lead" orientation="right" stroke="#334155" />
              <Tooltip />
              <Bar yAxisId="risk" dataKey="risk" fill="#f59e0b" radius={[6, 6, 0, 0]} />
              <Bar yAxisId="lead" dataKey="leadHours" fill="#0ea5e9" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
        <p className="mt-2 text-xs text-slate-500">Orange: avg risk score, Blue: avg lead time (hours)</p>
      </section>
    </main>
  );
}

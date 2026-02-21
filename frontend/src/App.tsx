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

type GovernanceRule = {
  code: string;
  description: string;
};

type GovernanceResponse = {
  lastUpdatedAt: string;
  rules: GovernanceRule[];
  thresholds: PolicyItem[];
};

type PolicyItem = {
  id?: number;
  violationType: string;
  thresholdCount: number;
  timeWindowDays: number;
  escalationLevel: string;
  notifyEmail: string;
  notifySlack: string;
  active: boolean;
  updatedBy?: string;
  updatedAt?: string;
};

type PolicyChange = {
  id: number;
  thresholdId: number;
  oldValue: string;
  newValue: string;
  changedBy: string;
  changedAt: string;
};

type TimeSummaryRow = {
  taskKey: string;
  status: string;
  trackedMinutes: number;
  hasEngineeringActivity: boolean;
  trackedState: string;
};

function Header() {
  return (
    <header className="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <div>
        <h1 className="text-xl font-semibold text-slate-900">Snayvik KPI Governance</h1>
        <p className="text-sm text-slate-600">Operational visibility and policy discipline</p>
      </div>
      <nav className="flex items-center gap-2">
        <a href="/" className="rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-700 hover:bg-slate-100">
          Dashboard
        </a>
        <a
          href="/admin/policies"
          className="rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-700 hover:bg-slate-100"
        >
          Policies
        </a>
        <a
          href="/governance-rules"
          className="rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-700 hover:bg-slate-100"
        >
          Governance Rules
        </a>
        <a href="/time" className="rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-700 hover:bg-slate-100">
          Time
        </a>
      </nav>
    </header>
  );
}

function DashboardPage() {
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
    <section className="mx-auto max-w-6xl rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-2xl font-semibold">Executive Dashboard</h2>
      <p className="mt-2 text-sm text-slate-600">Lead time, risk, rework and snapshot trends from async KPI computation.</p>

      <div className="mt-6 grid gap-4 md:grid-cols-4">
        <article className="rounded-lg border border-slate-200 p-4">
          <p className="text-xs uppercase tracking-wide text-slate-500">Tracked Tasks</p>
          <p className="mt-1 text-2xl font-semibold">{overview?.taskCount ?? 0}</p>
        </article>
        <article className="rounded-lg border border-slate-200 p-4">
          <p className="text-xs uppercase tracking-wide text-slate-500">Avg Lead Time</p>
          <p className="mt-1 text-2xl font-semibold">{((overview?.avgLeadTimeSeconds ?? 0) / 3600).toFixed(1)}h</p>
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
  );
}

function GovernanceRulesPage() {
  const [payload, setPayload] = useState<GovernanceResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    fetch('/api/kpi/governance-rules', { signal: controller.signal })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`Failed to load governance rules: ${response.status}`);
        }
        return response.json() as Promise<GovernanceResponse>;
      })
      .then((data) => {
        setPayload(data);
        setError(null);
      })
      .catch((fetchError) => {
        if (fetchError instanceof Error && fetchError.name === 'AbortError') {
          return;
        }
        setError(fetchError instanceof Error ? fetchError.message : 'Unknown error');
      });
    return () => controller.abort();
  }, []);

  return (
    <section className="mx-auto max-w-6xl rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-2xl font-semibold">Governance Rules</h2>
      <p className="mt-2 text-sm text-slate-600">Transparent view of active policy rules and their latest update timestamp.</p>
      <p className="mt-2 text-xs text-slate-500">Last updated: {payload?.lastUpdatedAt ?? '-'}</p>
      {error ? <p className="mt-4 text-sm text-rose-600">{error}</p> : null}
      <div className="mt-4 space-y-3">
        {(payload?.rules ?? []).map((rule) => (
          <article key={rule.code} className="rounded-lg border border-slate-200 p-4">
            <p className="text-sm font-semibold text-slate-900">{rule.code}</p>
            <p className="mt-1 text-sm text-slate-600">{rule.description}</p>
          </article>
        ))}
      </div>
      <h3 className="mt-6 text-lg font-semibold">Thresholds</h3>
      <div className="mt-3 space-y-2">
        {(payload?.thresholds ?? []).map((threshold) => (
          <article key={`${threshold.id}-${threshold.violationType}`} className="rounded-lg border border-slate-200 p-3">
            <p className="text-sm font-semibold text-slate-900">{threshold.violationType}</p>
            <p className="text-xs text-slate-600">
              threshold={threshold.thresholdCount} windowDays={threshold.timeWindowDays} escalation={threshold.escalationLevel}
            </p>
          </article>
        ))}
      </div>
    </section>
  );
}

function AdminPoliciesPage() {
  const [policies, setPolicies] = useState<PolicyItem[]>([]);
  const [changes, setChanges] = useState<PolicyChange[]>([]);
  const [form, setForm] = useState<PolicyItem>({
    violationType: 'MISSING_TASK_KEY',
    thresholdCount: 3,
    timeWindowDays: 7,
    escalationLevel: 'WARN',
    notifyEmail: '',
    notifySlack: '',
    active: true,
  });
  const [message, setMessage] = useState<string>('');

  const loadData = () => {
    Promise.all([fetch('/api/kpi/admin/policies'), fetch('/api/kpi/admin/policies/changes')])
      .then(async ([policiesRes, changesRes]) => {
        if (!policiesRes.ok || !changesRes.ok) {
          throw new Error('Failed to load policy data');
        }
        const policiesData = (await policiesRes.json()) as PolicyItem[];
        const changesData = (await changesRes.json()) as PolicyChange[];
        setPolicies(policiesData);
        setChanges(changesData);
      })
      .catch((err) => {
        setMessage(err instanceof Error ? err.message : 'Failed to load data');
      });
  };

  useEffect(() => {
    loadData();
  }, []);

  const savePolicy = () => {
    fetch('/api/kpi/admin/policies', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Actor': 'admin-ui',
      },
      body: JSON.stringify(form),
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`Failed to save policy: ${response.status}`);
        }
        await response.json();
        setMessage('Policy saved successfully');
        loadData();
      })
      .catch((err) => {
        setMessage(err instanceof Error ? err.message : 'Save failed');
      });
  };

  return (
    <section className="mx-auto max-w-6xl rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-2xl font-semibold">Threshold Policies</h2>
      <p className="mt-2 text-sm text-slate-600">Edit threshold policies for automated Slack and email notifications.</p>
      <div className="mt-4 grid gap-3 md:grid-cols-2">
        <input
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          value={form.violationType}
          onChange={(event) => setForm({ ...form, violationType: event.target.value })}
          placeholder="Violation Type"
        />
        <input
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          type="number"
          value={form.thresholdCount}
          onChange={(event) => setForm({ ...form, thresholdCount: Number(event.target.value) })}
          placeholder="Threshold Count"
        />
        <input
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          type="number"
          value={form.timeWindowDays}
          onChange={(event) => setForm({ ...form, timeWindowDays: Number(event.target.value) })}
          placeholder="Time Window Days"
        />
        <input
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          value={form.escalationLevel}
          onChange={(event) => setForm({ ...form, escalationLevel: event.target.value })}
          placeholder="Escalation Level"
        />
        <input
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          value={form.notifyEmail}
          onChange={(event) => setForm({ ...form, notifyEmail: event.target.value })}
          placeholder="Notify Email"
        />
        <input
          className="rounded-md border border-slate-300 px-3 py-2 text-sm"
          value={form.notifySlack}
          onChange={(event) => setForm({ ...form, notifySlack: event.target.value })}
          placeholder="Notify Slack"
        />
      </div>
      <button
        type="button"
        className="mt-4 rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700"
        onClick={savePolicy}
      >
        Save Policy
      </button>
      {message ? <p className="mt-3 text-sm text-slate-600">{message}</p> : null}

      <h3 className="mt-6 text-lg font-semibold">Current Policies</h3>
      <div className="mt-3 space-y-2">
        {policies.map((policy) => (
          <article key={`${policy.id}-${policy.violationType}`} className="rounded-lg border border-slate-200 p-3">
            <p className="text-sm font-semibold text-slate-900">{policy.violationType}</p>
            <p className="text-xs text-slate-600">
              threshold={policy.thresholdCount} windowDays={policy.timeWindowDays} active={String(policy.active)}
            </p>
          </article>
        ))}
      </div>

      <h3 className="mt-6 text-lg font-semibold">Recent Changes</h3>
      <div className="mt-3 space-y-2">
        {changes.map((change) => (
          <article key={change.id} className="rounded-lg border border-slate-200 p-3">
            <p className="text-xs text-slate-700">
              thresholdId={change.thresholdId} by={change.changedBy}
            </p>
            <p className="text-xs text-slate-500">{change.changedAt}</p>
          </article>
        ))}
      </div>
    </section>
  );
}

function TimeAccountabilityPage() {
  const [rows, setRows] = useState<TimeSummaryRow[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch('/api/kpi/time/summary')
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`Failed to load time summary: ${response.status}`);
        }
        return response.json() as Promise<TimeSummaryRow[]>;
      })
      .then((data) => {
        setRows(data);
        setError(null);
      })
      .catch((fetchError) => {
        setError(fetchError instanceof Error ? fetchError.message : 'Unknown error');
      });
  }, []);

  return (
    <section className="mx-auto max-w-6xl rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-2xl font-semibold">Time Accountability</h2>
      <p className="mt-2 text-sm text-slate-600">Tracked vs untracked time by task based on GitHub/monday activity.</p>
      {error ? <p className="mt-3 text-sm text-rose-600">{error}</p> : null}
      <div className="mt-4 overflow-x-auto">
        <table className="min-w-full border-collapse text-sm">
          <thead>
            <tr className="border-b border-slate-200 text-left text-slate-600">
              <th className="px-2 py-2">Task</th>
              <th className="px-2 py-2">Status</th>
              <th className="px-2 py-2">Tracked Minutes</th>
              <th className="px-2 py-2">Engineering Activity</th>
              <th className="px-2 py-2">State</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.taskKey} className="border-b border-slate-100">
                <td className="px-2 py-2 font-medium text-slate-900">{row.taskKey}</td>
                <td className="px-2 py-2 text-slate-600">{row.status || '-'}</td>
                <td className="px-2 py-2 text-slate-600">{row.trackedMinutes}</td>
                <td className="px-2 py-2 text-slate-600">{String(row.hasEngineeringActivity)}</td>
                <td className={`px-2 py-2 ${row.trackedState === 'TRACKED' ? 'text-emerald-600' : 'text-rose-600'}`}>
                  {row.trackedState}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

export function App() {
  const path = window.location.pathname;
  return (
    <main className="min-h-screen bg-slate-50 p-6 text-slate-900">
      <Header />
      {path.startsWith('/governance-rules') ? <GovernanceRulesPage /> : null}
      {path.startsWith('/admin/policies') ? <AdminPoliciesPage /> : null}
      {path.startsWith('/time') ? <TimeAccountabilityPage /> : null}
      {!path.startsWith('/governance-rules') && !path.startsWith('/admin/policies') && !path.startsWith('/time') ? (
        <DashboardPage />
      ) : null}
    </main>
  );
}

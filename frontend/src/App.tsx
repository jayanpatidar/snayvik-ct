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

type AccessUser = {
  id: string;
  email: string;
  name: string;
  active: boolean;
};

type AccessAuditRow = {
  id: number;
  userId: string;
  system: string;
  action: string;
  status: string;
  performedBy: string;
  timestamp: string;
};

type HealthSummary = {
  timestamp: string;
  queueName: string;
  queueBacklog: number;
  webhookReceived: number;
  webhookProcessed: number;
  webhookFailed: number;
};

type IntegrationSystem = 'GITHUB' | 'MONDAY' | 'SLACK' | 'EMAIL';

type IntegrationConnectionView = {
  system: IntegrationSystem;
  active: boolean;
  settings: Record<string, string>;
  hasSecret: boolean;
  secretUpdatedAt?: string;
  updatedBy?: string;
  updatedAt?: string;
};

type IntegrationConnectionTestResult = {
  success: boolean;
  message: string;
  missing: string[];
};

type IntegrationDraft = {
  active: boolean;
  settings: Record<string, string>;
  secret: string;
  clearSecret: boolean;
  hasSecret: boolean;
  secretUpdatedAt?: string;
  updatedBy?: string;
  updatedAt?: string;
};

type RepoMappingView = {
  repository: string;
  enabled: boolean;
  allowedPrefixes: string[];
  updatedBy?: string;
  updatedAt?: string;
};

type RepoMappingEditorRow = {
  repository: string;
  enabled: boolean;
  allowedPrefixes: string;
};

type BoardMappingView = {
  prefix: string;
  boardId: string;
  boardName: string;
};

type SyncRunReport = {
  runType: string;
  enabled: boolean;
  startedAt: string;
  finishedAt: string;
  boardsScanned: number;
  mondayItemsProcessed: number;
  githubRepositoriesScanned: number;
  githubPullRequestsProcessed: number;
  githubCommitsProcessed: number;
  touchedTasks: number;
  tasksRecomputed: number;
};

const INTEGRATION_SYSTEMS: IntegrationSystem[] = ['GITHUB', 'MONDAY', 'SLACK', 'EMAIL'];

const INTEGRATION_SETTING_FIELDS: Record<IntegrationSystem, Array<{ key: string; label: string; placeholder: string }>> = {
  GITHUB: [{ key: 'org', label: 'Organization', placeholder: 'snayvik-org' }],
  MONDAY: [{ key: 'apiUrl', label: 'API URL', placeholder: 'https://api.monday.com/v2' }],
  SLACK: [{ key: 'channel', label: 'Channel', placeholder: '#engineering-alerts' }],
  EMAIL: [
    { key: 'smtpHost', label: 'SMTP Host', placeholder: 'smtp.example.com' },
    { key: 'fromAddress', label: 'From Address', placeholder: 'kpi@snayvik.com' },
  ],
};

function createEmptyIntegrationDraft(): IntegrationDraft {
  return {
    active: false,
    settings: {},
    secret: '',
    clearSecret: false,
    hasSecret: false,
    secretUpdatedAt: undefined,
    updatedBy: undefined,
    updatedAt: undefined,
  };
}

function toIntegrationDraft(view?: IntegrationConnectionView): IntegrationDraft {
  if (!view) {
    return createEmptyIntegrationDraft();
  }
  return {
    active: view.active,
    settings: view.settings ?? {},
    secret: '',
    clearSecret: false,
    hasSecret: view.hasSecret,
    secretUpdatedAt: view.secretUpdatedAt,
    updatedBy: view.updatedBy,
    updatedAt: view.updatedAt,
  };
}

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
        <a
          href="/admin/access"
          className="rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-700 hover:bg-slate-100"
        >
          Access
        </a>
        <a
          href="/admin/integrations"
          className="rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-700 hover:bg-slate-100"
        >
          Integrations
        </a>
        <a href="/health" className="rounded-md border border-slate-200 px-3 py-2 text-sm text-slate-700 hover:bg-slate-100">
          Health
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

function IntegrationsPage() {
  const [actor, setActor] = useState<string>('admin-ui');
  const [connectionDrafts, setConnectionDrafts] = useState<Record<IntegrationSystem, IntegrationDraft>>(() =>
    Object.fromEntries(INTEGRATION_SYSTEMS.map((system) => [system, createEmptyIntegrationDraft()])) as Record<
      IntegrationSystem,
      IntegrationDraft
    >,
  );
  const [connectionMessages, setConnectionMessages] = useState<Partial<Record<IntegrationSystem, string>>>({});
  const [connectionTests, setConnectionTests] = useState<Partial<Record<IntegrationSystem, IntegrationConnectionTestResult>>>({});
  const [repoRows, setRepoRows] = useState<RepoMappingEditorRow[]>([]);
  const [boardRows, setBoardRows] = useState<BoardMappingView[]>([]);
  const [repoMessage, setRepoMessage] = useState<string>('');
  const [boardMessage, setBoardMessage] = useState<string>('');
  const [syncMessage, setSyncMessage] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string>('');

  const loadData = (oauthNotice?: { system: IntegrationSystem; message: string }) => {
    setLoading(true);
    Promise.all([
      fetch('/api/kpi/admin/integrations/connections'),
      fetch('/api/kpi/admin/integrations/repositories'),
      fetch('/api/kpi/admin/integrations/boards'),
    ])
      .then(async ([connectionsRes, reposRes, boardsRes]) => {
        if (!connectionsRes.ok || !reposRes.ok || !boardsRes.ok) {
          throw new Error(
            `Load failed: connections=${connectionsRes.status}, repos=${reposRes.status}, boards=${boardsRes.status}`,
          );
        }
        const connections = (await connectionsRes.json()) as IntegrationConnectionView[];
        const repos = (await reposRes.json()) as RepoMappingView[];
        const boards = (await boardsRes.json()) as BoardMappingView[];

        const bySystem = Object.fromEntries(
          INTEGRATION_SYSTEMS.map((system) => {
            const found = connections.find((item) => item.system === system);
            return [system, toIntegrationDraft(found)];
          }),
        ) as Record<IntegrationSystem, IntegrationDraft>;

        setConnectionDrafts(bySystem);
        setConnectionMessages(oauthNotice ? { [oauthNotice.system]: oauthNotice.message } : {});
        setConnectionTests({});
        setRepoRows(
          repos.map((row) => ({
            repository: row.repository,
            enabled: row.enabled,
            allowedPrefixes: row.allowedPrefixes.join(','),
          })),
        );
        setBoardRows(boards);
        setError('');
      })
      .catch((fetchError) => {
        setError(fetchError instanceof Error ? fetchError.message : 'Failed to load integration data');
      })
      .finally(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const oauthSystem = params.get('oauthSystem');
    const oauthStatus = params.get('oauthStatus');
    const oauthMessage = params.get('oauthMessage');

    let oauthNotice: { system: IntegrationSystem; message: string } | undefined;
    if (oauthSystem && oauthStatus && INTEGRATION_SYSTEMS.includes(oauthSystem as IntegrationSystem)) {
      const system = oauthSystem as IntegrationSystem;
      const messagePrefix = oauthStatus === 'connected' ? 'SSO connected' : 'SSO failed';
      oauthNotice = { system, message: `${messagePrefix}: ${oauthMessage ?? '-'}` };
      window.history.replaceState({}, '', window.location.pathname);
    }

    loadData(oauthNotice);
  }, []);

  const setDraft = (system: IntegrationSystem, updater: (draft: IntegrationDraft) => IntegrationDraft) => {
    setConnectionDrafts((previous) => ({ ...previous, [system]: updater(previous[system]) }));
  };

  const setSetting = (system: IntegrationSystem, key: string, value: string) => {
    setDraft(system, (draft) => ({
      ...draft,
      settings: {
        ...draft.settings,
        [key]: value,
      },
    }));
  };

  const saveConnection = (system: IntegrationSystem) => {
    const draft = connectionDrafts[system];
    const payload: {
      active: boolean;
      settings: Record<string, string>;
      secret?: string;
      clearSecret?: boolean;
    } = {
      active: draft.active,
      settings: draft.settings,
    };
    if (draft.secret.trim()) {
      payload.secret = draft.secret.trim();
    }
    if (draft.clearSecret) {
      payload.clearSecret = true;
    }

    fetch(`/api/kpi/admin/integrations/connections/${system}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'X-Actor': actor,
      },
      body: JSON.stringify(payload),
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`Save failed for ${system}: ${response.status}`);
        }
        return response.json() as Promise<IntegrationConnectionView>;
      })
      .then((saved) => {
        setDraft(system, () => toIntegrationDraft(saved));
        setConnectionMessages((previous) => ({ ...previous, [system]: 'Saved' }));
        setConnectionTests((previous) => {
          const copy = { ...previous };
          delete copy[system];
          return copy;
        });
      })
      .catch((saveError) => {
        setConnectionMessages((previous) => ({
          ...previous,
          [system]: saveError instanceof Error ? saveError.message : `Save failed for ${system}`,
        }));
      });
  };

  const testConnection = (system: IntegrationSystem) => {
    fetch(`/api/kpi/admin/integrations/connections/${system}/test`, { method: 'POST' })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`Test failed for ${system}: ${response.status}`);
        }
        return response.json() as Promise<IntegrationConnectionTestResult>;
      })
      .then((result) => {
        setConnectionTests((previous) => ({ ...previous, [system]: result }));
      })
      .catch((testError) => {
        setConnectionTests((previous) => ({
          ...previous,
          [system]: {
            success: false,
            message: testError instanceof Error ? testError.message : 'Connection test failed',
            missing: [],
          },
        }));
      });
  };

  const startSsoConnect = (system: IntegrationSystem) => {
    fetch(`/api/kpi/admin/integrations/oauth/${system}/authorize-url`)
      .then(async (response) => {
        if (!response.ok) {
          const responseBody = await response.text();
          throw new Error(`SSO authorize failed for ${system}: ${response.status} ${responseBody}`);
        }
        return response.json() as Promise<{ authorizationUrl: string }>;
      })
      .then((payload) => {
        if (!payload.authorizationUrl) {
          throw new Error('Missing authorization URL from server');
        }
        window.location.href = payload.authorizationUrl;
      })
      .catch((oauthError) => {
        setConnectionMessages((previous) => ({
          ...previous,
          [system]: oauthError instanceof Error ? oauthError.message : `SSO start failed for ${system}`,
        }));
      });
  };

  const saveRepositories = () => {
    const payload = repoRows
      .filter((row) => row.repository.trim())
      .map((row) => ({
        repository: row.repository.trim(),
        enabled: row.enabled,
        allowedPrefixes: row.allowedPrefixes
          .split(',')
          .map((value) => value.trim().toUpperCase())
          .filter((value) => value.length > 0),
      }));

    fetch('/api/kpi/admin/integrations/repositories', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'X-Actor': actor,
      },
      body: JSON.stringify(payload),
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`Failed to save repositories: ${response.status}`);
        }
        return response.json() as Promise<RepoMappingView[]>;
      })
      .then((saved) => {
        setRepoRows(
          saved.map((row) => ({
            repository: row.repository,
            enabled: row.enabled,
            allowedPrefixes: row.allowedPrefixes.join(','),
          })),
        );
        setRepoMessage('Repository mappings saved');
      })
      .catch((saveError) => {
        setRepoMessage(saveError instanceof Error ? saveError.message : 'Failed to save repositories');
      });
  };

  const saveBoards = () => {
    const payload = boardRows
      .filter((row) => row.prefix.trim() && row.boardId.trim())
      .map((row) => ({
        prefix: row.prefix.trim().toUpperCase(),
        boardId: row.boardId.trim(),
        boardName: row.boardName.trim(),
      }));

    fetch('/api/kpi/admin/integrations/boards', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`Failed to save boards: ${response.status}`);
        }
        return response.json() as Promise<BoardMappingView[]>;
      })
      .then((saved) => {
        setBoardRows(saved);
        setBoardMessage('Board mappings saved');
      })
      .catch((saveError) => {
        setBoardMessage(saveError instanceof Error ? saveError.message : 'Failed to save boards');
      });
  };

  const runSync = (mode: 'full' | 'reconcile') => {
    fetch(`/api/kpi/admin/sync/${mode}`, { method: 'POST' })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`${mode} sync failed: ${response.status}`);
        }
        return response.json() as Promise<SyncRunReport>;
      })
      .then((report) => {
        setSyncMessage(
          `${report.runType} ${report.enabled ? 'completed' : 'disabled'}: touchedTasks=${report.touchedTasks}, tasksRecomputed=${report.tasksRecomputed}`,
        );
      })
      .catch((syncError) => {
        setSyncMessage(syncError instanceof Error ? syncError.message : 'Sync request failed');
      });
  };

  return (
    <section className="mx-auto max-w-6xl rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-2xl font-semibold">Integration Linking</h2>
      <p className="mt-2 text-sm text-slate-600">
        Configure GitHub, monday, Slack and email connections, then manage repository and board mappings.
      </p>

      <div className="mt-4 rounded-lg border border-slate-200 p-4">
        <p className="text-sm font-semibold text-slate-900">Actor</p>
        <p className="mt-1 text-xs text-slate-600">Recorded as updater for connection and repository mapping changes.</p>
        <input
          className="mt-2 w-full rounded-md border border-slate-300 px-3 py-2 text-sm md:w-80"
          value={actor}
          onChange={(event) => setActor(event.target.value)}
          placeholder="admin-ui"
        />
      </div>

      {loading ? <p className="mt-4 text-sm text-slate-500">Loading integration data...</p> : null}
      {error ? <p className="mt-4 text-sm text-rose-600">{error}</p> : null}

      <div className="mt-6 grid gap-4 md:grid-cols-2">
        {INTEGRATION_SYSTEMS.map((system) => {
          const draft = connectionDrafts[system];
          const fields = INTEGRATION_SETTING_FIELDS[system];
          const testResult = connectionTests[system];
          const infoMessage = connectionMessages[system];
          return (
            <article key={system} className="rounded-lg border border-slate-200 p-4">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <h3 className="text-base font-semibold text-slate-900">{system}</h3>
                  <p className="text-xs text-slate-600">
                    Secret: {draft.hasSecret ? 'configured' : 'not configured'}
                    {draft.secretUpdatedAt ? ` (${draft.secretUpdatedAt})` : ''}
                  </p>
                </div>
                <label className="flex items-center gap-2 text-xs text-slate-700">
                  <input
                    type="checkbox"
                    checked={draft.active}
                    onChange={(event) => setDraft(system, (value) => ({ ...value, active: event.target.checked }))}
                  />
                  Active
                </label>
              </div>

              <div className="mt-3 space-y-2">
                {fields.map((field) => (
                  <input
                    key={field.key}
                    className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                    value={draft.settings[field.key] ?? ''}
                    onChange={(event) => setSetting(system, field.key, event.target.value)}
                    placeholder={`${field.label} (${field.placeholder})`}
                  />
                ))}
                <input
                  className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm"
                  type="password"
                  value={draft.secret}
                  onChange={(event) => setDraft(system, (value) => ({ ...value, secret: event.target.value, clearSecret: false }))}
                  placeholder="New secret/token (leave empty to keep existing)"
                />
                <label className="flex items-center gap-2 text-xs text-slate-700">
                  <input
                    type="checkbox"
                    checked={draft.clearSecret}
                    onChange={(event) => setDraft(system, (value) => ({ ...value, clearSecret: event.target.checked, secret: '' }))}
                  />
                  Clear existing secret
                </label>
              </div>

              <div className="mt-3 flex items-center gap-2">
                <button
                  type="button"
                  className="rounded-md bg-slate-900 px-3 py-2 text-xs font-medium text-white hover:bg-slate-700"
                  onClick={() => saveConnection(system)}
                >
                  Save
                </button>
                <button
                  type="button"
                  className="rounded-md border border-slate-300 px-3 py-2 text-xs text-slate-700 hover:bg-slate-50"
                  onClick={() => testConnection(system)}
                >
                  Test
                </button>
                {system !== 'EMAIL' ? (
                  <button
                    type="button"
                    className="rounded-md border border-slate-300 px-3 py-2 text-xs text-slate-700 hover:bg-slate-50"
                    onClick={() => startSsoConnect(system)}
                  >
                    Connect via SSO
                  </button>
                ) : null}
              </div>
              {infoMessage ? <p className="mt-2 text-xs text-slate-600">{infoMessage}</p> : null}
              {draft.updatedAt ? (
                <p className="mt-1 text-xs text-slate-500">
                  Updated by {draft.updatedBy ?? '-'} at {draft.updatedAt}
                </p>
              ) : null}
              {testResult ? (
                <p className={`mt-2 text-xs ${testResult.success ? 'text-emerald-600' : 'text-rose-600'}`}>
                  {testResult.message}
                  {testResult.missing.length > 0 ? ` missing=${testResult.missing.join(',')}` : ''}
                </p>
              ) : null}
            </article>
          );
        })}
      </div>

      <div className="mt-8 rounded-lg border border-slate-200 p-4">
        <div className="flex items-center justify-between gap-3">
          <h3 className="text-lg font-semibold">Repository Mappings</h3>
          <div className="flex gap-2">
            <button
              type="button"
              className="rounded-md border border-slate-300 px-3 py-2 text-xs text-slate-700 hover:bg-slate-50"
              onClick={() => setRepoRows((rows) => [...rows, { repository: '', enabled: true, allowedPrefixes: '' }])}
            >
              Add Row
            </button>
            <button
              type="button"
              className="rounded-md bg-slate-900 px-3 py-2 text-xs font-medium text-white hover:bg-slate-700"
              onClick={saveRepositories}
            >
              Save
            </button>
          </div>
        </div>
        <p className="mt-1 text-xs text-slate-600">Allowed prefixes is comma-separated. Example: JWV2,GOV.</p>
        <div className="mt-3 space-y-2">
          {repoRows.map((row, index) => (
            <div key={`${row.repository}-${index}`} className="grid gap-2 md:grid-cols-12">
              <input
                className="rounded-md border border-slate-300 px-3 py-2 text-sm md:col-span-5"
                value={row.repository}
                onChange={(event) =>
                  setRepoRows((rows) =>
                    rows.map((existing, rowIndex) =>
                      rowIndex === index ? { ...existing, repository: event.target.value } : existing,
                    ),
                  )
                }
                placeholder="org/repository"
              />
              <input
                className="rounded-md border border-slate-300 px-3 py-2 text-sm md:col-span-5"
                value={row.allowedPrefixes}
                onChange={(event) =>
                  setRepoRows((rows) =>
                    rows.map((existing, rowIndex) =>
                      rowIndex === index ? { ...existing, allowedPrefixes: event.target.value } : existing,
                    ),
                  )
                }
                placeholder="PREFIX1,PREFIX2"
              />
              <label className="flex items-center gap-2 rounded-md border border-slate-300 px-3 py-2 text-xs text-slate-700 md:col-span-2">
                <input
                  type="checkbox"
                  checked={row.enabled}
                  onChange={(event) =>
                    setRepoRows((rows) =>
                      rows.map((existing, rowIndex) =>
                        rowIndex === index ? { ...existing, enabled: event.target.checked } : existing,
                      ),
                    )
                  }
                />
                Enabled
              </label>
            </div>
          ))}
          {repoRows.length === 0 ? <p className="text-xs text-slate-500">No repository mappings configured.</p> : null}
        </div>
        {repoMessage ? <p className="mt-2 text-xs text-slate-600">{repoMessage}</p> : null}
      </div>

      <div className="mt-8 rounded-lg border border-slate-200 p-4">
        <div className="flex items-center justify-between gap-3">
          <h3 className="text-lg font-semibold">Board Mappings</h3>
          <div className="flex gap-2">
            <button
              type="button"
              className="rounded-md border border-slate-300 px-3 py-2 text-xs text-slate-700 hover:bg-slate-50"
              onClick={() => setBoardRows((rows) => [...rows, { prefix: '', boardId: '', boardName: '' }])}
            >
              Add Row
            </button>
            <button
              type="button"
              className="rounded-md bg-slate-900 px-3 py-2 text-xs font-medium text-white hover:bg-slate-700"
              onClick={saveBoards}
            >
              Save
            </button>
          </div>
        </div>
        <div className="mt-3 space-y-2">
          {boardRows.map((row, index) => (
            <div key={`${row.prefix}-${index}`} className="grid gap-2 md:grid-cols-12">
              <input
                className="rounded-md border border-slate-300 px-3 py-2 text-sm md:col-span-3"
                value={row.prefix}
                onChange={(event) =>
                  setBoardRows((rows) =>
                    rows.map((existing, rowIndex) =>
                      rowIndex === index ? { ...existing, prefix: event.target.value.toUpperCase() } : existing,
                    ),
                  )
                }
                placeholder="PREFIX"
              />
              <input
                className="rounded-md border border-slate-300 px-3 py-2 text-sm md:col-span-4"
                value={row.boardId}
                onChange={(event) =>
                  setBoardRows((rows) =>
                    rows.map((existing, rowIndex) =>
                      rowIndex === index ? { ...existing, boardId: event.target.value } : existing,
                    ),
                  )
                }
                placeholder="Board ID"
              />
              <input
                className="rounded-md border border-slate-300 px-3 py-2 text-sm md:col-span-5"
                value={row.boardName}
                onChange={(event) =>
                  setBoardRows((rows) =>
                    rows.map((existing, rowIndex) =>
                      rowIndex === index ? { ...existing, boardName: event.target.value } : existing,
                    ),
                  )
                }
                placeholder="Board Name"
              />
            </div>
          ))}
          {boardRows.length === 0 ? <p className="text-xs text-slate-500">No board mappings configured.</p> : null}
        </div>
        {boardMessage ? <p className="mt-2 text-xs text-slate-600">{boardMessage}</p> : null}
      </div>

      <div className="mt-8 rounded-lg border border-slate-200 p-4">
        <h3 className="text-lg font-semibold">Sync Controls</h3>
        <p className="mt-1 text-xs text-slate-600">Run full sync or reconciliation after changing mappings or integration tokens.</p>
        <div className="mt-3 flex items-center gap-2">
          <button
            type="button"
            className="rounded-md border border-slate-300 px-3 py-2 text-xs text-slate-700 hover:bg-slate-50"
            onClick={() => runSync('full')}
          >
            Run Full Sync
          </button>
          <button
            type="button"
            className="rounded-md border border-slate-300 px-3 py-2 text-xs text-slate-700 hover:bg-slate-50"
            onClick={() => runSync('reconcile')}
          >
            Run Reconcile
          </button>
          <button
            type="button"
            className="rounded-md border border-slate-300 px-3 py-2 text-xs text-slate-700 hover:bg-slate-50"
            onClick={() => loadData()}
          >
            Reload
          </button>
        </div>
        {syncMessage ? <p className="mt-2 text-xs text-slate-600">{syncMessage}</p> : null}
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

function AccessGovernancePage() {
  const [users, setUsers] = useState<AccessUser[]>([]);
  const [audit, setAudit] = useState<AccessAuditRow[]>([]);
  const [message, setMessage] = useState<string>('');
  const [actor, setActor] = useState<string>('admin-ui');

  const load = () => {
    Promise.all([fetch('/api/kpi/admin/access/users'), fetch('/api/kpi/admin/access/audit')])
      .then(async ([usersRes, auditRes]) => {
        if (!usersRes.ok || !auditRes.ok) {
          throw new Error('Failed to load access data');
        }
        setUsers((await usersRes.json()) as AccessUser[]);
        setAudit((await auditRes.json()) as AccessAuditRow[]);
      })
      .catch((error) => {
        setMessage(error instanceof Error ? error.message : 'Failed to load');
      });
  };

  useEffect(() => {
    load();
  }, []);

  const deactivate = (userId: string) => {
    fetch(`/api/kpi/admin/access/users/${userId}/deactivate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ performedBy: actor }),
    })
      .then(async (response) => {
        if (!response.ok) {
          const text = await response.text();
          throw new Error(`Deactivate failed: ${response.status} ${text}`);
        }
        setMessage(`Deactivated ${userId}`);
        load();
      })
      .catch((error) => {
        setMessage(error instanceof Error ? error.message : 'Deactivate failed');
      });
  };

  return (
    <section className="mx-auto max-w-6xl rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-2xl font-semibold">Access Governance</h2>
      <p className="mt-2 text-sm text-slate-600">Centralized user deprovisioning with audit trail and safety guards.</p>
      <input
        className="mt-3 rounded-md border border-slate-300 px-3 py-2 text-sm"
        value={actor}
        onChange={(event) => setActor(event.target.value)}
        placeholder="Performed by user id"
      />
      {message ? <p className="mt-3 text-sm text-slate-600">{message}</p> : null}

      <h3 className="mt-6 text-lg font-semibold">Active Users</h3>
      <div className="mt-3 space-y-2">
        {users.map((user) => (
          <article key={user.id} className="flex items-center justify-between rounded-lg border border-slate-200 p-3">
            <div>
              <p className="text-sm font-semibold text-slate-900">{user.name}</p>
              <p className="text-xs text-slate-600">{user.email}</p>
            </div>
            <button
              type="button"
              className="rounded-md border border-rose-300 px-3 py-1 text-xs font-medium text-rose-700 hover:bg-rose-50"
              onClick={() => deactivate(user.id)}
            >
              Deactivate
            </button>
          </article>
        ))}
      </div>

      <h3 className="mt-6 text-lg font-semibold">Audit Log</h3>
      <div className="mt-3 space-y-2">
        {audit.map((row) => (
          <article key={row.id} className="rounded-lg border border-slate-200 p-3">
            <p className="text-xs text-slate-800">
              {row.system} {row.action} {row.userId} status={row.status}
            </p>
            <p className="text-xs text-slate-500">
              by {row.performedBy} at {row.timestamp}
            </p>
          </article>
        ))}
      </div>
    </section>
  );
}

function SystemHealthPage() {
  const [summary, setSummary] = useState<HealthSummary | null>(null);
  const [eventId, setEventId] = useState<string>('');
  const [message, setMessage] = useState<string>('');

  const load = () => {
    fetch('/api/kpi/system/health-summary')
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`Failed to load health summary: ${response.status}`);
        }
        return response.json() as Promise<HealthSummary>;
      })
      .then((data) => {
        setSummary(data);
        setMessage('');
      })
      .catch((error) => {
        setMessage(error instanceof Error ? error.message : 'Failed to load summary');
      });
  };

  useEffect(() => {
    load();
  }, []);

  const replay = () => {
    fetch(`/api/kpi/admin/replay/${eventId}`, { method: 'POST' })
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`Replay failed: ${response.status}`);
        }
        const payload = await response.json();
        setMessage(`Requeued event ${payload.eventId}`);
      })
      .catch((error) => {
        setMessage(error instanceof Error ? error.message : 'Replay failed');
      });
  };

  return (
    <section className="mx-auto max-w-6xl rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
      <h2 className="text-2xl font-semibold">System Health</h2>
      <p className="mt-2 text-sm text-slate-600">Queue backlog, webhook processing states and replay tooling.</p>
      <button
        type="button"
        className="mt-3 rounded-md border border-slate-300 px-3 py-1 text-xs text-slate-700 hover:bg-slate-50"
        onClick={load}
      >
        Refresh
      </button>
      {summary ? (
        <div className="mt-4 grid gap-3 md:grid-cols-3">
          <article className="rounded-lg border border-slate-200 p-3">
            <p className="text-xs text-slate-500">Queue Backlog</p>
            <p className={`text-xl font-semibold ${summary.queueBacklog > 1000 ? 'text-rose-600' : 'text-emerald-600'}`}>
              {summary.queueBacklog}
            </p>
          </article>
          <article className="rounded-lg border border-slate-200 p-3">
            <p className="text-xs text-slate-500">Webhook Processed</p>
            <p className="text-xl font-semibold text-slate-900">{summary.webhookProcessed}</p>
          </article>
          <article className="rounded-lg border border-slate-200 p-3">
            <p className="text-xs text-slate-500">Webhook Failed</p>
            <p className={`text-xl font-semibold ${summary.webhookFailed > 0 ? 'text-rose-600' : 'text-emerald-600'}`}>
              {summary.webhookFailed}
            </p>
          </article>
        </div>
      ) : null}

      <div className="mt-6 rounded-lg border border-slate-200 p-4">
        <p className="text-sm font-semibold text-slate-900">Replay Webhook Event</p>
        <div className="mt-2 flex items-center gap-2">
          <input
            className="rounded-md border border-slate-300 px-3 py-2 text-sm"
            value={eventId}
            onChange={(event) => setEventId(event.target.value)}
            placeholder="event id"
          />
          <button
            type="button"
            className="rounded-md bg-slate-900 px-4 py-2 text-sm text-white hover:bg-slate-700"
            onClick={replay}
          >
            Replay
          </button>
        </div>
      </div>
      {message ? <p className="mt-3 text-sm text-slate-600">{message}</p> : null}
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
      {path.startsWith('/admin/integrations') ? <IntegrationsPage /> : null}
      {path.startsWith('/time') ? <TimeAccountabilityPage /> : null}
      {path.startsWith('/admin/access') ? <AccessGovernancePage /> : null}
      {path.startsWith('/health') ? <SystemHealthPage /> : null}
      {!path.startsWith('/governance-rules')
        && !path.startsWith('/admin/policies')
        && !path.startsWith('/admin/integrations')
        && !path.startsWith('/time')
        && !path.startsWith('/admin/access')
        && !path.startsWith('/health') ? (
        <DashboardPage />
      ) : null}
    </main>
  );
}

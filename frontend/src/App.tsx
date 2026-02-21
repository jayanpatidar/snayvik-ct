import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

const snapshotData = [
  { day: 'Mon', risk: 22, drift: 11 },
  { day: 'Tue', risk: 35, drift: 12 },
  { day: 'Wed', risk: 47, drift: 20 },
  { day: 'Thu', risk: 41, drift: 16 },
  { day: 'Fri', risk: 29, drift: 10 },
];

export function App() {
  return (
    <main className="min-h-screen bg-slate-50 p-6 text-slate-900">
      <section className="mx-auto max-w-6xl rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <h1 className="text-2xl font-semibold">Snayvik KPI Governance Dashboard</h1>
        <p className="mt-2 text-sm text-slate-600">
          Executive overview placeholder for lead time, risk, drift, and policy violations.
        </p>

        <div className="mt-6 h-80 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={snapshotData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
              <XAxis dataKey="day" stroke="#475569" />
              <YAxis stroke="#475569" />
              <Tooltip />
              <Bar dataKey="risk" fill="#f59e0b" radius={[6, 6, 0, 0]} />
              <Bar dataKey="drift" fill="#0ea5e9" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </section>
    </main>
  );
}

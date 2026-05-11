"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";

type User = { id: number; username: string };
type ClubRow = { id: number; name: string; updatedAt: string; _count: { players: number } };

export default function Home() {
  const [user, setUser] = useState<User | null | undefined>(undefined);
  const [clubs, setClubs] = useState<ClubRow[]>([]);
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    const me = await fetch("/api/me", { credentials: "include" }).then((r) => r.json());
    setUser(me.user);
    if (!me.user) {
      setClubs([]);
      return;
    }
    const c = await fetch("/api/clubs", { credentials: "include" }).then((r) => r.json());
    if (c.clubs) setClubs(c.clubs);
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  async function signIn(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    const res = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ username, password }),
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      setError(typeof data.error === "string" ? data.error : "Request failed");
      return;
    }
    setPassword("");
    await refresh();
  }

  async function logout() {
    await fetch("/api/auth/logout", { method: "POST", credentials: "include" });
    await refresh();
  }

  if (user === undefined) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-100 text-zinc-600">
        Loading…
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-zinc-100 px-4 py-12 text-zinc-900">
      <div className="mx-auto max-w-lg space-y-8">
        <header>
          <h1 className="text-2xl font-semibold tracking-tight">TeamBalancer</h1>
          <p className="mt-1 text-sm text-zinc-600">
            Web is <strong>view-only</strong>. Create clubs, players, fixtures, and scores in the Android app;
            when that data syncs to the server, you can browse it here.
          </p>
        </header>

        {!user ? (
          <section className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
            <h2 className="text-sm font-semibold text-zinc-800">Sign in to view</h2>
            <p className="mt-1 text-xs text-zinc-500">
              Use the same account the app will use once API login is wired. Registration stays in the app for now.
            </p>
            <form onSubmit={signIn} className="mt-4 space-y-3">
              <div>
                <label className="block text-xs font-medium text-zinc-500">Username</label>
                <input
                  className="mt-1 w-full rounded-lg border border-zinc-200 px-3 py-2 text-sm"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  autoComplete="username"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-zinc-500">Password</label>
                <input
                  type="password"
                  className="mt-1 w-full rounded-lg border border-zinc-200 px-3 py-2 text-sm"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  autoComplete="current-password"
                />
              </div>
              {error && <p className="text-sm text-red-600">{error}</p>}
              <button
                type="submit"
                className="w-full rounded-lg bg-emerald-600 py-2 text-sm font-medium text-white hover:bg-emerald-700"
              >
                Sign in
              </button>
            </form>
          </section>
        ) : (
          <section className="space-y-4">
            <div className="flex items-center justify-between rounded-xl border border-zinc-200 bg-white px-4 py-3 shadow-sm">
              <span className="text-sm">
                Viewing as <strong>{user.username}</strong>
              </span>
              <button type="button" className="text-sm text-zinc-600 underline" onClick={() => void logout()}>
                Sign out
              </button>
            </div>

            <div className="rounded-xl border border-zinc-200 bg-white p-6 shadow-sm">
              <h2 className="text-sm font-semibold text-zinc-800">Clubs</h2>
              <p className="mt-1 text-xs text-zinc-500">Open a club to see players and match history (read-only).</p>
              <ul className="mt-4 divide-y divide-zinc-100">
                {clubs.length === 0 ? (
                  <li className="py-3 text-sm text-zinc-500">No clubs yet—add them in the app.</li>
                ) : (
                  clubs.map((c) => (
                    <li key={c.id} className="flex items-center justify-between py-3 text-sm">
                      <Link href={`/clubs/${c.id}`} className="font-medium text-emerald-800 underline-offset-2 hover:underline">
                        {c.name}
                      </Link>
                      <span className="text-zinc-500">{c._count.players} players</span>
                    </li>
                  ))
                )}
              </ul>
            </div>
          </section>
        )}
      </div>
    </div>
  );
}

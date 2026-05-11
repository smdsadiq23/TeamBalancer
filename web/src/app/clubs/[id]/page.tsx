"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";

type Player = {
  id: number;
  name: string;
  style: string;
  category: string;
  battingRating: number;
  bowlingRating: number;
  fieldingRating: number;
  isCaptain: boolean;
  isAvailable: boolean;
};

type ClubDetail = {
  id: number;
  name: string;
  teamNames: unknown;
  history: unknown;
  players: Player[];
};

/** Gson/Room mirrors Android `Club.TeamHistory` / `Team` / `Match` shape (loosely typed JSON). */
type HistoryEntry = {
  date?: string;
  sport?: string;
  teams?: TeamEntry[];
  matches?: MatchEntry[];
};

type TeamEntry = {
  name?: string;
  players?: { name?: string }[];
  totalStrength?: number;
};

type MatchEntry = {
  id?: string;
  team1?: string;
  team2?: string;
  sport?: string;
  score1?: number;
  score2?: number;
  wickets1?: number;
  wickets2?: number;
  overs1?: number;
  overs2?: number;
  isFixtureSchedule?: boolean;
  hasStarted?: boolean;
  isCompleted?: boolean;
  venue?: string;
  matchType?: string;
  tossWinner?: string;
  tossDecision?: string;
  scheduledStartMillis?: number;
};

const CATEGORY_POWER: Record<string, number> = {
  REGULAR: 10,
  PROMISING_TALENT: 8,
  VETERAN: 7,
};

function playerPowerRating(p: Player): number {
  const base = CATEGORY_POWER[p.category] ?? 6;
  return base + p.battingRating + p.bowlingRating + p.fieldingRating;
}

function formatScheduled(ms: unknown): string {
  if (typeof ms !== "number" || ms <= 0) return "";
  try {
    return new Date(ms).toLocaleString(undefined, {
      dateStyle: "medium",
      timeStyle: "short",
    });
  } catch {
    return "";
  }
}

function formatCricketTotals(m: MatchEntry): string {
  const runs1 = typeof m.score1 === "number" ? m.score1 : 0;
  const runs2 = typeof m.score2 === "number" ? m.score2 : 0;
  const wk1 = typeof m.wickets1 === "number" ? m.wickets1 : 0;
  const wk2 = typeof m.wickets2 === "number" ? m.wickets2 : 0;
  const o1 = typeof m.overs1 === "number" ? m.overs1 : 0;
  const o2 = typeof m.overs2 === "number" ? m.overs2 : 0;
  const t1 = m.team1 ?? "Team A";
  const t2 = m.team2 ?? "Team B";
  const cricketish =
    `${m.sport ?? ""}${m.matchType ?? ""}`.toLowerCase().includes("cricket") ||
    o1 > 0 ||
    o2 > 0;
  if (cricketish) {
    const l1 = `${runs1}/${wk1}${o1 > 0 ? ` (${Number(o1).toFixed(1)} ov)` : ""}`;
    const l2 = `${runs2}/${wk2}${o2 > 0 ? ` (${Number(o2).toFixed(1)} ov)` : ""}`;
    return `${t1} ${l1} · ${t2} ${l2}`;
  }
  return `${t1} ${runs1} – ${t2} ${runs2}`;
}

function matchStatusLabel(m: MatchEntry): string {
  if (m.isCompleted) return "Completed";
  if (m.hasStarted) return "Live / in progress";
  if (m.isFixtureSchedule !== false && !m.hasStarted) return "Fixture";
  return "Scheduled";
}

function parseHistory(raw: unknown): HistoryEntry[] {
  if (!Array.isArray(raw)) return [];
  return raw as HistoryEntry[];
}

export default function ClubViewerPage() {
  const params = useParams();
  const idRaw = params.id;
  const id = typeof idRaw === "string" ? idRaw : Array.isArray(idRaw) ? idRaw[0] : "";

  const [club, setClub] = useState<ClubDetail | null | undefined>(undefined);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!id) return;
    setError(null);
    const res = await fetch(`/api/clubs/${id}`, { credentials: "include" });
    if (res.status === 401) {
      setClub(null);
      setError("Sign in first (home).");
      return;
    }
    if (!res.ok) {
      setClub(null);
      setError(res.status === 404 ? "Club not found." : "Could not load club.");
      return;
    }
    const data = await res.json();
    setClub(data.club ?? null);
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  if (club === undefined) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-zinc-100 text-zinc-600">
        Loading…
      </div>
    );
  }

  if (error || !club) {
    return (
      <div className="min-h-screen bg-zinc-100 px-4 py-12">
        <div className="mx-auto max-w-4xl">
          <Link href="/" className="text-sm text-emerald-700 underline">
            ← Home
          </Link>
          <p className="mt-6 text-sm text-red-600">{error ?? "Unavailable."}</p>
        </div>
      </div>
    );
  }

  const teamNames = Array.isArray(club.teamNames) ? (club.teamNames as string[]) : [];
  const history = parseHistory(club.history);
  const playersByPower = [...club.players].sort((a, b) => playerPowerRating(b) - playerPowerRating(a));

  return (
    <div className="min-h-screen bg-zinc-100 px-4 py-10 text-zinc-900">
      <div className="mx-auto max-w-5xl space-y-6">
        <div>
          <Link href="/" className="text-sm text-emerald-700 underline">
            ← Clubs
          </Link>
          <h1 className="mt-2 text-2xl font-semibold tracking-tight">{club.name}</h1>
          <p className="mt-1 text-sm text-zinc-600">
            Read-only view: squad ratings, balance sessions, and cricket results sync from the app.
          </p>
        </div>

        {teamNames.length > 0 && (
          <section className="rounded-xl border border-zinc-200 bg-white p-5 shadow-sm">
            <h2 className="text-sm font-semibold text-zinc-800">Saved team names</h2>
            <ul className="mt-2 flex flex-wrap gap-2 text-sm text-zinc-700">
              {teamNames.map((t) => (
                <li
                  key={t}
                  className="rounded-full border border-zinc-200 bg-zinc-50 px-3 py-1 text-zinc-800"
                >
                  {t}
                </li>
              ))}
            </ul>
          </section>
        )}

        <section className="rounded-xl border border-zinc-200 bg-white p-5 shadow-sm">
          <h2 className="text-sm font-semibold text-zinc-800">Players &amp; skill profile</h2>
          <p className="mt-1 text-xs text-zinc-500">
            Power ≈ category base (REGULAR 10 / PROMISING 8 / VETERAN 7) + bat + bowl + field — same idea as
            the balance screen.
          </p>
          {club.players.length === 0 ? (
            <p className="mt-2 text-sm text-zinc-500">No players yet.</p>
          ) : (
            <div className="mt-3 overflow-x-auto">
              <table className="w-full border-collapse text-left text-sm">
                <thead>
                  <tr className="border-b border-zinc-200 text-xs uppercase tracking-wide text-zinc-500">
                    <th className="py-2 pr-3 font-medium">Player</th>
                    <th className="py-2 pr-3 font-medium">Style</th>
                    <th className="py-2 pr-3 font-medium">Category</th>
                    <th className="py-2 pr-2 font-medium text-right">Bat</th>
                    <th className="py-2 pr-2 font-medium text-right">Bowl</th>
                    <th className="py-2 pr-2 font-medium text-right">Fld</th>
                    <th className="py-2 pr-2 font-medium text-right">Power</th>
                    <th className="py-2 font-medium">Avail</th>
                  </tr>
                </thead>
                <tbody>
                  {playersByPower.map((p) => (
                      <tr key={p.id} className="border-b border-zinc-100">
                        <td className="py-2 pr-3 font-medium">
                          {p.name}
                          {p.isCaptain ? (
                            <span className="ml-1 rounded bg-amber-100 px-1.5 text-xs text-amber-900">
                              C
                            </span>
                          ) : null}
                        </td>
                        <td className="py-2 pr-3 text-zinc-600">{humanStyle(p.style)}</td>
                        <td className="py-2 pr-3 text-zinc-600">{humanCategory(p.category)}</td>
                        <td className="py-2 pr-2 text-right tabular-nums">{p.battingRating}</td>
                        <td className="py-2 pr-2 text-right tabular-nums">{p.bowlingRating}</td>
                        <td className="py-2 pr-2 text-right tabular-nums">{p.fieldingRating}</td>
                        <td className="py-2 pr-2 text-right tabular-nums font-medium text-emerald-800">
                          {playerPowerRating(p)}
                        </td>
                        <td className="py-2 text-zinc-600">{p.isAvailable ? "Yes" : "No"}</td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <section className="rounded-xl border border-zinc-200 bg-white p-5 shadow-sm">
          <h2 className="text-sm font-semibold text-zinc-800">Balance sessions &amp; match history</h2>
          <p className="mt-1 text-xs text-zinc-500">
            Each block is one saved session from the app (date + sport). Expand to see generated teams and
            every match line.
          </p>
          {history.length === 0 ? (
            <p className="mt-2 text-sm text-zinc-500">No history yet.</p>
          ) : (
            <ul className="mt-4 space-y-3">
              {history.map((h, idx) => (
                <li key={idx} className="rounded-xl border border-zinc-200 bg-zinc-50/80">
                  <details className="group">
                    <summary className="cursor-pointer list-none px-4 py-3 [&::-webkit-details-marker]:hidden">
                      <div className="flex flex-wrap items-baseline justify-between gap-2">
                        <span className="font-semibold text-zinc-900">{h.date ?? "No date"}</span>
                        <span className="text-xs text-zinc-500">
                          {h.sport ?? "Other"} · {Array.isArray(h.teams) ? h.teams.length : 0} team
                          {Array.isArray(h.teams) && h.teams.length === 1 ? "" : "s"} ·{" "}
                          {Array.isArray(h.matches) ? h.matches.length : 0} match
                          {Array.isArray(h.matches) && h.matches.length === 1 ? "" : "es"}
                        </span>
                      </div>
                      <span className="mt-1 block text-xs text-emerald-700 group-open:hidden">
                        Tap to expand teams &amp; matches
                      </span>
                    </summary>
                    <div className="space-y-4 border-t border-zinc-200 px-4 py-3 text-sm">
                      {Array.isArray(h.teams) && h.teams.length > 0 ? (
                        <div>
                          <h3 className="text-xs font-semibold uppercase tracking-wide text-zinc-500">
                            Balanced teams
                          </h3>
                          <div className="mt-2 grid gap-3 sm:grid-cols-2">
                            {h.teams.map((team, ti) => (
                              <div
                                key={ti}
                                className="rounded-lg border border-zinc-200 bg-white p-3 shadow-sm"
                              >
                                <div className="flex items-center justify-between gap-2 border-b border-zinc-100 pb-2">
                                  <span className="font-medium text-zinc-900">
                                    {team.name ?? `Team ${ti + 1}`}
                                  </span>
                                  {typeof team.totalStrength === "number" ? (
                                    <span className="rounded-md bg-emerald-50 px-2 py-0.5 text-xs font-medium text-emerald-900">
                                      Strength {team.totalStrength}
                                    </span>
                                  ) : null}
                                </div>
                                <ul className="mt-2 max-h-48 space-y-1 overflow-y-auto text-zinc-700">
                                  {Array.isArray(team.players) && team.players.length > 0 ? (
                                    team.players.map((pl, pi) => (
                                      <li key={pi} className="text-xs">
                                        {pl.name ?? "—"}
                                      </li>
                                    ))
                                  ) : (
                                    <li className="text-xs text-zinc-400">No player list stored</li>
                                  )}
                                </ul>
                              </div>
                            ))}
                          </div>
                        </div>
                      ) : (
                        <p className="text-xs text-zinc-400">No balanced teams recorded for this session.</p>
                      )}

                      {Array.isArray(h.matches) && h.matches.length > 0 ? (
                        <div>
                          <h3 className="text-xs font-semibold uppercase tracking-wide text-zinc-500">
                            Matches
                          </h3>
                          <ul className="mt-2 space-y-2">
                            {h.matches.map((m, mi) => (
                              <li
                                key={m.id ?? mi}
                                className="rounded-lg border border-zinc-200 bg-white px-3 py-2 text-xs shadow-sm"
                              >
                                <div className="flex flex-wrap items-center justify-between gap-2">
                                  <span className="font-medium text-zinc-900">
                                    {m.team1 ?? "?"} <span className="text-zinc-400">vs</span>{" "}
                                    {m.team2 ?? "?"}
                                  </span>
                                  <span
                                    className={`rounded px-2 py-0.5 text-[10px] font-medium uppercase ${
                                      m.isCompleted
                                        ? "bg-zinc-200 text-zinc-800"
                                        : m.hasStarted
                                          ? "bg-amber-100 text-amber-900"
                                          : "bg-sky-100 text-sky-900"
                                    }`}
                                  >
                                    {matchStatusLabel(m)}
                                  </span>
                                </div>
                                <p className="mt-1 font-mono text-[11px] text-zinc-700">
                                  {formatCricketTotals(m)}
                                </p>
                                <div className="mt-1 flex flex-wrap gap-x-3 gap-y-0.5 text-[10px] text-zinc-500">
                                  {m.matchType ? <span>{m.matchType}</span> : null}
                                  {m.venue ? <span>{m.venue}</span> : null}
                                  {formatScheduled(m.scheduledStartMillis) ? (
                                    <span>{formatScheduled(m.scheduledStartMillis)}</span>
                                  ) : null}
                                  {m.tossWinner ? (
                                    <span>
                                      Toss: {m.tossWinner}
                                      {m.tossDecision ? ` (${m.tossDecision})` : ""}
                                    </span>
                                  ) : null}
                                  {m.sport && m.sport !== h.sport ? <span>{m.sport}</span> : null}
                                </div>
                              </li>
                            ))}
                          </ul>
                        </div>
                      ) : (
                        <p className="text-xs text-zinc-400">No matches in this session yet.</p>
                      )}
                    </div>
                  </details>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  );
}

function humanStyle(s: string): string {
  const map: Record<string, string> = {
    BATSMAN: "Batsman",
    BOWLER: "Bowler",
    ALL_ROUNDER: "All-rounder",
  };
  return map[s] ?? s;
}

function humanCategory(c: string): string {
  const map: Record<string, string> = {
    REGULAR: "Regular",
    PROMISING_TALENT: "Promising talent",
    VETERAN: "Veteran",
  };
  return map[c] ?? c;
}

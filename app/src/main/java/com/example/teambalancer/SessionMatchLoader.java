package com.example.teambalancer;

import java.util.Comparator;
import java.util.List;

/**
 * Normalizes persisted {@link Match} rows for the active session (Gson/Room load):
 * ids, completion, innings rules, fixture flags.
 */
public final class SessionMatchLoader {

    private SessionMatchLoader() {}

    /**
     * Optimally prepares matches for the session in a single pass to avoid ANRs.
     * @return true if any match was modified and should be written back.
     */
    public static boolean prepareMatchesForSession(List<Match> matches) {
        if (matches == null || matches.isEmpty()) {
            return false;
        }
        boolean anyUpdated = false;
        
        // 1. Normalize started flags (requires full list context)
        anyUpdated |= MatchCompletionHelper.normalizeStartedFlags(matches);

        // 2. Perform all other per-match normalizations in one pass
        for (Match m : matches) {
            if (m == null) continue;
            
            boolean mUpdated = false;
            
            // Core restoration
            MatchPersistenceHelper.ensureListsNotNull(m);
            mUpdated |= MatchPersistenceHelper.restoreListsFromJson(m);
            mUpdated |= MatchCompletionHelper.ensureMatchId(m);
            mUpdated |= MatchCompletionHelper.syncCompletionFromTimestamp(m);
            mUpdated |= MatchCompletionHelper.syncCompletionFromRemembered(m);
            
            // Logical consistency
            mUpdated |= MatchCompletionHelper.applyInningsCompletionRules(m);
            mUpdated |= MatchCompletionHelper.restoreDisplayStateFromSnapshot(m);
            mUpdated |= CricketTotalsRecomputer.recomputeFromStoredEvents(m);
            mUpdated |= MatchCompletionHelper.ensureSnapshotForCompletedMatch(m);
            mUpdated |= MatchCompletionHelper.refreshFinalSnapshotFromLive(m);
            
            // UI State
            MatchFixtureHelper.normalizeFixtureScheduleOnLoad(m);
            
            if (mUpdated) {
                anyUpdated = true;
            }
        }
        return anyUpdated;
    }

    /** Scheduled time first; matches without a time sort last; then team name. */
    public static final Comparator<Match> BY_SCHEDULE_THEN_TEAM = (a, b) -> {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;

        long ka = a.scheduledStartMillis <= 0 ? Long.MAX_VALUE : a.scheduledStartMillis;
        long kb = b.scheduledStartMillis <= 0 ? Long.MAX_VALUE : b.scheduledStartMillis;
        if (ka != kb) {
            return Long.compare(ka, kb);
        }
        String n1 = a.team1 != null ? a.team1 : "";
        String n2 = b.team1 != null ? b.team1 : "";
        return n1.compareToIgnoreCase(n2);
    };
}

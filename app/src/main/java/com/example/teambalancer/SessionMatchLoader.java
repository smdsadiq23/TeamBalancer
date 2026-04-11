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
     * @return true if any match was modified and should be written back with {@link DataManager#updateClub(Club)}.
     */
    public static boolean prepareMatchesForSession(List<Match> matches) {
        if (matches == null) {
            return false;
        }
        boolean updated = false;
        for (Match m : matches) {
            MatchPersistenceHelper.ensureListsNotNull(m);
            if (MatchPersistenceHelper.restoreListsFromJson(m)) {
                updated = true;
            }
            if (MatchCompletionHelper.ensureMatchId(m)) {
                updated = true;
            }
            if (MatchCompletionHelper.syncCompletionFromTimestamp(m)) {
                updated = true;
            }
            if (MatchCompletionHelper.syncCompletionFromRemembered(m)) {
                updated = true;
            }
        }
        updated |= MatchCompletionHelper.normalizeStartedFlags(matches);
        for (Match m : matches) {
            if (MatchCompletionHelper.applyInningsCompletionRules(m)) {
                updated = true;
            }
        }
        for (Match m : matches) {
            if (MatchCompletionHelper.ensureSnapshotForCompletedMatch(m)) {
                updated = true;
            }
        }
        for (Match m : matches) {
            if (MatchCompletionHelper.restoreDisplayStateFromSnapshot(m)) {
                updated = true;
            }
        }
        for (Match m : matches) {
            MatchFixtureHelper.normalizeFixtureScheduleOnLoad(m);
        }
        return updated;
    }

    /** Scheduled time first; matches without a time sort last; then team name. */
    public static final Comparator<Match> BY_SCHEDULE_THEN_TEAM = (a, b) -> {
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

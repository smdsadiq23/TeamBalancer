package com.example.teambalancer;

/**
 * Separates scheduled fixtures (not yet started as a match) from active/completed matches.
 * {@link Match#isFixtureSchedule} is true while a row only belongs on the Fixtures screen;
 * it becomes false once scoring/toss begins.
 */
public final class MatchFixtureHelper {

    private MatchFixtureHelper() {}

    /**
     * Call after loading from storage so legacy rows and Gson defaults are consistent.
     */
    public static void normalizeFixtureScheduleOnLoad(Match m) {
        if (m == null) {
            return;
        }
        if (m.isCompleted || m.matchCompletedAt > 0L) {
            m.isFixtureSchedule = false;
            return;
        }
        m.isFixtureSchedule = !hasMatchProgress(m);
    }

    /**
     * True if this game should appear only under Fixtures (scheduled, not yet a live match).
     */
    public static boolean isScheduledFixture(Match m) {
        return m != null && m.isFixtureSchedule;
    }

    /**
     * User began the match (toss, first score, etc.) — stop showing under Fixtures only.
     */
    public static void promoteToMatch(Match m) {
        if (m != null) {
            m.isFixtureSchedule = false;
        }
    }

    private static boolean hasMatchProgress(Match m) {
        if (m.isCompleted || m.matchCompletedAt > 0L) {
            return true;
        }
        if (m.tossWinner != null) {
            return true;
        }
        if (m.battingTeam != null || m.bowlingTeam != null) {
            return true;
        }
        if (m.ballHistory != null && !m.ballHistory.isEmpty()) {
            return true;
        }
        if (m.score1 != 0 || m.score2 != 0 || m.wickets1 != 0 || m.wickets2 != 0) {
            return true;
        }
        if (m.overs1 > 0 || m.overs2 > 0) {
            return true;
        }
        if (!"Cricket".equalsIgnoreCase(m.sport) && m.hasStarted) {
            return true;
        }
        return false;
    }
}

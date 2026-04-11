package com.example.teambalancer;

import java.util.Locale;

/**
 * Reads totals for list UI: prefers frozen snapshot for completed matches when live fields were cleared.
 */
public final class MatchScoreDisplay {

    private MatchScoreDisplay() {}

    public static int runs1(Match m) {
        if (m == null) {
            return 0;
        }
        return m.hasFinalScoreSnapshot ? m.finalRuns1 : m.score1;
    }

    public static int runs2(Match m) {
        if (m == null) {
            return 0;
        }
        return m.hasFinalScoreSnapshot ? m.finalRuns2 : m.score2;
    }

    public static int wickets1(Match m) {
        if (m == null) {
            return 0;
        }
        return m.hasFinalScoreSnapshot ? m.finalWickets1 : m.wickets1;
    }

    public static int wickets2(Match m) {
        if (m == null) {
            return 0;
        }
        return m.hasFinalScoreSnapshot ? m.finalWickets2 : m.wickets2;
    }

    public static double overs1(Match m) {
        if (m == null) {
            return 0;
        }
        return m.hasFinalScoreSnapshot ? m.finalOvers1 : m.overs1;
    }

    public static double overs2(Match m) {
        if (m == null) {
            return 0;
        }
        return m.hasFinalScoreSnapshot ? m.finalOvers2 : m.overs2;
    }

    /** Cricket list row: runs/wickets plus overs. */
    public static String cricketScoreWithOvers(Match m, boolean teamOne) {
        int r = teamOne ? runs1(m) : runs2(m);
        int w = teamOne ? wickets1(m) : wickets2(m);
        double o = teamOne ? overs1(m) : overs2(m);
        return String.format(Locale.getDefault(), "%d/%d · %.1f ov", r, w, o);
    }

    public static String cricketScoreRunsWickets(Match m, boolean teamOne) {
        int r = teamOne ? runs1(m) : runs2(m);
        int w = teamOne ? wickets1(m) : wickets2(m);
        return String.format(Locale.getDefault(), "%d/%d", r, w);
    }
}

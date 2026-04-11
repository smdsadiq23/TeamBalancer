package com.example.teambalancer;

import java.util.List;

/**
 * Single place for cricket match results (list cards, scorecard header, dialogs). Uses totals from
 * {@link MatchScoreDisplay} and infers who batted first from ball one / toss — not {@link Match#battingTeam}
 * alone, which can point at the last innings after completion.
 */
public final class CricketMatchResultFormatter {

    private CricketMatchResultFormatter() {}

    /** Full sentence for winners / tie, e.g. "BLUES won by 8 wickets". */
    public static String formatResult(Match m) {
        if (m == null) {
            return "";
        }
        int s1 = MatchScoreDisplay.runs1(m);
        int s2 = MatchScoreDisplay.runs2(m);
        if (s1 == s2) {
            return "Match tied";
        }
        String first = firstInningsBattingTeam(m);
        boolean team1BattedFirst = m.team1 != null && m.team1.equals(first);

        if (s1 > s2) {
            if (team1BattedFirst) {
                return m.team1 + " won by " + (s1 - s2) + " runs";
            }
            int rem = maxWicketsForTeam(m, m.team1) - MatchScoreDisplay.wickets1(m);
            return m.team1 + " won by " + Math.max(rem, 0) + " wickets";
        }
        if (!team1BattedFirst) {
            return m.team2 + " won by " + (s2 - s1) + " runs";
        }
        int rem = maxWicketsForTeam(m, m.team2) - MatchScoreDisplay.wickets2(m);
        return m.team2 + " won by " + Math.max(rem, 0) + " wickets";
    }

    /**
     * Secondary line on fixture cards when the match is finished (never empty for completed+cricket).
     */
    public static String formatCardSubtitle(Match m) {
        if (m == null || !"Cricket".equalsIgnoreCase(m.sport)) {
            return "";
        }
        if (!MatchCompletionHelper.isEffectivelyCompleted(m)) {
            return "";
        }
        int s1 = MatchScoreDisplay.runs1(m);
        int s2 = MatchScoreDisplay.runs2(m);
        boolean noTotals = s1 == 0 && s2 == 0 && MatchScoreDisplay.overs1(m) <= 0 && MatchScoreDisplay.overs2(m) <= 0;
        boolean noEvents = MatchBallEvents.forStats(m).isEmpty();
        if (noTotals && noEvents) {
            return "Completed — no score data on file";
        }
        return formatResult(m);
    }

    private static String firstInningsBattingTeam(Match m) {
        List<BallEvent> ev = MatchBallEvents.forStats(m);
        if (!ev.isEmpty()) {
            BallEvent first = ev.get(0);
            if (first.striker != null) {
                if (contains(m.squad1, first.striker)) {
                    return m.team1;
                }
                if (contains(m.squad2, first.striker)) {
                    return m.team2;
                }
            }
        }
        if (m.tossWinner != null && m.tossDecision != null) {
            if ("Batting".equalsIgnoreCase(m.tossDecision)) {
                return m.tossWinner;
            }
            if ("Bowling".equalsIgnoreCase(m.tossDecision)) {
                return m.tossWinner.equals(m.team1) ? m.team2 : m.team1;
            }
        }
        return m.team1;
    }

    private static boolean contains(List<String> squad, String name) {
        if (squad == null || name == null) {
            return false;
        }
        for (String p : squad) {
            if (name.equals(p)) {
                return true;
            }
        }
        return false;
    }

    private static int maxWicketsForTeam(Match m, String teamName) {
        if (teamName == null || m == null) {
            return 10;
        }
        List<String> sq = teamName.equals(m.team1) ? m.squad1 : m.squad2;
        if (sq != null && !sq.isEmpty()) {
            return Math.max(1, sq.size() - 1);
        }
        return 10;
    }
}

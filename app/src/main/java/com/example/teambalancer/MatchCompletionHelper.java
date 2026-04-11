package com.example.teambalancer;

import java.util.List;

/**
 * Centralizes chase / innings-end rules so completion state stays consistent
 * after DB load, ball events, and manual finish.
 */
public final class MatchCompletionHelper {

    private MatchCompletionHelper() {}

    public static int getMaxWickets(Match match) {
        if (match.battingTeam == null || match.team1 == null) {
            return 10;
        }
        List<String> squad = match.battingTeam.equals(match.team1) ? match.squad1 : match.squad2;
        if (squad != null && !squad.isEmpty()) {
            return squad.size() - 1;
        }
        return 10;
    }

    /** Keeps {@link Match#hasStarted} aligned with persisted state after Gson load. */
    public static boolean normalizeStartedFlags(List<Match> matches) {
        if (matches == null) {
            return false;
        }
        boolean changed = false;
        for (Match match : matches) {
            boolean shouldBeStarted = match.isCompleted
                    || match.score1 > 0
                    || match.score2 > 0
                    || match.wickets1 > 0
                    || match.wickets2 > 0
                    || match.overs1 > 0
                    || match.overs2 > 0
                    || match.tossWinner != null
                    || match.tossDecision != null
                    || match.battingTeam != null
                    || match.bowlingTeam != null
                    || !match.ballHistory.isEmpty();
            if (match.hasStarted != shouldBeStarted) {
                match.hasStarted = shouldBeStarted;
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Applies the same rules as in-match {@code checkMatchStatus}: second-innings chase
     * target met, all out, or overs exhausted (non-Test formats).
     *
     * @return true if {@link Match#isCompleted} was set to true by this call
     */
    public static boolean applyInningsCompletionRules(Match match) {
        if (match.isCompleted) {
            return false;
        }
        if (match.currentInnings != 2) {
            return false;
        }
        if ("Test".equalsIgnoreCase(match.matchType)) {
            return false;
        }
        if (match.battingTeam == null || match.team1 == null) {
            return false;
        }

        int battingScore = (match.battingTeam.equals(match.team1)) ? match.score1 : match.score2;
        int bowlingScore = (match.battingTeam.equals(match.team1)) ? match.score2 : match.score1;
        int target = bowlingScore + 1;

        int wkts = (match.battingTeam.equals(match.team1)) ? match.wickets1 : match.wickets2;
        double overs = (match.battingTeam.equals(match.team1)) ? match.overs1 : match.overs2;

        if (battingScore >= target || wkts >= getMaxWickets(match) || overs >= match.maxOvers) {
            match.hasStarted = true;
            match.isCompleted = true;
            return true;
        }
        return false;
    }
}

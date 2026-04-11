package com.example.teambalancer;

import java.util.List;

/**
 * Centralizes chase / innings-end rules so completion state stays consistent
 * after DB load, ball events, and manual finish.
 */
public final class MatchCompletionHelper {

    private MatchCompletionHelper() {}

    /** True if the match is finished (flag and/or durable completion timestamp). */
    public static boolean isEffectivelyCompleted(Match match) {
        return match != null && (match.isCompleted || match.matchCompletedAt > 0L);
    }

    /** Mark finished and stamp time once (for persistence). */
    public static void markMatchCompleted(Match match) {
        if (match == null) {
            return;
        }
        match.isCompleted = true;
        if (match.matchCompletedAt <= 0L) {
            match.matchCompletedAt = System.currentTimeMillis();
        }
    }

    /** After Gson load: if timestamp survived but flag did not, restore the flag. */
    public static boolean syncCompletionFromTimestamp(Match match) {
        if (match == null) {
            return false;
        }
        if (match.matchCompletedAt > 0L && !match.isCompleted) {
            match.isCompleted = true;
            return true;
        }
        return false;
    }

    /** True once any ball, run, wicket, or over has been recorded (not just toss/setup). */
    public static boolean hasRecordedPlay(Match match) {
        if (match == null) {
            return false;
        }
        if (match.ballHistory != null && !match.ballHistory.isEmpty()) {
            return true;
        }
        return match.score1 != 0
                || match.score2 != 0
                || match.wickets1 != 0
                || match.wickets2 != 0
                || match.overs1 > 0
                || match.overs2 > 0;
    }

    /**
     * Toss or lineup is set but no delivery yet — avoid showing "LIVE" for 0/0 scorecards.
     */
    public static boolean isPreBallSetup(Match match) {
        if (match == null || isEffectivelyCompleted(match)) {
            return false;
        }
        if (hasRecordedPlay(match)) {
            return false;
        }
        return match.tossWinner != null
                || match.battingTeam != null
                || match.bowlingTeam != null
                || match.hasStarted;
    }

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
                    || match.matchCompletedAt > 0L
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
        if (isEffectivelyCompleted(match)) {
            return false;
        }
        if (match.currentInnings != 2) {
            return false;
        }
        if ("Test".equalsIgnoreCase(match.matchType)) {
            return false;
        }
        if (match.team1 == null) {
            return false;
        }
        boolean hadBatting = match.battingTeam != null;
        recoverBattingAndBowlingTeamsIfNull(match);
        boolean recoveredTeams = !hadBatting && match.battingTeam != null;
        if (match.battingTeam == null) {
            return recoveredTeams;
        }

        int battingScore = (match.battingTeam.equals(match.team1)) ? match.score1 : match.score2;
        int bowlingScore = (match.battingTeam.equals(match.team1)) ? match.score2 : match.score1;
        int target = bowlingScore + 1;

        int wkts = (match.battingTeam.equals(match.team1)) ? match.wickets1 : match.wickets2;
        double overs = (match.battingTeam.equals(match.team1)) ? match.overs1 : match.overs2;

        if (battingScore >= target || wkts >= getMaxWickets(match) || overs >= match.maxOvers) {
            match.hasStarted = true;
            markMatchCompleted(match);
            return true;
        }
        return recoveredTeams;
    }

    /**
     * Gson / edge cases can drop battingTeam while ballHistory still has striker names — recover so chase rules run.
     */
    private static void recoverBattingAndBowlingTeamsIfNull(Match match) {
        if (match.battingTeam != null) {
            return;
        }
        if (match.ballHistory == null || match.ballHistory.isEmpty()) {
            return;
        }
        for (int i = match.ballHistory.size() - 1; i >= 0; i--) {
            BallEvent e = match.ballHistory.get(i);
            String name = e.striker;
            if (name == null) {
                name = e.nonStriker;
            }
            if (name == null) {
                continue;
            }
            if (teamContainsPlayer(match, match.team1, name)) {
                match.battingTeam = match.team1;
                match.bowlingTeam = match.team2;
                return;
            }
            if (teamContainsPlayer(match, match.team2, name)) {
                match.battingTeam = match.team2;
                match.bowlingTeam = match.team1;
                return;
            }
        }
        BallEvent last = match.ballHistory.get(match.ballHistory.size() - 1);
        if (last.bowler != null) {
            if (teamContainsPlayer(match, match.team1, last.bowler)) {
                match.bowlingTeam = match.team1;
                match.battingTeam = match.team2;
                return;
            }
            if (teamContainsPlayer(match, match.team2, last.bowler)) {
                match.bowlingTeam = match.team2;
                match.battingTeam = match.team1;
                return;
            }
        }
    }

    private static boolean teamContainsPlayer(Match match, String teamName, String playerName) {
        if (playerName == null || teamName == null) {
            return false;
        }
        List<String> squad = teamName.equals(match.team1) ? match.squad1 : match.squad2;
        if (squad != null) {
            for (String n : squad) {
                if (playerName.equals(n)) {
                    return true;
                }
            }
        }
        return false;
    }
}

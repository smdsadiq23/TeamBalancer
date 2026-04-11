package com.example.teambalancer;

import java.util.Locale;

/**
 * Shared ball-by-ball scoring logic for {@link MatchesFragment}, {@link FixturesFragment}, and
 * {@link CricketScoringActivity}.
 */
public final class CricketScoringHelper {

    private CricketScoringHelper() {}

    public static void checkMatchStatus(Match match) {
        MatchCompletionHelper.applyInningsCompletionRules(match);
    }

    public static void processBallAndUpdateRotation(Match match, BallEvent event) {
        if (MatchCompletionHelper.isEffectivelyCompleted(match)) {
            return;
        }

        processBall(match, event);

        int runsForRotation = 0;
        if (event.extraType == BallEvent.ExtraType.NONE) {
            runsForRotation = event.runs;
        } else if (event.extraType == BallEvent.ExtraType.BYE || event.extraType == BallEvent.ExtraType.LEG_BYE) {
            runsForRotation = event.runs;
        }

        if (runsForRotation % 2 != 0) {
            String temp = match.striker;
            match.striker = match.nonStriker;
            match.nonStriker = temp;
        }

        double overs = (match.battingTeam.equals(match.team1)) ? match.overs1 : match.overs2;
        int balls = (int) Math.round((overs - (int) overs) * 10);

        if (event.isLegalBall && balls == 0) {
            String temp = match.striker;
            match.striker = match.nonStriker;
            match.nonStriker = temp;
            match.currentBowler = null;
        }
    }

    public static void processBall(Match match, BallEvent event) {
        if (MatchCompletionHelper.isEffectivelyCompleted(match) || match.battingTeam == null) {
            return;
        }

        if (match.ballHistory == null) {
            match.ballHistory = new java.util.ArrayList<>();
        }
        match.ballHistory.add(event);
        boolean isTeam1 = match.battingTeam.equals(match.team1);

        if (event.extraType == BallEvent.ExtraType.NO_BALL) {
            match.isFreeHit = true;
        }

        if (match.isFreeHit && event.wicketType != BallEvent.WicketType.RUN_OUT) {
            event.wicketType = BallEvent.WicketType.NONE;
        }

        if (isTeam1) {
            match.score1 += event.runs;
            if (event.wicketType != BallEvent.WicketType.NONE) {
                match.wickets1++;
            }
            if (event.isLegalBall) {
                match.overs1 = addBall(match.overs1);
            }
        } else {
            match.score2 += event.runs;
            if (event.wicketType != BallEvent.WicketType.NONE) {
                match.wickets2++;
            }
            if (event.isLegalBall) {
                match.overs2 = addBall(match.overs2);
            }
        }

        if (match.isFreeHit && event.extraType != BallEvent.ExtraType.NO_BALL) {
            match.isFreeHit = false;
        }
        MatchPersistenceHelper.syncJsonFromLists(match);
    }

    public static void undoBall(Match match, BallEvent event) {
        boolean isTeam1 = match.battingTeam.equals(match.team1);
        if (isTeam1) {
            match.score1 -= event.runs;
            if (event.wicketType != BallEvent.WicketType.NONE) {
                match.wickets1--;
            }
            if (event.isLegalBall) {
                match.overs1 = removeBall(match.overs1);
            }
        } else {
            match.score2 -= event.runs;
            if (event.wicketType != BallEvent.WicketType.NONE) {
                match.wickets2--;
            }
            if (event.isLegalBall) {
                match.overs2 = removeBall(match.overs2);
            }
        }
        if (!match.ballHistory.isEmpty()) {
            BallEvent last = match.ballHistory.get(match.ballHistory.size() - 1);
            match.striker = last.striker;
            match.nonStriker = last.nonStriker;
            match.currentBowler = last.bowler;
        }
    }

    public static double addBall(double overs) {
        int whole = (int) overs;
        int balls = (int) Math.round((overs - whole) * 10);
        balls++;
        if (balls >= 6) {
            whole++;
            balls = 0;
        }
        return whole + (balls / 10.0);
    }

    public static double removeBall(double overs) {
        int whole = (int) overs;
        int balls = (int) Math.round((overs - whole) * 10);
        balls--;
        if (balls < 0) {
            whole--;
            balls = 5;
        }
        if (whole < 0) {
            return 0.0;
        }
        return whole + (balls / 10.0);
    }

    public static String getPlayerBattingStats(Match match, String playerName) {
        int runs = 0;
        int balls = 0;
        for (BallEvent event : MatchBallEvents.forStats(match)) {
            if (playerName.equals(event.striker)) {
                if (event.extraType != BallEvent.ExtraType.WIDE) {
                    balls++;
                    if (event.extraType == BallEvent.ExtraType.NONE) {
                        runs += event.runs;
                    }
                }
            }
        }
        return runs + " (" + balls + ")";
    }

    public static String getPlayerBowlingStats(Match match, String playerName) {
        int runsConceded = 0;
        int wickets = 0;
        int balls = 0;
        for (BallEvent event : MatchBallEvents.forStats(match)) {
            if (playerName.equals(event.bowler)) {
                if (event.isLegalBall) {
                    balls++;
                }
                if (event.extraType == BallEvent.ExtraType.WIDE || event.extraType == BallEvent.ExtraType.NO_BALL) {
                    runsConceded += event.runs;
                } else if (event.extraType == BallEvent.ExtraType.NONE) {
                    runsConceded += event.runs;
                }
                if (event.wicketType != BallEvent.WicketType.NONE && event.wicketType != BallEvent.WicketType.RUN_OUT) {
                    wickets++;
                }
            }
        }
        int overs = balls / 6;
        int remainingBalls = balls % 6;
        String line = String.format(Locale.getDefault(), "%d.%d - %d - %d", overs, remainingBalls, runsConceded, wickets);
        if (match.maxOversPerBowler > 0) {
            int capBalls = BowlingQuotaHelper.maxLegalBallsPerBowler(match);
            int bowled = BowlingQuotaHelper.legalBallsBowledInCurrentInnings(match, playerName);
            if (capBalls > 0 && bowled >= 0) {
                line += String.format(Locale.getDefault(), " · %d/%d", bowled, capBalls);
            }
        }
        return line;
    }
}

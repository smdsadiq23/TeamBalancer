package com.example.teambalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds a text scoreboard from {@link Match} totals and ball history (live or snapshot).
 */
public final class CricketScoreboardFormatter {

    private CricketScoreboardFormatter() {}

    public static String formatFull(Match match) {
        if (match == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Innings summary\n");
        sb.append(match.team1 != null ? match.team1 : "?")
                .append(": ")
                .append(MatchScoreDisplay.cricketScoreWithOvers(match, true))
                .append('\n');
        sb.append(match.team2 != null ? match.team2 : "?")
                .append(": ")
                .append(MatchScoreDisplay.cricketScoreWithOvers(match, false))
                .append('\n');
        if (match.tossWinner != null) {
            sb.append("\nToss: ").append(match.tossWinner);
            if (match.tossDecision != null) {
                sb.append(" — ").append(match.tossDecision);
            }
            sb.append('\n');
        }
        List<BallEvent> balls = eventsForDisplay(match);
        if (balls.isEmpty()) {
            sb.append("\nNo ball-by-ball data stored for this match.");
            return sb.toString();
        }
        sb.append("\nBall-by-ball (").append(balls.size()).append(")\n");
        int n = 1;
        for (BallEvent e : balls) {
            sb.append(n++).append(". ").append(formatBall(e)).append('\n');
        }
        return sb.toString();
    }

    private static List<BallEvent> eventsForDisplay(Match match) {
        return new ArrayList<>(MatchBallEvents.forStats(match));
    }

    private static String formatBall(BallEvent e) {
        if (e == null) {
            return "";
        }
        StringBuilder s = new StringBuilder();
        s.append("O").append(e.overNumber).append('.').append(e.ballNumber);
        if (e.striker != null) {
            s.append(" ").append(e.striker);
        }
        if (e.extraType != null && e.extraType != BallEvent.ExtraType.NONE) {
            s.append(" ").append(e.extraType.name().replace('_', ' '));
        }
        if (e.wicketType != null && e.wicketType != BallEvent.WicketType.NONE) {
            s.append(" WICKET ").append(e.wicketType.name().replace('_', ' '));
        } else {
            s.append(" +").append(e.runs).append(" run").append(e.runs == 1 ? "" : "s");
        }
        if (e.bowler != null) {
            s.append(" · ").append(e.bowler);
        }
        return s.toString();
    }
}

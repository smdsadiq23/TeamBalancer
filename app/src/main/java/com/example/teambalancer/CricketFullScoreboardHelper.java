package com.example.teambalancer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Full scoreboard strings (batting lines, bowling lines, extras summary, ball timeline) from
 * {@link Match#ballHistory} / {@link MatchBallEvents#forStats(Match)}.
 */
public final class CricketFullScoreboardHelper {

    private CricketFullScoreboardHelper() {}

    public static String formatBatting(Match match) {
        List<BallEvent> events = MatchBallEvents.forStats(match);
        Map<String, int[]> map = new LinkedHashMap<>();
        for (BallEvent e : events) {
            if (e.striker == null) {
                continue;
            }
            map.putIfAbsent(e.striker, new int[] {0, 0, 0, 0});
            int[] s = map.get(e.striker);

            if (e.extraType == BallEvent.ExtraType.NONE) {
                s[0] += e.runs;
                s[1]++;
                if (e.runs == 4) {
                    s[2]++;
                }
                if (e.runs == 6) {
                    s[3]++;
                }
            }
            if (e.extraType == BallEvent.ExtraType.BYE || e.extraType == BallEvent.ExtraType.LEG_BYE) {
                s[1]++;
            }
        }

        if (map.isEmpty()) {
            return "—";
        }

        StringBuilder text = new StringBuilder();
        for (String player : map.keySet()) {
            int[] s = map.get(player);
            double sr = s[1] == 0 ? 0 : (s[0] * 100.0) / s[1];
            text.append(player)
                    .append("  ")
                    .append(s[0])
                    .append("(")
                    .append(s[1])
                    .append(")  ")
                    .append("4s:")
                    .append(s[2])
                    .append(" ")
                    .append("6s:")
                    .append(s[3])
                    .append(" ")
                    .append("SR:")
                    .append(String.format(Locale.getDefault(), "%.1f", sr))
                    .append("\n");
        }
        return text.toString().trim();
    }

    public static String formatBowling(Match match) {
        List<BallEvent> events = MatchBallEvents.forStats(match);
        Map<String, int[]> map = new LinkedHashMap<>();
        for (BallEvent e : events) {
            if (e.bowler == null) {
                continue;
            }
            map.putIfAbsent(e.bowler, new int[] {0, 0, 0});
            int[] s = map.get(e.bowler);
            if (e.isLegalBall) {
                s[0]++;
            }
            s[1] += e.runs;
            if (e.wicketType != BallEvent.WicketType.NONE
                    && e.wicketType != BallEvent.WicketType.RUN_OUT) {
                s[2]++;
            }
        }

        if (map.isEmpty()) {
            return "—";
        }

        StringBuilder text = new StringBuilder();
        for (String player : map.keySet()) {
            int[] s = map.get(player);
            int overs = s[0] / 6;
            int balls = s[0] % 6;
            double econ = s[0] == 0 ? 0 : (s[1] * 6.0) / s[0];
            text.append(player)
                    .append("  ")
                    .append(overs)
                    .append(".")
                    .append(balls)
                    .append("  ")
                    .append(s[1])
                    .append("/")
                    .append(s[2])
                    .append("  Econ:")
                    .append(String.format(Locale.getDefault(), "%.1f", econ))
                    .append("\n");
        }
        return text.toString().trim();
    }

    public static String formatExtras(Match match) {
        List<BallEvent> events = MatchBallEvents.forStats(match);
        int wide = 0;
        int nb = 0;
        int bye = 0;
        int lb = 0;
        for (BallEvent e : events) {
            switch (e.extraType) {
                case WIDE:
                    wide++;
                    break;
                case NO_BALL:
                    nb++;
                    break;
                case BYE:
                    bye += e.runs;
                    break;
                case LEG_BYE:
                    lb += e.runs;
                    break;
                default:
                    break;
            }
        }
        return "Wd:" + wide + " Nb:" + nb + " B:" + bye + " Lb:" + lb;
    }

    public static String formatTimeline(Match match) {
        List<BallEvent> events = MatchBallEvents.forStats(match);
        if (events.isEmpty()) {
            return "—";
        }
        StringBuilder timeline = new StringBuilder();
        for (BallEvent e : events) {
            if (e.wicketType != BallEvent.WicketType.NONE) {
                timeline.append("W ");
            } else if (e.extraType == BallEvent.ExtraType.WIDE) {
                timeline.append("Wd ");
            } else if (e.extraType == BallEvent.ExtraType.NO_BALL) {
                timeline.append("Nb ");
            } else {
                timeline.append(e.runs).append(" ");
            }
        }
        return timeline.toString().trim();
    }

    /** Both squads with team names; bullets per player. */
    public static String formatSquads(Match match) {
        if (match == null) {
            return "—";
        }
        String t1 = match.team1 != null ? match.team1 : "Team 1";
        String t2 = match.team2 != null ? match.team2 : "Team 2";
        StringBuilder b = new StringBuilder();
        b.append(t1).append("\n");
        if (match.squad1 != null && !match.squad1.isEmpty()) {
            for (String p : match.squad1) {
                b.append("• ").append(p).append("\n");
            }
        } else {
            b.append("—\n");
        }
        b.append("\n").append(t2).append("\n");
        if (match.squad2 != null && !match.squad2.isEmpty()) {
            for (String p : match.squad2) {
                b.append("• ").append(p).append("\n");
            }
        } else {
            b.append("—\n");
        }
        return b.toString().trim();
    }
}

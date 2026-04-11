package com.example.teambalancer;

/**
 * Squad lists for the full scoreboard footer in {@link CricketScoringActivity}.
 */
public final class CricketFullScoreboardHelper {

    private CricketFullScoreboardHelper() {}

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

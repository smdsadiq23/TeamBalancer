package com.example.teambalancer;

import java.util.List;

/**
 * Replays {@link BallEvent} history into score/wicket/over fields. Used after Gson/Room load when
 * lists were restored from {@link Match#ballHistoryJson} but primitive totals stayed at zero.
 */
public final class CricketTotalsRecomputer {

    private CricketTotalsRecomputer() {}

    /**
     * @return true if match totals were recalculated (caller may persist).
     */
    public static boolean recomputeFromStoredEvents(Match match) {
        if (match == null || !"Cricket".equalsIgnoreCase(match.sport)) {
            return false;
        }
        List<BallEvent> events = MatchBallEvents.forStats(match);
        if (events.isEmpty()) {
            return false;
        }

        int split = match.inningsTwoFirstBallIndex;
        if (split > events.size()) {
            split = events.size();
        }

        match.score1 = 0;
        match.score2 = 0;
        match.wickets1 = 0;
        match.wickets2 = 0;
        match.overs1 = 0.0;
        match.overs2 = 0.0;

        String batting = battingTeamForFirstBall(match, events.get(0));
        if (batting == null) {
            batting = match.team1;
        }
        boolean freeHit = false;

        for (int i = 0; i < events.size(); i++) {
            if (split >= 0 && i == split) {
                batting = otherTeam(match, batting);
                freeHit = false;
            }
            BallEvent raw = events.get(i);
            BallEvent e = BallEvent.copyOf(raw);

            if (e.extraType == BallEvent.ExtraType.NO_BALL) {
                freeHit = true;
            }
            if (freeHit && e.wicketType != BallEvent.WicketType.RUN_OUT) {
                e.wicketType = BallEvent.WicketType.NONE;
            }

            boolean isTeam1 = batting.equals(match.team1);
            if (isTeam1) {
                match.score1 += e.runs;
                if (e.wicketType != BallEvent.WicketType.NONE) {
                    match.wickets1++;
                }
                if (e.isLegalBall) {
                    match.overs1 = addBall(match.overs1);
                }
            } else {
                match.score2 += e.runs;
                if (e.wicketType != BallEvent.WicketType.NONE) {
                    match.wickets2++;
                }
                if (e.isLegalBall) {
                    match.overs2 = addBall(match.overs2);
                }
            }

            if (freeHit && e.extraType != BallEvent.ExtraType.NO_BALL) {
                freeHit = false;
            }
        }

        match.battingTeam = batting;
        return true;
    }

    private static String battingTeamForFirstBall(Match m, BallEvent first) {
        if (first != null && first.striker != null) {
            if (containsPlayer(m.squad1, first.striker)) {
                return m.team1;
            }
            if (containsPlayer(m.squad2, first.striker)) {
                return m.team2;
            }
        }
        if (m.battingTeam != null) {
            return m.battingTeam;
        }
        return m.team1;
    }

    private static boolean containsPlayer(List<String> squad, String name) {
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

    private static String otherTeam(Match m, String batting) {
        if (m.team1 != null && batting != null && batting.equals(m.team1)) {
            return m.team2;
        }
        return m.team1;
    }

    private static double addBall(double overs) {
        int whole = (int) overs;
        int balls = (int) Math.round((overs - whole) * 10);
        balls++;
        if (balls >= 6) {
            whole++;
            balls = 0;
        }
        return whole + (balls / 10.0);
    }
}

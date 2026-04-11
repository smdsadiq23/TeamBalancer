package com.example.teambalancer;

import java.util.Collections;
import java.util.List;

/**
 * Read-only access to ball events for stats when live {@link Match#ballHistory} was cleared but snapshot exists.
 */
public final class MatchBallEvents {

    private MatchBallEvents() {}

    public static List<BallEvent> forStats(Match match) {
        if (match == null) {
            return Collections.emptyList();
        }
        if (match.ballHistory != null && !match.ballHistory.isEmpty()) {
            return match.ballHistory;
        }
        if (match.scoreboardSnapshot != null && !match.scoreboardSnapshot.isEmpty()) {
            return match.scoreboardSnapshot;
        }
        return match.ballHistory != null ? match.ballHistory : Collections.emptyList();
    }

    /** Safe iteration count for null checks. */
    public static int count(Match match) {
        return forStats(match).size();
    }
}

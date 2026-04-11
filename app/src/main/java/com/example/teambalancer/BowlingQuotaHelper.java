package com.example.teambalancer;

/**
 * Tracks legal balls bowled by each bowler in the current innings so
 * {@link Match#maxOversPerBowler} can be enforced (e.g. box / club rules).
 */
public final class BowlingQuotaHelper {

    private BowlingQuotaHelper() {}

    /**
     * Legal balls bowled by this bowler in the current innings only.
     *
     * @return -1 if the innings split is unknown (legacy saves), so callers should not enforce quota
     */
    public static int legalBallsBowledInCurrentInnings(Match match, String bowlerName) {
        if (bowlerName == null || match.ballHistory == null) {
            return 0;
        }
        int start;
        if (match.currentInnings <= 1) {
            start = 0;
        } else {
            if (match.inningsTwoFirstBallIndex < 0) {
                return -1;
            }
            start = match.inningsTwoFirstBallIndex;
        }
        int end = match.ballHistory.size();
        int count = 0;
        for (int i = start; i < end; i++) {
            BallEvent e = match.ballHistory.get(i);
            if (!bowlerName.equals(e.bowler)) {
                continue;
            }
            if (e.isLegalBall) {
                count++;
            }
        }
        return count;
    }

    /**
     * Maximum legal balls allowed per bowler this innings, or 0 for no limit.
     */
    public static int maxLegalBallsPerBowler(Match match) {
        int maxOv = match.maxOversPerBowler;
        if (maxOv <= 0) {
            return 0;
        }
        return maxOv * 6;
    }

    /**
     * True if this bowler may not bowl another legal delivery this innings (quota unknown returns false).
     */
    public static boolean isAtOrOverQuota(Match match, String bowlerName) {
        int cap = maxLegalBallsPerBowler(match);
        if (cap <= 0) {
            return false;
        }
        int balls = legalBallsBowledInCurrentInnings(match, bowlerName);
        if (balls < 0) {
            return false;
        }
        return balls >= cap;
    }

    /**
     * True if adding one legal ball for this bowler would exceed the innings quota.
     */
    public static boolean wouldExceedQuotaAfterLegalBall(Match match, String bowlerName) {
        int cap = maxLegalBallsPerBowler(match);
        if (cap <= 0) {
            return false;
        }
        int balls = legalBallsBowledInCurrentInnings(match, bowlerName);
        if (balls < 0) {
            return false;
        }
        return balls + 1 > cap;
    }
}

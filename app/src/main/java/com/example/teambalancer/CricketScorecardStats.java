package com.example.teambalancer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds batting, bowling, extras, and timeline labels from {@link Match} ball history
 * (same rules as in-match player stats).
 */
public final class CricketScorecardStats {

    private CricketScorecardStats() {}

    public static List<BattingStats> buildBatting(Match match) {
        List<BallEvent> events = MatchBallEvents.forStats(match);
        Map<String, BattingStats> byName = new HashMap<>();
        for (BallEvent e : events) {
            if (e == null || e.striker == null) {
                continue;
            }
            BattingStats row = byName.computeIfAbsent(e.striker, k -> {
                BattingStats b = new BattingStats();
                b.name = k;
                return b;
            });
            if (e.extraType != BallEvent.ExtraType.WIDE) {
                row.balls++;
                if (e.extraType == BallEvent.ExtraType.NONE) {
                    row.runs += e.runs;
                    if (e.runs == 4) {
                        row.fours++;
                    } else if (e.runs == 6) {
                        row.sixes++;
                    }
                } else if (e.extraType == BallEvent.ExtraType.BYE || e.extraType == BallEvent.ExtraType.LEG_BYE) {
                    row.runs += e.runs;
                }
            }
        }
        return new ArrayList<>(byName.values());
    }

    public static List<BowlingStats> buildBowling(Match match) {
        List<BallEvent> events = MatchBallEvents.forStats(match);
        Map<String, BowlingStats> byName = new HashMap<>();
        for (BallEvent e : events) {
            if (e == null || e.bowler == null) {
                continue;
            }
            BowlingStats row = byName.computeIfAbsent(e.bowler, k -> {
                BowlingStats b = new BowlingStats();
                b.name = k;
                return b;
            });
            if (e.isLegalBall) {
                row.legalBalls++;
            }
            if (e.extraType == BallEvent.ExtraType.WIDE || e.extraType == BallEvent.ExtraType.NO_BALL) {
                row.runsConceded += e.runs;
            } else if (e.extraType == BallEvent.ExtraType.NONE) {
                row.runsConceded += e.runs;
            } else if (e.extraType == BallEvent.ExtraType.BYE || e.extraType == BallEvent.ExtraType.LEG_BYE) {
                row.runsConceded += e.runs;
            }
            if (e.wicketType != BallEvent.WicketType.NONE && e.wicketType != BallEvent.WicketType.RUN_OUT) {
                row.wickets++;
            }
        }
        return new ArrayList<>(byName.values());
    }

    public static String buildExtrasLine(Match match) {
        List<BallEvent> events = MatchBallEvents.forStats(match);
        int w = 0, nb = 0, b = 0, lb = 0;
        for (BallEvent e : events) {
            if (e == null || e.extraType == null || e.extraType == BallEvent.ExtraType.NONE) {
                continue;
            }
            switch (e.extraType) {
                case WIDE:
                    w += e.runs;
                    break;
                case NO_BALL:
                    nb += e.runs;
                    break;
                case BYE:
                    b += e.runs;
                    break;
                case LEG_BYE:
                    lb += e.runs;
                    break;
                default:
                    break;
            }
        }
        if (w + nb + b + lb == 0) {
            return "None";
        }
        return String.format(Locale.getDefault(), "Wd %d · Nb %d · B %d · Lb %d", w, nb, b, lb);
    }

    /** Short labels for each delivery: "4", "W", "Wd", etc. */
    public static List<String> buildTimelineLabels(Match match) {
        List<BallEvent> events = MatchBallEvents.forStats(match);
        List<String> out = new ArrayList<>(events.size());
        for (BallEvent e : events) {
            out.add(labelForBall(e));
        }
        return out;
    }

    private static String labelForBall(BallEvent e) {
        if (e == null) {
            return "?";
        }
        if (e.wicketType != BallEvent.WicketType.NONE) {
            return "W";
        }
        if (e.extraType == BallEvent.ExtraType.WIDE) {
            return e.runs > 1 ? e.runs + "Wd" : "Wd";
        }
        if (e.extraType == BallEvent.ExtraType.NO_BALL) {
            return e.runs > 1 ? e.runs + "Nb" : "Nb";
        }
        if (e.extraType == BallEvent.ExtraType.BYE) {
            return "B" + e.runs;
        }
        if (e.extraType == BallEvent.ExtraType.LEG_BYE) {
            return "Lb" + e.runs;
        }
        return String.valueOf(e.runs);
    }
}

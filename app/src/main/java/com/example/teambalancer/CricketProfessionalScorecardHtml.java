package com.example.teambalancer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Builds a professional cricket scorecard (batting table, extras, total, DNB, bowling) as HTML for
 * display in a {@link android.webkit.WebView}, matching common broadcast-style layouts.
 */
public final class CricketProfessionalScorecardHtml {

    private CricketProfessionalScorecardHtml() {}

    public static String build(Match match) {
        if (match == null) {
            return emptyDoc("No match");
        }
        List<BallEvent> all = new ArrayList<>(MatchBallEvents.forStats(match));
        if (all.isEmpty()) {
            return emptyDoc("No ball-by-ball data yet.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        sb.append("<style>");
        sb.append(css());
        sb.append("</style></head><body>");

        if (MatchCompletionHelper.isEffectivelyCompleted(match)) {
            sb.append("<div class=\"result\">")
                    .append(esc(CricketMatchResultFormatter.formatResult(match)))
                    .append("</div>");
        }

        int split = match.inningsTwoFirstBallIndex;
        List<BallEvent> inn1 =
                split > 0 ? new ArrayList<>(all.subList(0, split)) : new ArrayList<>(all);
        List<BallEvent> inn2 =
                split > 0 && split < all.size()
                        ? new ArrayList<>(all.subList(split, all.size()))
                        : Collections.emptyList();

        appendInningsBlock(sb, match, inn1);
        if (!inn2.isEmpty()) {
            appendInningsBlock(sb, match, inn2);
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private static String emptyDoc(String message) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>"
                + css()
                + "</style></head><body><div class=\"empty\">"
                + esc(message)
                + "</div></body></html>";
    }

    private static String css() {
        return "body{font-family:system-ui,-apple-system,sans-serif;background:#12121f;color:#e8e8f0;"
                + "margin:0;padding:10px 8px 24px;font-size:13px;line-height:1.35;}"
                + ".result{color:#64b5f6;font-weight:600;margin-bottom:12px;padding:4px 0;}"
                + ".innings{margin-bottom:22px;}"
                + ".inn-header{background:#1b5e20;color:#fff;padding:10px 12px;display:flex;"
                + "justify-content:space-between;align-items:center;font-weight:600;border-radius:4px 4px 0 0;}"
                + ".inn-header span:last-child{font-weight:700;}"
                + "table{width:100%;border-collapse:collapse;margin:0;}"
                + "th{background:#2d2d44;color:#c5c5dc;font-size:11px;font-weight:600;padding:8px 4px;"
                + "text-align:left;border-bottom:1px solid #444;}"
                + "th.num{text-align:right;}"
                + "td{border-bottom:1px solid #2a2a3d;padding:8px 4px;vertical-align:top;}"
                + "td.num{text-align:right;font-variant-numeric:tabular-nums;}"
                + "td.num b{font-weight:700;}"
                + ".name{color:#64b5f6;font-weight:500;}"
                + ".dismiss{color:#9e9e9e;font-size:11px;display:block;margin-top:2px;}"
                + ".chev{color:#666;font-size:12px;padding-left:4px;}"
                + ".summary{padding:10px 4px;border-bottom:1px solid #2a2a3d;font-size:12px;}"
                + ".summary b{font-size:14px;color:#fff;}"
                + ".dnb{padding:10px 4px;font-size:12px;border-bottom:1px solid #2a2a3d;}"
                + ".dnb .names{color:#64b5f6;}"
                + ".bowl-title{color:#f48fb1;font-weight:600;margin:12px 0 6px 4px;font-size:13px;}"
                + ".empty{color:#9e9e9e;padding:16px;text-align:center;}";
    }

    private static void appendInningsBlock(StringBuilder sb, Match match, List<BallEvent> slice) {
        if (slice.isEmpty()) {
            return;
        }
        String batTeam = inferBattingTeam(match, slice);
        String bowlTeam =
                batTeam != null && batTeam.equals(match.team1) ? match.team2 : match.team1;

        int totalRuns = 0;
        int totalWkts = 0;
        for (BallEvent e : slice) {
            totalRuns += e.runs;
            if (e.wicketType != BallEvent.WicketType.NONE) {
                totalWkts++;
            }
        }
        int legalBalls = countLegalBalls(slice);
        double oversDec = legalBalls / 6.0;
        String oversStr = formatOversDisplay(oversDec);
        double rr = oversDec > 0 ? totalRuns / oversDec : 0;

        sb.append("<div class=\"innings\">");
        sb.append("<div class=\"inn-header\"><span>")
                .append(esc(batTeam))
                .append("</span><span>")
                .append(totalRuns)
                .append("-")
                .append(totalWkts)
                .append(" (")
                .append(oversStr)
                .append(" Ov)</span></div>");

        sb.append("<table><thead><tr>");
        sb.append("<th>Batter</th>");
        sb.append("<th class=\"num\">R</th><th class=\"num\">B</th><th class=\"num\">4s</th>");
        sb.append("<th class=\"num\">6s</th><th class=\"num\">SR</th><th class=\"num\"></th>");
        sb.append("</tr></thead><tbody>");

        List<String> order = battingOrder(slice);
        Map<String, BattingAgg> batMap = aggregateBatting(slice);
        Set<String> faced = strikersWhoFaced(slice);

        for (String player : order) {
            BattingAgg a = batMap.get(player);
            if (a == null) {
                continue;
            }
            String dis = dismissalLine(slice, player);
            double sr = a.balls == 0 ? 0 : (a.runs * 100.0) / a.balls;
            sb.append("<tr><td><span class=\"name\">")
                    .append(esc(player))
                    .append("</span><span class=\"dismiss\">")
                    .append(esc(dis))
                    .append("</span></td>");
            sb.append("<td class=\"num\"><b>").append(a.runs).append("</b></td>");
            sb.append("<td class=\"num\">").append(a.balls).append("</td>");
            sb.append("<td class=\"num\">").append(a.fours).append("</td>");
            sb.append("<td class=\"num\">").append(a.sixes).append("</td>");
            sb.append("<td class=\"num\">")
                    .append(String.format(Locale.getDefault(), "%.2f", sr))
                    .append("</td>");
            sb.append("<td class=\"num chev\">›</td></tr>");
        }
        sb.append("</tbody></table>");

        ExtrasBreakdown ex = extrasBreakdown(slice);
        sb.append("<div class=\"summary\">Extras <b>")
                .append(ex.total)
                .append("</b> <span class=\"dismiss\">(b ")
                .append(ex.byes)
                .append(", lb ")
                .append(ex.legByes)
                .append(", w ")
                .append(ex.wides)
                .append(", nb ")
                .append(ex.noBalls)
                .append(", p ")
                .append(ex.penalties)
                .append(")</span></div>");

        sb.append("<div class=\"summary\">Total <b>")
                .append(totalRuns)
                .append("-")
                .append(totalWkts)
                .append("</b> <span class=\"dismiss\">(")
                .append(String.format(Locale.getDefault(), "%.1f", oversDec))
                .append(" Overs, RR: ")
                .append(String.format(Locale.getDefault(), "%.2f", rr))
                .append(")</span></div>");

        List<String> squad = squadForTeam(match, batTeam);
        if (squad != null && !squad.isEmpty()) {
            List<String> dnb = new ArrayList<>();
            for (String p : squad) {
                if (!faced.contains(p)) {
                    dnb.add(p);
                }
            }
            if (!dnb.isEmpty()) {
                sb.append("<div class=\"dnb\">Did not Bat: <span class=\"names\">");
                for (int i = 0; i < dnb.size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(esc(dnb.get(i)));
                }
                sb.append("</span></div>");
            }
        }

        sb.append("<div class=\"bowl-title\">Bowling — ")
                .append(esc(bowlTeam != null ? bowlTeam : "Fielding"))
                .append("</div>");
        sb.append("<table><thead><tr>");
        sb.append("<th>Bowler</th>");
        sb.append("<th class=\"num\">O</th><th class=\"num\">M</th><th class=\"num\">R</th>");
        sb.append("<th class=\"num\">W</th><th class=\"num\">NB</th><th class=\"num\">WD</th><th class=\"num\">ECO</th><th class=\"num\"></th>");
        sb.append("</tr></thead><tbody>");

        Map<String, BowlingAgg> bowlMap = aggregateBowling(slice);
        for (String bowler : bowlMap.keySet()) {
            BowlingAgg b = bowlMap.get(bowler);
            double oDec = b.legalBalls / 6.0;
            double econ = b.legalBalls == 0 ? 0 : (b.runs * 6.0) / b.legalBalls;
            String oStr =
                    String.format(
                            Locale.getDefault(),
                            "%d.%d",
                            b.legalBalls / 6,
                            b.legalBalls % 6);
            sb.append("<tr><td><span class=\"name\">")
                    .append(esc(bowler))
                    .append("</span></td>");
            sb.append("<td class=\"num\">").append(oStr).append("</td>");
            sb.append("<td class=\"num\">").append(b.maidens).append("</td>");
            sb.append("<td class=\"num\">").append(b.runs).append("</td>");
            sb.append("<td class=\"num\"><b>").append(b.wickets).append("</b></td>");
            sb.append("<td class=\"num\">").append(b.noBalls).append("</td>");
            sb.append("<td class=\"num\">").append(b.wides).append("</td>");
            sb.append("<td class=\"num\">")
                    .append(String.format(Locale.getDefault(), "%.2f", econ))
                    .append("</td>");
            sb.append("<td class=\"num chev\">›</td></tr>");
        }
        sb.append("</tbody></table>");
        sb.append("</div>");
    }

    private static String formatOversDisplay(double oversDec) {
        int whole = (int) oversDec;
        int balls = (int) Math.round((oversDec - whole) * 6);
        if (balls >= 6) {
            whole++;
            balls = 0;
        }
        return whole + "." + balls;
    }

    private static int countLegalBalls(List<BallEvent> slice) {
        int n = 0;
        for (BallEvent e : slice) {
            if (e.isLegalBall) {
                n++;
            }
        }
        return n;
    }

    private static String inferBattingTeam(Match m, List<BallEvent> slice) {
        if (slice.isEmpty()) {
            return m.battingTeam != null ? m.battingTeam : m.team1;
        }
        String st = slice.get(0).striker;
        if (st != null && m.squad1 != null) {
            for (String p : m.squad1) {
                if (p.equals(st)) {
                    return m.team1;
                }
            }
        }
        if (st != null && m.squad2 != null) {
            for (String p : m.squad2) {
                if (p.equals(st)) {
                    return m.team2;
                }
            }
        }
        return m.battingTeam != null ? m.battingTeam : m.team1;
    }

    private static List<String> squadForTeam(Match m, String teamName) {
        if (teamName != null && teamName.equals(m.team1) && m.squad1 != null) {
            return m.squad1;
        }
        if (teamName != null && teamName.equals(m.team2) && m.squad2 != null) {
            return m.squad2;
        }
        return new ArrayList<>();
    }

    private static List<String> battingOrder(List<BallEvent> slice) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (BallEvent e : slice) {
            if (e.striker != null) {
                seen.add(e.striker);
            }
        }
        return new ArrayList<>(seen);
    }

    /** Any striker on a delivery (including wides) is not "did not bat". */
    private static Set<String> strikersWhoFaced(List<BallEvent> slice) {
        Set<String> set = new LinkedHashSet<>();
        for (BallEvent e : slice) {
            if (e.striker != null) {
                set.add(e.striker);
            }
        }
        return set;
    }

    private static String dismissalLine(List<BallEvent> slice, String player) {
        for (int i = slice.size() - 1; i >= 0; i--) {
            BallEvent e = slice.get(i);
            if (e.wicketType == BallEvent.WicketType.NONE) {
                continue;
            }
            if (player.equals(e.striker)) {
                return formatDismissal(e);
            }
        }
        return "not out";
    }

    private static String formatDismissal(BallEvent e) {
        String bow = e.bowler != null ? e.bowler : "?";
        switch (e.wicketType) {
            case BOWLED:
                return "b " + bow;
            case CAUGHT:
                return "c b " + bow;
            case LBW:
                return "lbw b " + bow;
            case RUN_OUT:
                return "run out";
            case STUMPED:
                return "st b " + bow;
            case HIT_WICKET:
                return "hit wicket b " + bow;
            default:
                return e.wicketType.name().toLowerCase(Locale.US).replace('_', ' ') + " b " + bow;
        }
    }

    private static Map<String, BattingAgg> aggregateBatting(List<BallEvent> slice) {
        Map<String, BattingAgg> map = new LinkedHashMap<>();
        for (BallEvent e : slice) {
            if (e.striker == null) {
                continue;
            }
            map.putIfAbsent(e.striker, new BattingAgg());
            BattingAgg a = map.get(e.striker);
            if (e.extraType == BallEvent.ExtraType.NONE) {
                a.runs += e.runs;
                a.balls++;
                if (e.runs == 4) {
                    a.fours++;
                }
                if (e.runs == 6) {
                    a.sixes++;
                }
            } else if (e.extraType == BallEvent.ExtraType.BYE
                    || e.extraType == BallEvent.ExtraType.LEG_BYE) {
                a.balls++;
            }
        }
        return map;
    }

    private static ExtrasBreakdown extrasBreakdown(List<BallEvent> slice) {
        ExtrasBreakdown x = new ExtrasBreakdown();
        for (BallEvent e : slice) {
            switch (e.extraType) {
                case WIDE:
                    x.wides++;
                    x.total += e.runs;
                    break;
                case NO_BALL:
                    x.noBalls++;
                    x.total += e.runs;
                    break;
                case BYE:
                    x.byes += e.runs;
                    x.total += e.runs;
                    break;
                case LEG_BYE:
                    x.legByes += e.runs;
                    x.total += e.runs;
                    break;
                default:
                    break;
            }
        }
        return x;
    }

    private static Map<String, BowlingAgg> aggregateBowling(List<BallEvent> slice) {
        Map<String, BowlingAgg> map = new LinkedHashMap<>();
        for (BallEvent e : slice) {
            if (e.bowler == null) {
                continue;
            }
            map.putIfAbsent(e.bowler, new BowlingAgg());
            BowlingAgg b = map.get(e.bowler);
            if (e.isLegalBall) {
                b.legalBalls++;
            }
            b.runs += e.runs;
            if (e.wicketType != BallEvent.WicketType.NONE
                    && e.wicketType != BallEvent.WicketType.RUN_OUT) {
                b.wickets++;
            }
            if (e.extraType == BallEvent.ExtraType.NO_BALL) {
                b.noBalls++;
            }
            if (e.extraType == BallEvent.ExtraType.WIDE) {
                b.wides++;
            }
        }
        for (String bowler : map.keySet()) {
            map.get(bowler).maidens = maidenOversForBowler(slice, bowler);
        }
        return map;
    }

    private static int maidenOversForBowler(List<BallEvent> slice, String bowler) {
        List<Integer> legalRuns = new ArrayList<>();
        for (BallEvent e : slice) {
            if (!e.isLegalBall || !bowler.equals(e.bowler)) {
                continue;
            }
            legalRuns.add(e.runs);
        }
        int maidens = 0;
        for (int i = 0; i + 6 <= legalRuns.size(); i += 6) {
            int sum = 0;
            for (int j = 0; j < 6; j++) {
                sum += legalRuns.get(i + j);
            }
            if (sum == 0) {
                maidens++;
            }
        }
        return maidens;
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static final class BattingAgg {
        int runs;
        int balls;
        int fours;
        int sixes;
    }

    private static final class BowlingAgg {
        int legalBalls;
        int runs;
        int wickets;
        int maidens;
        int noBalls;
        int wides;
    }

    private static final class ExtrasBreakdown {
        int total;
        int byes;
        int legByes;
        int wides;
        int noBalls;
        final int penalties = 0;
    }
}

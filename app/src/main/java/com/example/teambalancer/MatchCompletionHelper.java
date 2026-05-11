package com.example.teambalancer;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Centralizes chase / innings-end rules so completion state stays consistent
 * after DB load, ball events, and manual finish.
 */
public final class MatchCompletionHelper {

    private static final String PREFS = "match_completion_backup";
    private static final String KEY_PREFIX = "done_";

    private static Context appContext;

    private MatchCompletionHelper() {}

    /** Call from {@link MainActivity} so completion can be backed up by match id. */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    /** Gson may omit {@link Match#id} on old rows; required for completion backup. */
    public static boolean ensureMatchId(Match match) {
        if (match == null || (match.id != null && !match.id.isEmpty())) {
            return false;
        }
        match.id = UUID.randomUUID().toString();
        return true;
    }

    /** True if the match is finished (flag, timestamp, and/or prefs backup by id). */
    public static boolean isEffectivelyCompleted(Match match) {
        return match != null
                && (match.isCompleted
                        || match.matchCompletedAt > 0L
                        || isRememberedCompleted(match));
    }

    /** Mark finished and stamp time once (for persistence). */
    public static void markMatchCompleted(Match match) {
        if (match == null) {
            return;
        }
        snapshotFinalScoreboard(match);
        match.isCompleted = true;
        if (match.matchCompletedAt <= 0L) {
            match.matchCompletedAt = System.currentTimeMillis();
        }
        rememberCompleted(match);
    }

    /**
     * Captures runs/wickets/overs and a copy of {@link Match#ballHistory} once, so completed cards and scoreboard
     * still show real numbers after Gson/Room drops live fields.
     */
    private static void snapshotFinalScoreboard(Match match) {
        if (match.hasFinalScoreSnapshot) {
            return;
        }
        match.hasFinalScoreSnapshot = true;
        match.finalRuns1 = match.score1;
        match.finalRuns2 = match.score2;
        match.finalWickets1 = match.wickets1;
        match.finalWickets2 = match.wickets2;
        match.finalOvers1 = match.overs1;
        match.finalOvers2 = match.overs2;
        match.scoreboardSnapshot = new ArrayList<>();
        if (match.ballHistory != null) {
            for (BallEvent e : match.ballHistory) {
                match.scoreboardSnapshot.add(BallEvent.copyOf(e));
            }
        }
        MatchPersistenceHelper.syncJsonFromLists(match);
    }

    /**
     * For matches marked completed before snapshots existed: if live data exists, freeze it once.
     *
     * @return true if snapshot was written.
     */
    public static boolean ensureSnapshotForCompletedMatch(Match match) {
        if (match == null || !isEffectivelyCompleted(match) || match.hasFinalScoreSnapshot) {
            return false;
        }
        boolean hasData = hasRecordedPlay(match)
                || match.score1 != 0
                || match.score2 != 0
                || match.wickets1 != 0
                || match.wickets2 != 0
                || match.overs1 > 0
                || match.overs2 > 0;
        if (!hasData) {
            return false;
        }
        snapshotFinalScoreboard(match);
        return true;
    }

    /**
     * After totals are repaired from ball history, keep {@link Match#hasFinalScoreSnapshot} fields aligned
     * for {@link MatchScoreDisplay} and JSON mirrors.
     */
    public static boolean refreshFinalSnapshotFromLive(Match match) {
        if (match == null || !match.hasFinalScoreSnapshot || !isEffectivelyCompleted(match)) {
            return false;
        }
        boolean changed = false;
        if (match.finalRuns1 != match.score1) { match.finalRuns1 = match.score1; changed = true; }
        if (match.finalRuns2 != match.score2) { match.finalRuns2 = match.score2; changed = true; }
        if (match.finalWickets1 != match.wickets1) { match.finalWickets1 = match.wickets1; changed = true; }
        if (match.finalWickets2 != match.wickets2) { match.finalWickets2 = match.wickets2; changed = true; }
        if (Double.compare(match.finalOvers1, match.overs1) != 0) { match.finalOvers1 = match.overs1; changed = true; }
        if (Double.compare(match.finalOvers2, match.overs2) != 0) { match.finalOvers2 = match.overs2; changed = true; }

        List<BallEvent> src = match.ballHistory != null && !match.ballHistory.isEmpty()
                ? match.ballHistory
                : match.scoreboardSnapshot;

        if (match.scoreboardSnapshot == null || src == null || match.scoreboardSnapshot.size() != src.size()) {
            match.scoreboardSnapshot = new ArrayList<>();
            if (src != null) {
                for (BallEvent e : src) {
                    match.scoreboardSnapshot.add(BallEvent.copyOf(e));
                }
            }
            changed = true;
        }

        if (changed) {
            MatchPersistenceHelper.syncJsonFromLists(match);
        }
        return changed;
    }

    public static boolean restoreDisplayStateFromSnapshot(Match match) {
        if (match == null || !isEffectivelyCompleted(match) || !match.hasFinalScoreSnapshot) {
            return false;
        }
        boolean changed = false;
        if (match.score1 != match.finalRuns1
                || match.score2 != match.finalRuns2
                || match.wickets1 != match.finalWickets1
                || match.wickets2 != match.finalWickets2
                || Double.compare(match.overs1, match.finalOvers1) != 0
                || Double.compare(match.overs2, match.finalOvers2) != 0) {
            match.score1 = match.finalRuns1;
            match.score2 = match.finalRuns2;
            match.wickets1 = match.finalWickets1;
            match.wickets2 = match.finalWickets2;
            match.overs1 = match.finalOvers1;
            match.overs2 = match.finalOvers2;
            changed = true;
        }
        if ((match.ballHistory == null || match.ballHistory.isEmpty())
                && match.scoreboardSnapshot != null
                && !match.scoreboardSnapshot.isEmpty()) {
            if (match.ballHistory == null) {
                match.ballHistory = new ArrayList<>();
            } else {
                match.ballHistory.clear();
            }
            for (BallEvent ev : match.scoreboardSnapshot) {
                match.ballHistory.add(BallEvent.copyOf(ev));
            }
            changed = true;
        }
        if (changed) {
            MatchPersistenceHelper.syncJsonFromLists(match);
        }
        return changed;
    }

    /** Call when a match row is removed so we do not treat a future reuse of the same id as finished. */
    public static void forgetMatchCompletion(Match match) {
        if (appContext == null || match == null || match.id == null) {
            return;
        }
        prefs().edit().remove(KEY_PREFIX + match.id).apply();
    }

    private static SharedPreferences prefs() {
        return appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static void rememberCompleted(Match match) {
        if (appContext == null || match == null || match.id == null) {
            return;
        }
        prefs().edit().putBoolean(KEY_PREFIX + match.id, true).apply();
    }

    private static boolean isRememberedCompleted(Match match) {
        if (appContext == null || match == null || match.id == null) {
            return false;
        }
        return prefs().getBoolean(KEY_PREFIX + match.id, false);
    }

    /**
     * After load: if prefs say this match was finished, restore flags so Gson/Room JSON stays aligned.
     */
    public static boolean syncCompletionFromRemembered(Match match) {
        if (match == null || !isRememberedCompleted(match)) {
            return false;
        }
        boolean changed = false;
        if (!match.isCompleted) {
            match.isCompleted = true;
            changed = true;
        }
        if (match.matchCompletedAt <= 0L) {
            match.matchCompletedAt = System.currentTimeMillis();
            changed = true;
        }
        return changed;
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
            boolean hasBalls = match.ballHistory != null && !match.ballHistory.isEmpty();
            boolean shouldBeStarted = isEffectivelyCompleted(match)
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
                    || hasBalls;
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

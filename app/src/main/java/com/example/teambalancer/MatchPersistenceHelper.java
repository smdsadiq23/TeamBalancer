package com.example.teambalancer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Room/Gson can drop or null nested {@link BallEvent} lists. We mirror lists as JSON strings so
 * ball-by-ball data survives round-trips to the database.
 */
public final class MatchPersistenceHelper {

    private static final Gson GSON = new Gson();
    private static final Type BALL_LIST_TYPE = new TypeToken<ArrayList<BallEvent>>() {}.getType();

    private MatchPersistenceHelper() {}

    public static void ensureListsNotNull(Match m) {
        if (m == null) {
            return;
        }
        if (m.ballHistory == null) {
            m.ballHistory = new ArrayList<>();
        }
        if (m.scoreboardSnapshot == null) {
            m.scoreboardSnapshot = new ArrayList<>();
        }
        coalesceJsonStrings(m);
    }

    /** Gson may leave JSON mirror fields null when keys were missing in older stored rows. */
    public static void coalesceJsonStrings(Match m) {
        if (m == null) {
            return;
        }
        if (m.ballHistoryJson == null) {
            m.ballHistoryJson = "";
        }
        if (m.scoreboardSnapshotJson == null) {
            m.scoreboardSnapshotJson = "";
        }
    }

    /** Call after any change to {@link Match#ballHistory} or {@link Match#scoreboardSnapshot}. */
    public static void syncJsonFromLists(Match m) {
        if (m == null) {
            return;
        }
        ensureListsNotNull(m);
        m.ballHistoryJson = GSON.toJson(m.ballHistory);
        m.scoreboardSnapshotJson = GSON.toJson(m.scoreboardSnapshot);
    }

    /**
     * Restores lists when Gson left them empty/null but the JSON string column survived.
     *
     * @return true if the match was modified.
     */
    public static boolean restoreListsFromJson(Match m) {
        if (m == null) {
            return false;
        }
        ensureListsNotNull(m);
        boolean changed = false;
        if (m.ballHistory.isEmpty() && nonEmptyJson(m.ballHistoryJson)) {
            List<BallEvent> parsed = GSON.fromJson(m.ballHistoryJson, BALL_LIST_TYPE);
            if (parsed != null && !parsed.isEmpty()) {
                m.ballHistory.clear();
                m.ballHistory.addAll(parsed);
                changed = true;
            }
        }
        if (m.scoreboardSnapshot.isEmpty() && nonEmptyJson(m.scoreboardSnapshotJson)) {
            List<BallEvent> parsed = GSON.fromJson(m.scoreboardSnapshotJson, BALL_LIST_TYPE);
            if (parsed != null && !parsed.isEmpty()) {
                m.scoreboardSnapshot.clear();
                m.scoreboardSnapshot.addAll(parsed);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean nonEmptyJson(String json) {
        if (json == null) {
            return false;
        }
        String t = json.trim();
        return !t.isEmpty() && !"[]".equals(t) && !"null".equalsIgnoreCase(t);
    }
}

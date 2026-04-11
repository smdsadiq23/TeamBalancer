package com.example.teambalancer;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Club history is stored as JSON. Nested {@link Match} lists can lose {@link BallEvent} rows on
 * round-trip; we always mirror JSON strings on write ({@link MatchPersistenceHelper#syncJsonFromLists})
 * and normalize after read ({@link SessionMatchLoader#prepareMatchesForSession}).
 */
public class Converters {

    private static final Gson HISTORY_GSON = new Gson();
    @TypeConverter
    public static Player.Style fromStyle(String value) {
        return value == null ? null : Player.Style.valueOf(value);
    }

    @TypeConverter
    public static String toStyle(Player.Style style) {
        return style == null ? null : style.name();
    }

    @TypeConverter
    public static Player.Category fromCategory(String value) {
        return value == null ? null : Player.Category.valueOf(value);
    }

    @TypeConverter
    public static String toCategory(Player.Category category) {
        return category == null ? null : category.name();
    }

    @TypeConverter
    public static List<String> fromString(String value) {
        Type listType = new TypeToken<ArrayList<String>>() {}.getType();
        return HISTORY_GSON.fromJson(value, listType);
    }

    @TypeConverter
    public static String fromList(List<String> list) {
        return HISTORY_GSON.toJson(list);
    }

    @TypeConverter
    public static List<Club.TeamHistory> fromHistory(String value) {
        if (value == null || value.isEmpty()) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<ArrayList<Club.TeamHistory>>() {}.getType();
        List<Club.TeamHistory> history = HISTORY_GSON.fromJson(value, listType);
        if (history == null) {
            return new ArrayList<>();
        }
        for (Club.TeamHistory th : history) {
            if (th.matches != null) {
                SessionMatchLoader.prepareMatchesForSession(th.matches);
            }
        }
        return history;
    }

    @TypeConverter
    public static String toHistory(List<Club.TeamHistory> history) {
        if (history == null) {
            return "[]";
        }
        for (Club.TeamHistory th : history) {
            if (th.matches == null) {
                continue;
            }
            for (Match m : th.matches) {
                MatchPersistenceHelper.syncJsonFromLists(m);
            }
        }
        return HISTORY_GSON.toJson(history);
    }
}

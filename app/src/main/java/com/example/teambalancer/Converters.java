package com.example.teambalancer;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Converters {
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
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromList(List<String> list) {
        return new Gson().toJson(list);
    }

    @TypeConverter
    public static List<Club.TeamHistory> fromHistory(String value) {
        Type listType = new TypeToken<ArrayList<Club.TeamHistory>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String toHistory(List<Club.TeamHistory> history) {
        return new Gson().toJson(history);
    }
}

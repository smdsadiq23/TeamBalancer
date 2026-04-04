package com.example.teambalancer;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import java.util.List;

public class DataManager {
    private static final String PREFS_NAME = "TeamBalancerPrefs";
    private static final String KEY_LAST_CLUB = "last_club_name";
    private static final String KEY_LOGGED_IN_USER = "logged_in_user";

    private final SharedPreferences prefs;
    private final AppDatabase db;
    private String lastClubName;

    public DataManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        db = AppDatabase.getInstance(context);
        lastClubName = prefs.getString(KEY_LAST_CLUB, "Default Club");
        ensureDefaultClub();
    }

    private void ensureDefaultClub() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (db.clubDao().getAllClubsSync().isEmpty()) {
                Club defaultClub = new Club("Default Club");
                db.clubDao().insert(defaultClub);
            }
        });
    }

    public LiveData<List<Club>> getClubs() {
        return db.clubDao().getAllClubs();
    }

    public LiveData<List<ClubWithPlayers>> getClubsWithPlayers() {
        return db.clubDao().getClubsWithPlayers();
    }

    public LiveData<Club> getCurrentClub() {
        return db.clubDao().getClubByName(lastClubName);
    }
    
    public LiveData<Club> getClubById(int id) {
        return db.clubDao().getClubById(id);
    }

    public LiveData<List<Player>> getPlayersForClub(int clubId) {
        return db.playerDao().getPlayersForClub(clubId);
    }

    public void setCurrentClub(String name) {
        this.lastClubName = name;
        prefs.edit().putString(KEY_LAST_CLUB, name).apply();
    }

    public void setLoggedInUser(String username) {
        prefs.edit().putString(KEY_LOGGED_IN_USER, username).apply();
    }

    public String getLoggedInUser() {
        return prefs.getString(KEY_LOGGED_IN_USER, "Guest");
    }

    public void addPlayer(Player player) {
        AppDatabase.databaseWriteExecutor.execute(() -> db.playerDao().insert(player));
    }

    public void updatePlayer(Player player) {
        AppDatabase.databaseWriteExecutor.execute(() -> db.playerDao().update(player));
    }

    public void deletePlayer(Player player) {
        AppDatabase.databaseWriteExecutor.execute(() -> db.playerDao().delete(player));
    }

    public void addClub(Club club) {
        AppDatabase.databaseWriteExecutor.execute(() -> db.clubDao().insert(club));
    }

    public void updateClub(Club club) {
        AppDatabase.databaseWriteExecutor.execute(() -> db.clubDao().update(club));
    }

    public void deleteClub(Club club) {
        AppDatabase.databaseWriteExecutor.execute(() -> db.clubDao().delete(club));
    }
}

package com.example.teambalancer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.function.Consumer;

public class DataManager {
    static final String PREFS_NAME = "TeamBalancerPrefs";
    private static final String KEY_LAST_CLUB = "last_club_name";
    private static final String KEY_LOGGED_IN_USER = "logged_in_user";

    private final Context appContext;
    private final SharedPreferences prefs;
    private final AppDatabase db;
    private String lastClubName;

    public DataManager(Context context) {
        appContext = context.getApplicationContext();
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        db = AppDatabase.getInstance(appContext);
        lastClubName = prefs.getString(KEY_LAST_CLUB, "Default Club");
        ensureDefaultClub();
    }

    /** Active club label from prefs (does not touch Room). Sync uses this path. */
    public static String peekCurrentClubName(Context ctx) {
        return ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_CLUB, "Default Club");
    }

    private void ensureDefaultClub() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (db.clubDao().getAllClubsSync().isEmpty()) {
                Club defaultClub = new Club("Default Club");
                long rowId = db.clubDao().insert(defaultClub);
                defaultClub.id = (int) rowId;
                EndToEndSync.onClubInserted(appContext, defaultClub);
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
        EndToEndSync.schedulePull(appContext);
    }

    /** Active club name (for reloading from DB after navigation). */
    public String getCurrentClubName() {
        return lastClubName;
    }

    public void setLoggedInUser(String username) {
        prefs.edit().putString(KEY_LOGGED_IN_USER, username).apply();
    }

    public String getLoggedInUser() {
        return prefs.getString(KEY_LOGGED_IN_USER, "Guest");
    }

    public void addPlayer(Player player) {
        AppDatabase.databaseWriteExecutor.execute(
                () -> {
                    long id = db.playerDao().insert(player);
                    player.id = (int) id;
                    EndToEndSync.onPlayerInserted(appContext, player);
                });
    }

    public void updatePlayer(Player player) {
        AppDatabase.databaseWriteExecutor.execute(
                () -> {
                    db.playerDao().update(player);
                    EndToEndSync.onPlayerUpdated(appContext, player);
                });
    }

    public void deletePlayer(Player player) {
        AppDatabase.databaseWriteExecutor.execute(
                () -> {
                    Integer remote = player.remoteId;
                    db.playerDao().delete(player);
                    EndToEndSync.onPlayerDeleted(appContext, remote);
                });
    }

    public void addClub(Club club) {
        AppDatabase.databaseWriteExecutor.execute(
                () -> {
                    long row = db.clubDao().insert(club);
                    club.id = (int) row;
                    EndToEndSync.onClubInserted(appContext, club);
                });
    }

    public void updateClub(Club club) {
        AppDatabase.databaseWriteExecutor.execute(
                () -> {
                    db.clubDao().update(club);
                    EndToEndSync.debouncePushClub(appContext);
                });
    }

    /**
     * Replaces the {@link Match} with the same {@link Match#id} in the club JSON and persists.
     * Used by {@link CricketScoringActivity} after each change so Room stays the source of truth.
     */
    public void replaceMatchInClubAndSave(String clubName, Match updated) {
        if (clubName == null || updated == null || updated.id == null) {
            return;
        }
        MatchPersistenceHelper.syncJsonFromLists(updated);
        AppDatabase.databaseWriteExecutor.execute(
                () -> {
                    Club c = db.clubDao().getClubByNameSync(clubName);
                    if (c == null || !replaceMatchInClub(c, updated)) {
                        return;
                    }
                    db.clubDao().update(c);
                    EndToEndSync.debouncePushClub(appContext);
                });
    }

    private static boolean replaceMatchInClub(Club c, Match updated) {
        if (c.history == null) {
            return false;
        }
        for (Club.TeamHistory th : c.history) {
            if (th.matches == null) {
                continue;
            }
            for (int i = 0; i < th.matches.size(); i++) {
                Match m = th.matches.get(i);
                if (updated.id.equals(m.id)) {
                    th.matches.set(i, updated);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Reads the current club from SQLite (after any pending writes), runs {@link SessionMatchLoader}
     * normalization, persists if needed, then delivers the result on the main thread.
     */
    public void loadClubFromDatabaseAsync(@Nullable Consumer<Club> onMainThread) {
        final String name = lastClubName;
        AppDatabase.databaseWriteExecutor.execute(
                () -> {
                    Club c = db.clubDao().getClubByNameSync(name);
                    boolean needWrite = false;
                    if (c != null && c.history != null && !c.history.isEmpty()) {
                        Club.TeamHistory th = c.history.get(c.history.size() - 1);
                        if (th.matches != null) {
                            needWrite = SessionMatchLoader.prepareMatchesForSession(th.matches);
                        }
                    }
                    if (needWrite && c != null) {
                        db.clubDao().update(c);
                        c = db.clubDao().getClubByNameSync(name);
                    }
                    final Club out = c;
                    new Handler(Looper.getMainLooper())
                            .post(
                                    () -> {
                                        if (onMainThread != null) {
                                            onMainThread.accept(out);
                                        }
                                    });
                });
    }

    public void deleteClub(Club club) {
        AppDatabase.databaseWriteExecutor.execute(
                () -> {
                    Integer remote = club.remoteId;
                    EndToEndSync.notifyClubDeleted(appContext, remote);
                    db.clubDao().delete(club);
                });
    }
}

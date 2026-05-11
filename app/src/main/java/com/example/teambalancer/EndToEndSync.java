package com.example.teambalancer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Response;

/** Syncs clubs, match history, team names, and players to PostgreSQL via the Next.js API. */
public final class EndToEndSync {

    private static final String TAG = "TeamBalancerSync";
    private static final Gson GSON = new Gson();

    private static final Type STRING_LIST_TYPE = new TypeToken<ArrayList<String>>() {}.getType();
    private static final Type HISTORY_LIST_TYPE = new TypeToken<ArrayList<Club.TeamHistory>>() {}.getType();

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static Runnable deferredPush;

    private EndToEndSync() {}

    public static void debouncePushClub(Context appCtx) {
        if (!SecureSessionStore.hasToken(appCtx)) {
            return;
        }
        final Context app = appCtx.getApplicationContext();
        MAIN.removeCallbacks(deferredPush);
        deferredPush =
                () -> AppDatabase.databaseWriteExecutor.execute(() -> pushClubAggregateBlocking(app));
        MAIN.postDelayed(deferredPush, 1600);
    }

    public static void schedulePull(Context context) {
        if (!SecureSessionStore.hasToken(context)) {
            return;
        }
        Context app = context.getApplicationContext();
        AppDatabase.databaseWriteExecutor.execute(
                () -> {
                    try {
                        pullClubBlocking(app);
                    } catch (Exception e) {
                        Log.w(TAG, "pull failed", e);
                    }
                });
    }

    public static void onClubInserted(Context appCtx, Club club) {
        if (!SecureSessionStore.hasToken(appCtx) || club == null) {
            return;
        }
        AppDatabase.databaseWriteExecutor.execute(
                () -> {
                    try {
                        TeamBalancerApi api = ApiModule.api();
                        AppDatabase db = AppDatabase.getInstance(appCtx);
                        Club fresh = db.clubDao().getClubByIdSync(club.id);
                        if (fresh == null || fresh.remoteId != null) {
                            return;
                        }
                        Response<TeamBalancerApi.ClubCreateResponseDto> resp =
                                api.createClub(new TeamBalancerApi.CreateClubBody(fresh.name)).execute();
                        if (resp.isSuccessful() && resp.body() != null && resp.body().club != null) {
                            fresh.remoteId = resp.body().club.id;
                            db.clubDao().update(fresh);
                            return;
                        }
                        Integer sid = lookupRemoteClubId(api, fresh.name);
                        if (sid != null) {
                            fresh.remoteId = sid;
                            db.clubDao().update(fresh);
                        }
                    } catch (IOException e) {
                        Log.w(TAG, "club create sync", e);
                    }
                });
    }

    public static void notifyClubDeleted(Context appCtx, @Nullable Integer remoteClubId) {
        if (!SecureSessionStore.hasToken(appCtx) || remoteClubId == null) {
            return;
        }
        AppDatabase.databaseWriteExecutor.execute(
                () -> {
                    try {
                        ApiModule.api().deleteRemoteClub(remoteClubId).execute();
                    } catch (Exception e) {
                        Log.w(TAG, "club delete sync", e);
                    }
                });
    }

    public static void onPlayerInserted(Context appCtx, Player player) {
        if (!SecureSessionStore.hasToken(appCtx) || player == null) {
            return;
        }
        AppDatabase.databaseWriteExecutor.execute(() -> upsertRemotePlayerBlocking(appCtx, player));
    }

    public static void onPlayerUpdated(Context appCtx, Player player) {
        if (!SecureSessionStore.hasToken(appCtx) || player == null) {
            return;
        }
        AppDatabase.databaseWriteExecutor.execute(() -> upsertRemotePlayerBlocking(appCtx, player));
    }

    public static void onPlayerDeleted(Context appCtx, @Nullable Integer remotePlayerId) {
        if (!SecureSessionStore.hasToken(appCtx) || remotePlayerId == null) {
            return;
        }
        AppDatabase.databaseWriteExecutor.execute(
                () -> {
                    try {
                        ApiModule.api().deleteRemotePlayer(remotePlayerId).execute();
                    } catch (Exception e) {
                        Log.w(TAG, "player delete sync", e);
                    }
                });
    }

    private static void pushClubAggregateBlocking(Context appCtx) {
        try {
            AppDatabase db = AppDatabase.getInstance(appCtx);
            TeamBalancerApi api = ApiModule.api();
            String name = DataManager.peekCurrentClubName(appCtx);
            Club c = db.clubDao().getClubByNameSync(name);
            if (c == null) {
                return;
            }
            Integer rid = ensureRemoteClub(db, api, c);
            if (rid == null) {
                return;
            }
            syncMatchJsonMirrorsBeforePush(c);

            TeamBalancerApi.PatchClubBody patch = new TeamBalancerApi.PatchClubBody();
            patch.teamNames = c.teamNames != null ? new ArrayList<>(c.teamNames) : new ArrayList<>();
            patch.history =
                    GSON.toJsonTree(c.history != null ? c.history : new ArrayList<Club.TeamHistory>());

            Response<TeamBalancerApi.ClubEnvelopeDto> resp = api.patchClub(rid, patch).execute();
            if (!resp.isSuccessful()) {
                Log.w(TAG, "PATCH club failed: " + ApiErrorReader.readMessage(resp));
            }
        } catch (Exception e) {
            Log.w(TAG, "push club", e);
        }
    }

    private static void syncMatchJsonMirrorsBeforePush(Club club) {
        if (club == null || club.history == null) {
            return;
        }
        for (Club.TeamHistory th : club.history) {
            if (th == null || th.matches == null) {
                continue;
            }
            for (Match m : th.matches) {
                MatchPersistenceHelper.syncJsonFromLists(m);
            }
        }
    }

    private static void pullClubBlocking(Context appCtx) throws Exception {
        AppDatabase db = AppDatabase.getInstance(appCtx);
        TeamBalancerApi api = ApiModule.api();

        String name = DataManager.peekCurrentClubName(appCtx);
        Club local = db.clubDao().getClubByNameSync(name);
        if (local == null) {
            return;
        }

        Integer rid = local.remoteId != null ? local.remoteId : lookupRemoteClubId(api, local.name);
        if (rid == null) {
            rid = ensureRemoteClub(db, api, local);
        }
        if (rid == null) {
            return;
        }

        local.remoteId = rid;
        db.clubDao().update(local);

        Response<TeamBalancerApi.ClubEnvelopeDto> resp = api.getClub(rid).execute();
        if (!resp.isSuccessful() || resp.body() == null || resp.body().club == null) {
            return;
        }
        TeamBalancerApi.ClubRemoteDto dto = resp.body().club;

        local.teamNames = parseStringArray(dto.teamNames);
        local.history = parseHistory(dto.history);
        normalizePulledMatches(local.history);
        db.clubDao().update(local);

        db.playerDao().deletePlayersForClub(local.id);

        TeamBalancerApi.ServerPlayerDto[] sp = dto.players;
        if (sp != null) {
            for (TeamBalancerApi.ServerPlayerDto srv : sp) {
                if (srv == null) {
                    continue;
                }
                Player p = playerFromDto(srv, local.id);
                if (p != null) {
                    db.playerDao().insert(p);
                }
            }
        }
    }

    private static void normalizePulledMatches(@Nullable List<Club.TeamHistory> history) {
        if (history == null) {
            return;
        }
        for (Club.TeamHistory th : history) {
            if (th != null && th.matches != null) {
                SessionMatchLoader.prepareMatchesForSession(th.matches);
            }
        }
    }

    private static Player playerFromDto(TeamBalancerApi.ServerPlayerDto s, int localClubId) {
        try {
            Player p =
                    new Player(
                            s.name,
                            Player.Style.valueOf(s.style),
                            Player.Category.valueOf(s.category),
                            s.battingRating,
                            s.bowlingRating,
                            s.fieldingRating,
                            s.isCaptain);
            p.clubId = localClubId;
            p.isAvailable = s.isAvailable;
            p.remoteId = s.id;
            return p;
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "skip bad player dto " + s.name, e);
            return null;
        }
    }

    private static ArrayList<String> parseStringArray(JsonElement el) {
        if (el == null || el.isJsonNull()) {
            return new ArrayList<>();
        }
        ArrayList<String> out = GSON.fromJson(el, STRING_LIST_TYPE);
        return out != null ? out : new ArrayList<>();
    }

    private static ArrayList<Club.TeamHistory> parseHistory(JsonElement el) {
        if (el == null || el.isJsonNull()) {
            return new ArrayList<>();
        }
        ArrayList<Club.TeamHistory> out = GSON.fromJson(el, HISTORY_LIST_TYPE);
        return out != null ? out : new ArrayList<>();
    }

    private static Integer lookupRemoteClubId(TeamBalancerApi api, String clubName) throws IOException {
        Response<TeamBalancerApi.ClubsListResponseDto> r = api.listClubs().execute();
        if (!r.isSuccessful() || r.body() == null || r.body().clubs == null) {
            return null;
        }
        for (TeamBalancerApi.ClubListItemDto item : r.body().clubs) {
            if (item != null && clubName.equals(item.name)) {
                return item.id;
            }
        }
        return null;
    }

    @Nullable
    private static Integer ensureRemoteClub(AppDatabase db, TeamBalancerApi api, Club c) throws IOException {
        if (c.remoteId != null) {
            return c.remoteId;
        }
        Integer rid = lookupRemoteClubId(api, c.name);
        if (rid != null) {
            c.remoteId = rid;
            db.clubDao().update(c);
            return rid;
        }
        Response<TeamBalancerApi.ClubCreateResponseDto> resp =
                api.createClub(new TeamBalancerApi.CreateClubBody(c.name)).execute();
        if (resp.isSuccessful() && resp.body() != null && resp.body().club != null) {
            c.remoteId = resp.body().club.id;
            db.clubDao().update(c);
            return c.remoteId;
        }
        return null;
    }

    private static void upsertRemotePlayerBlocking(Context appCtx, Player ref) {
        try {
            AppDatabase db = AppDatabase.getInstance(appCtx);
            TeamBalancerApi api = ApiModule.api();
            Player p = db.playerDao().getPlayerByIdSync(ref.id);
            if (p == null) {
                return;
            }
            Club club = db.clubDao().getClubByIdSync(p.clubId);
            if (club == null) {
                return;
            }
            Integer rid = ensureRemoteClub(db, api, club);
            if (rid == null) {
                return;
            }
            TeamBalancerApi.PlayerUpsertDto dto = new TeamBalancerApi.PlayerUpsertDto(p);
            if (p.remoteId == null) {
                Response<TeamBalancerApi.PlayerCreateEnvelopeDto> r =
                        api.createRemotePlayer(rid, dto).execute();
                if (r.isSuccessful() && r.body() != null && r.body().player != null) {
                    p.remoteId = r.body().player.id;
                    db.playerDao().update(p);
                } else if (!r.isSuccessful()) {
                    Log.w(TAG, "POST player failed: " + ApiErrorReader.readMessage(r));
                }
            } else {
                Response<TeamBalancerApi.ServerPlayerDto> r =
                        api.patchRemotePlayer(p.remoteId, dto).execute();
                if (!r.isSuccessful()) {
                    Log.w(TAG, "PATCH player failed: " + ApiErrorReader.readMessage(r));
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "player sync", e);
        }
    }
}

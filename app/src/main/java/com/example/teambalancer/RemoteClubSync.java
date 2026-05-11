package com.example.teambalancer;

import androidx.annotation.Nullable;

/**
 * Merges server clubs into Room and tracks {@link Club#remoteId} for API sync.
 */
public final class RemoteClubSync {

    private RemoteClubSync() {}

    public static void mergeServerClubsIntoRoom(AppDatabase db, @Nullable TeamBalancerApi.ClubListItemDto[] clubs) {
        if (clubs == null) {
            return;
        }
        ClubDao dao = db.clubDao();
        for (TeamBalancerApi.ClubListItemDto c : clubs) {
            if (c == null || c.name == null || c.name.isEmpty()) {
                continue;
            }
            Club existing = dao.getClubByNameSync(c.name);
            if (existing != null) {
                existing.remoteId = c.id;
                dao.update(existing);
            } else {
                Club nc = new Club(c.name);
                nc.remoteId = c.id;
                dao.insert(nc);
            }
        }
    }

    public static boolean listContainsName(@Nullable TeamBalancerApi.ClubListItemDto[] clubs, String name) {
        if (clubs == null || name == null) {
            return false;
        }
        for (TeamBalancerApi.ClubListItemDto c : clubs) {
            if (c != null && name.equals(c.name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ensures the club exists on the server and in Room, then returns the canonical name to use.
     */
    public static String ensureClubExistsOnServer(
            AppDatabase db, TeamBalancerApi api, @Nullable TeamBalancerApi.ClubListItemDto[] clubs, String requestedName)
            throws Exception {
        if (requestedName == null || requestedName.isEmpty()) {
            throw new IllegalArgumentException("Club name required");
        }
        String name = requestedName.trim();
        Integer serverId = null;
        if (!listContainsName(clubs, name)) {
            retrofit2.Response<TeamBalancerApi.ClubCreateResponseDto> created =
                    api.createClub(new TeamBalancerApi.CreateClubBody(name)).execute();
            if (!created.isSuccessful()) {
                throw new HttpApiException(created.code(), ApiErrorReader.readMessage(created));
            }
            TeamBalancerApi.ClubCreateResponseDto body = created.body();
            if (body != null && body.club != null && body.club.name != null) {
                name = body.club.name;
            }
            if (body != null && body.club != null) {
                serverId = body.club.id;
            }
        } else if (clubs != null) {
            for (TeamBalancerApi.ClubListItemDto c : clubs) {
                if (c != null && name.equals(c.name)) {
                    serverId = c.id;
                    break;
                }
            }
        }
        ClubDao dao = db.clubDao();
        Club loc = dao.getClubByNameSync(name);
        if (loc == null) {
            Club nc = new Club(name);
            if (serverId != null) {
                nc.remoteId = serverId;
            }
            dao.insert(nc);
        } else if (serverId != null && (loc.remoteId == null || !loc.remoteId.equals(serverId))) {
            loc.remoteId = serverId;
            dao.update(loc);
        }
        return name;
    }
}

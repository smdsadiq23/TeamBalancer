package com.example.teambalancer;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** Retrofit bindings for Next.js `/api/*` routes. */
public interface TeamBalancerApi {

    @POST("api/auth/login")
    Call<AuthResponseDto> login(@Body UsernamePasswordBody body);

    @POST("api/auth/register")
    Call<AuthResponseDto> register(@Body UsernamePasswordBody body);

    @POST("api/auth/logout")
    Call<Void> logout();

    @GET("api/clubs")
    Call<ClubsListResponseDto> listClubs();

    @GET("api/clubs/{id}")
    Call<ClubEnvelopeDto> getClub(@Path("id") int remoteClubId);

    @PATCH("api/clubs/{id}")
    Call<ClubEnvelopeDto> patchClub(@Path("id") int remoteClubId, @Body PatchClubBody body);

    @DELETE("api/clubs/{id}")
    Call<Void> deleteRemoteClub(@Path("id") int remoteClubId);

    @POST("api/clubs")
    Call<ClubCreateResponseDto> createClub(@Body CreateClubBody body);

    @POST("api/clubs/{clubId}/players")
    Call<PlayerCreateEnvelopeDto> createRemotePlayer(@Path("clubId") int remoteClubId, @Body PlayerUpsertDto body);

    @PATCH("api/players/{id}")
    Call<ServerPlayerDto> patchRemotePlayer(@Path("id") int remotePlayerId, @Body PlayerUpsertDto body);

    @DELETE("api/players/{id}")
    Call<Void> deleteRemotePlayer(@Path("id") int remotePlayerId);

    final class UsernamePasswordBody {
        public final String username;
        public final String password;

        UsernamePasswordBody(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    final class AuthResponseDto {
        public String token;
        public UserDto user;
    }

    final class UserDto {
        public int id;
        public String username;
    }

    final class ClubsListResponseDto {
        public ClubListItemDto[] clubs;
    }

    final class ClubListItemDto {
        public int id;
        public String name;
        @SerializedName("_count")
        public PlayerCountDto _count;
    }

    final class PlayerCountDto {
        public int players;
    }

    final class ClubEnvelopeDto {
        public ClubRemoteDto club;
    }

    /** Matches Prisma/club GET + PATCH projection. */
    final class ClubRemoteDto {
        public int id;
        public String name;
        public JsonElement teamNames;
        public JsonElement history;
        public ServerPlayerDto[] players;
    }

    final class ServerPlayerDto {
        public int id;
        public int clubId;
        public String name;
        public String style;
        public String category;
        public int battingRating;
        public int bowlingRating;
        public int fieldingRating;
        public boolean isCaptain;
        public boolean isAvailable;
    }

    /** Partial PATCH body; omit null fields Gson default. */
    final class PatchClubBody {
        public String name;
        public List<String> teamNames;
        public JsonElement history;
    }

    final class CreateClubBody {
        public final String name;

        CreateClubBody(String name) {
            this.name = name;
        }
    }

    final class ClubCreateResponseDto {
        public ClubCreatedDto club;
    }

    final class ClubCreatedDto {
        public int id;
        public String name;
    }

    final class PlayerCreateEnvelopeDto {
        public ServerPlayerDto player;
    }

    /** POST / PATCH player body matches server Zod enums (BATSMAN, REGULAR, …). */
    final class PlayerUpsertDto {
        public final String name;
        public final String style;
        public final String category;
        public final int battingRating;
        public final int bowlingRating;
        public final int fieldingRating;
        public final boolean isCaptain;
        public final boolean isAvailable;

        PlayerUpsertDto(Player p) {
            String n = p.name != null ? p.name : "";
            this.name = n;
            this.style = p.style != null ? p.style.name() : Player.Style.BATSMAN.name();
            this.category =
                    p.category != null ? p.category.name() : Player.Category.REGULAR.name();
            this.battingRating = p.battingRating;
            this.bowlingRating = p.bowlingRating;
            this.fieldingRating = p.fieldingRating;
            this.isCaptain = p.isCaptain;
            this.isAvailable = p.isAvailable;
        }
    }
}

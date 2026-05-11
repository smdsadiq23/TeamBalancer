package com.example.teambalancer;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface PlayerDao {
    @Query("SELECT * FROM players WHERE clubId = :clubId")
    LiveData<List<Player>> getPlayersForClub(int clubId);

    @Query("SELECT * FROM players WHERE clubId = :clubId")
    List<Player> getPlayersForClubSync(int clubId);

    @Query("SELECT * FROM players WHERE id = :id LIMIT 1")
    Player getPlayerByIdSync(int id);

    @Query("DELETE FROM players WHERE clubId = :clubId")
    void deletePlayersForClub(int clubId);

    @Insert
    long insert(Player player);

    @Update
    void update(Player player);

    @Delete
    void delete(Player player);
}

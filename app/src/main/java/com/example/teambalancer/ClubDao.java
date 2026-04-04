package com.example.teambalancer;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ClubDao {
    @Query("SELECT * FROM clubs")
    LiveData<List<Club>> getAllClubs();

    @Transaction
    @Query("SELECT * FROM clubs")
    LiveData<List<ClubWithPlayers>> getClubsWithPlayers();

    @Query("SELECT * FROM clubs WHERE name = :name LIMIT 1")
    LiveData<Club> getClubByName(String name);

    @Query("SELECT * FROM clubs WHERE name = :name LIMIT 1")
    Club getClubByNameSync(String name);

    @Query("SELECT * FROM clubs WHERE id = :id")
    LiveData<Club> getClubById(int id);

    @Query("SELECT * FROM clubs")
    List<Club> getAllClubsSync();

    @Insert
    long insert(Club club);

    @Update
    void update(Club club);

    @Delete
    void delete(Club club);
}

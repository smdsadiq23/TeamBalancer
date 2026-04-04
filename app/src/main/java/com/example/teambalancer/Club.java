package com.example.teambalancer;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "clubs")
public class Club implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    
    // For simplicity in this migration, we'll keep these as converted fields
    // In a fully optimized "million user" design, these would be separate tables
    public List<String> teamNames;
    public List<TeamHistory> history;

    public Club(String name) {
        this.name = name;
        this.teamNames = new ArrayList<>();
        this.history = new ArrayList<>();
    }

    public static class TeamHistory implements Serializable {
        public String date;
        public List<Team> teams;

        public TeamHistory(String date, List<Team> teams) {
            this.date = date;
            this.teams = teams;
        }
    }
}

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

    /** Stable id on PostgreSQL (null until first successful sync). */
    public Integer remoteId;
    public String name;
    
    public List<String> teamNames;
    public List<TeamHistory> history;

    public Club(String name) {
        this.name = name;
        this.teamNames = new ArrayList<>();
        this.history = new ArrayList<>();
    }

    public static class TeamHistory implements Serializable {
        public String date;
        public String sport = "Other";
        public List<Team> teams;
        public List<Match> matches;

        public TeamHistory(String date, List<Team> teams) {
            this(date, teams, "Other");
        }

        public TeamHistory(String date, List<Team> teams, String sport) {
            this.date = date;
            this.teams = teams;
            this.sport = sport;
            this.matches = new ArrayList<>();
        }
    }
}

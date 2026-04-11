package com.example.teambalancer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Match implements Serializable {
    public String id;
    public String team1;
    public String team2;
    public String sport = "Other";
    public List<String> squad1 = new ArrayList<>();
    public List<String> squad2 = new ArrayList<>();

    public int score1 = 0;
    public int score2 = 0;
    public int wickets1 = 0;
    public int wickets2 = 0;
    public double overs1 = 0.0;
    public double overs2 = 0.0;
    public String matchType = "T20"; // T20, ODI, Test, Box Cricket, Custom
    public int maxOvers = 20;
    public boolean hasStarted = false;
    public boolean isCompleted = false;
    public String tossWinner;
    public String tossDecision; // Batting, Bowling
    public int currentInnings = 1; // 1 to 4
    public String battingTeam; // Name of the team currently batting
    public String bowlingTeam; // Name of the team currently bowling
    public List<BallEvent> ballHistory = new ArrayList<>();
    public String striker;
    public String nonStriker;
    public String currentBowler;
    public boolean isFreeHit = false;

    public Match(String team1, String team2) {
        this(team1, team2, "Other");
    }

    public Match(String team1, String team2, String sport) {
        this.id = UUID.randomUUID().toString();
        this.team1 = team1;
        this.team2 = team2;
        this.sport = sport;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Match match = (Match) o;
        return Objects.equals(id, match.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

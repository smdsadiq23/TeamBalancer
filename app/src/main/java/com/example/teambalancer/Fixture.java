package com.example.teambalancer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Fixture implements Serializable {
    public String id;
    public String team1;
    public String team2;
    public String sport = "Other";
    public List<String> squad1 = new ArrayList<>();
    public List<String> squad2 = new ArrayList<>();

    // Legacy embedded match state retained only so old saved data can be migrated.
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

    public Fixture(String team1, String team2) {
        this(team1, team2, "Other");
    }

    public Fixture(String team1, String team2, String sport) {
        this.id = java.util.UUID.randomUUID().toString();
        this.team1 = team1;
        this.team2 = team2;
        this.sport = sport;
    }

    public boolean hasLegacyMatchData() {
        return hasStarted
                || isCompleted
                || score1 > 0
                || score2 > 0
                || wickets1 > 0
                || wickets2 > 0
                || overs1 > 0
                || overs2 > 0
                || tossWinner != null
                || tossDecision != null
                || battingTeam != null
                || bowlingTeam != null
                || !ballHistory.isEmpty();
    }

    public void clearLegacyMatchData() {
        score1 = 0;
        score2 = 0;
        wickets1 = 0;
        wickets2 = 0;
        overs1 = 0.0;
        overs2 = 0.0;
        matchType = "T20";
        maxOvers = 20;
        hasStarted = false;
        isCompleted = false;
        tossWinner = null;
        tossDecision = null;
        currentInnings = 1;
        battingTeam = null;
        bowlingTeam = null;
        ballHistory = new ArrayList<>();
        striker = null;
        nonStriker = null;
        currentBowler = null;
        isFreeHit = false;
    }
}

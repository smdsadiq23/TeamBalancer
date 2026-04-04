package com.example.teambalancer;

import java.io.Serializable;
import java.util.List;

public class Team implements Serializable {
    String name;
    List<Player> players;
    int totalStrength;

    public Team(String name, List<Player> players, int totalStrength) {
        this.name = name;
        this.players = players;
        this.totalStrength = totalStrength;
    }
}

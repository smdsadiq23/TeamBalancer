package com.example.teambalancer;

import androidx.room.Embedded;
import androidx.room.Relation;
import java.util.List;

public class ClubWithPlayers {
    @Embedded
    public Club club;

    @Relation(
        parentColumn = "id",
        entityColumn = "clubId"
    )
    public List<Player> players;
}

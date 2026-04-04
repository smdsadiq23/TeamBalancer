package com.example.teambalancer;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "players",
        foreignKeys = @ForeignKey(entity = Club.class,
                parentColumns = "id",
                childColumns = "clubId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("clubId")})
public class Player implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int clubId;
    public String name;
    public Style style;
    public Category category;
    public int battingRating = 1;
    public int bowlingRating = 1;
    public int fieldingRating = 1;
    public boolean isCaptain;
    public boolean isAvailable = true;

    @Ignore
    public String assignedTeam; // Not persisted in DB, used for team balancing flow

    public enum Style {
        BATSMAN("Batsman"), 
        BOWLER("Bowler"), 
        ALL_ROUNDER("All-rounder");

        public final String displayName;

        Style(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum Category {
        REGULAR("Regular", 10),
        PROMISING_TALENT("Promising Talent", 8),
        VETERAN("Veteran", 7);

        public final String displayName;
        public final int basePower;

        Category(String displayName, int basePower) {
            this.displayName = displayName;
            this.basePower = basePower;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public Player(String name, Style style, Category category, int battingRating, int bowlingRating, int fieldingRating, boolean isCaptain) {
        this.name = name;
        this.style = style;
        this.category = category;
        this.battingRating = battingRating;
        this.bowlingRating = bowlingRating;
        this.fieldingRating = fieldingRating;
        this.isCaptain = isCaptain;
    }

    public int getPower() {
        return (category != null ? category.basePower : 0) + battingRating + bowlingRating + fieldingRating;
    }
}

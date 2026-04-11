package com.example.teambalancer;

import java.util.Locale;

/** Per-bowler figures for the scorecard screen. */
public class BowlingStats {
    public String name;
    /** Legal deliveries bowled (for overs and economy). */
    public int legalBalls;
    public int runsConceded;
    public int wickets;

    public String oversText() {
        int o = legalBalls / 6;
        int b = legalBalls % 6;
        return o + "." + b;
    }

    public double economy() {
        if (legalBalls <= 0) {
            return 0.0;
        }
        double overs = legalBalls / 6.0;
        return runsConceded / overs;
    }

    public String economyText() {
        return String.format(Locale.getDefault(), "%.2f", economy());
    }
}

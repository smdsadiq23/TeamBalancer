package com.example.teambalancer;

import java.util.Locale;

/** Per-player batting figures for the scorecard screen. */
public class BattingStats {
    public String name;
    public int runs;
    public int balls;
    public int fours;
    public int sixes;

    public double strikeRate() {
        if (balls <= 0) {
            return 0.0;
        }
        return (runs * 100.0) / balls;
    }

    public String strikeRateText() {
        return String.format(Locale.getDefault(), "%.1f", strikeRate());
    }
}

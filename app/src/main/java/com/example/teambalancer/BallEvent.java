package com.example.teambalancer;

import java.io.Serializable;

public class BallEvent implements Serializable {
    public int runs = 0;
    public ExtraType extraType = ExtraType.NONE;
    public WicketType wicketType = WicketType.NONE;
    public String striker;
    public String nonStriker;
    public String bowler;
    public boolean isLegalBall = true;
    public int overNumber;
    public int ballNumber;
    public long timestamp;

    public enum ExtraType { NONE, WIDE, NO_BALL, BYE, LEG_BYE }
    public enum WicketType { NONE, BOWLED, CAUGHT, LBW, RUN_OUT, STUMPED, HIT_WICKET, OTHER }

    public BallEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    /** Deep copy for completed-match scoreboard snapshots. */
    public static BallEvent copyOf(BallEvent src) {
        if (src == null) {
            return null;
        }
        BallEvent e = new BallEvent();
        e.runs = src.runs;
        e.extraType = src.extraType;
        e.wicketType = src.wicketType;
        e.striker = src.striker;
        e.nonStriker = src.nonStriker;
        e.bowler = src.bowler;
        e.isLegalBall = src.isLegalBall;
        e.overNumber = src.overNumber;
        e.ballNumber = src.ballNumber;
        e.timestamp = src.timestamp;
        return e;
    }
}

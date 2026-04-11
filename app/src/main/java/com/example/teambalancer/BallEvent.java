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
}

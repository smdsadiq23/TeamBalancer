package com.example.teambalancer;

import android.app.Application;

public class TeamBalancerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ApiModule.init(this);
    }
}

package com.example.teambalancer;

import androidx.fragment.app.Fragment;

/**
 * Opens the main read-only {@link ScorecardActivity} for a cricket match.
 */
public final class CompletedMatchScoreboardDialog {

    private CompletedMatchScoreboardDialog() {}

    public static void show(Fragment fragment, Match match) {
        if (fragment.getContext() == null || match == null) {
            return;
        }
        ScorecardActivity.start(fragment.requireContext(), match);
    }
}

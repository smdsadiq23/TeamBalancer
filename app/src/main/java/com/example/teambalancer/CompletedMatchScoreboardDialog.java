package com.example.teambalancer;

import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

/**
 * Scrollable full scoreboard for completed cricket matches.
 */
public final class CompletedMatchScoreboardDialog {

    private CompletedMatchScoreboardDialog() {}

    public static void show(Fragment fragment, Match match) {
        if (fragment.getContext() == null || match == null) {
            return;
        }
        ScrollView scroll = new ScrollView(fragment.requireContext());
        int pad = (int) (16 * fragment.getResources().getDisplayMetrics().density);
        scroll.setPadding(pad, pad, pad, pad);
        TextView tv = new TextView(fragment.requireContext());
        tv.setTextSize(14f);
        tv.setTextColor(ContextCompat.getColor(fragment.requireContext(), R.color.on_surface));
        tv.setTextIsSelectable(true);
        tv.setText(CricketScoreboardFormatter.formatFull(match));
        scroll.addView(tv);

        new AlertDialog.Builder(fragment.requireContext())
                .setTitle((match.team1 != null ? match.team1 : "") + " vs " + (match.team2 != null ? match.team2 : ""))
                .setView(scroll)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}

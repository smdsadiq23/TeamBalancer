package com.example.teambalancer;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/** Inflates the two-column Playing XI squad rows (mirrored layout like broadcast UIs). */
public final class SquadPlayingXiBinder {

    private SquadPlayingXiBinder() {}

    public static void bind(
            @Nullable LinearLayout rowsContainer,
            @Nullable TextView headerTeam1,
            @Nullable TextView headerTeam2,
            @Nullable Match match,
            LayoutInflater inflater) {
        if (rowsContainer == null || inflater == null || match == null) {
            return;
        }
        if (headerTeam1 != null) {
            headerTeam1.setText(match.team1 != null ? match.team1 : "Team 1");
        }
        if (headerTeam2 != null) {
            headerTeam2.setText(match.team2 != null ? match.team2 : "Team 2");
        }

        rowsContainer.removeAllViews();

        List<String> s1 = match.squad1 != null ? match.squad1 : Collections.emptyList();
        List<String> s2 = match.squad2 != null ? match.squad2 : Collections.emptyList();
        int max = Math.max(s1.size(), s2.size());
        String roleDefault = inflater.getContext().getString(R.string.squad_player_role_default);

        if (max == 0) {
            View row = inflater.inflate(R.layout.item_squad_playing_xi_row, rowsContainer, false);
            bindOneRow(row, "—", "—", roleDefault, roleDefault, 0);
            rowsContainer.addView(row);
            return;
        }

        for (int i = 0; i < max; i++) {
            View row = inflater.inflate(R.layout.item_squad_playing_xi_row, rowsContainer, false);
            String left = i < s1.size() ? s1.get(i) : "";
            String right = i < s2.size() ? s2.get(i) : "";
            bindOneRow(row, left, right, roleDefault, roleDefault, i);
            rowsContainer.addView(row);
        }
    }

    private static void bindOneRow(
            View row,
            String leftName,
            String rightName,
            String leftRole,
            String rightRole,
            int index) {
        TextView tl = row.findViewById(R.id.txtSquadLeftName);
        TextView tr = row.findViewById(R.id.txtSquadRightName);
        TextView rl = row.findViewById(R.id.txtSquadLeftRole);
        TextView rr = row.findViewById(R.id.txtSquadRightRole);

        if (leftName == null || leftName.isEmpty()) {
            tl.setText("");
            rl.setVisibility(View.GONE);
        } else {
            tl.setText(leftName);
            rl.setText(leftRole);
            rl.setVisibility(View.VISIBLE);
        }

        if (rightName == null || rightName.isEmpty()) {
            tr.setText("");
            rr.setVisibility(View.GONE);
        } else {
            tr.setText(rightName);
            rr.setText(rightRole);
            rr.setVisibility(View.VISIBLE);
        }

        if (index % 2 == 1) {
            row.setBackgroundResource(R.drawable.bg_playing_xi_row_alt);
        } else {
            row.setBackground(null);
        }
    }
}

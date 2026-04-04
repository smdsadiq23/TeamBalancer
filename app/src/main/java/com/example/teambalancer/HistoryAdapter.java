package com.example.teambalancer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private final List<Club.TeamHistory> historyList;
    private final OnHistoryActionListener listener;

    public interface OnHistoryActionListener {
        void onHistoryClick(Club.TeamHistory history);
        void onHistoryDelete(int position);
    }

    public HistoryAdapter(List<Club.TeamHistory> historyList, OnHistoryActionListener listener) {
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Club.TeamHistory history = historyList.get(position);
        holder.txtDate.setText(history.date);

        LayoutInflater inflater = holder.inflater;
        LinearLayout layoutTeams = holder.layoutTeams;
        List<Team> teams = history.teams;
        int teamCount = teams != null ? teams.size() : 0;
        int existing = layoutTeams.getChildCount();

        int totalPlayers = 0;
        for (int i = 0; i < teamCount; i++) {
            Team team = teams.get(i);
            totalPlayers += team.players.size();

            View teamView;
            if (i < existing) {
                teamView = layoutTeams.getChildAt(i);
            } else {
                teamView = inflater.inflate(R.layout.item_history_team, layoutTeams, false);
                layoutTeams.addView(teamView);
            }
            teamView.setVisibility(View.VISIBLE);

            TextView txtTeamName = teamView.findViewById(R.id.txtTeamName);
            TextView txtTeamStrength = teamView.findViewById(R.id.txtTeamStrength);
            TextView txtTeamPlayers = teamView.findViewById(R.id.txtTeamPlayers);

            txtTeamName.setText(team.name);
            txtTeamStrength.setText("Str: " + team.totalStrength);

            StringBuilder playersList = new StringBuilder(team.players.size() * 16);
            for (int j = 0; j < team.players.size(); j++) {
                Player p = team.players.get(j);
                playersList.append(p.isCaptain ? "★ " : "• ").append(p.name);
                if (j < team.players.size() - 1) {
                    playersList.append('\n');
                }
            }
            txtTeamPlayers.setText(playersList.toString());
        }

        for (int i = teamCount; i < existing; i++) {
            layoutTeams.getChildAt(i).setVisibility(View.GONE);
        }

        String summary = teamCount + " Teams, " + totalPlayers + " Players total";
        holder.txtSummary.setText(summary);

        holder.btnView.setOnClickListener(v -> listener.onHistoryClick(history));
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onHistoryDelete(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView txtDate, txtSummary;
        LinearLayout layoutTeams;
        MaterialButton btnView, btnDelete;
        final LayoutInflater inflater;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            inflater = LayoutInflater.from(itemView.getContext());
            txtDate = itemView.findViewById(R.id.txtHistoryDate);
            txtSummary = itemView.findViewById(R.id.txtHistorySummary);
            layoutTeams = itemView.findViewById(R.id.layoutHistoryTeams);
            btnView = itemView.findViewById(R.id.btnViewHistory);
            btnDelete = itemView.findViewById(R.id.btnDeleteHistory);
        }
    }
}

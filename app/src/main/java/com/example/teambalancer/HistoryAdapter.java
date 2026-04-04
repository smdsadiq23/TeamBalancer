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
        
        int totalPlayers = 0;
        holder.layoutTeams.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());

        for (Team team : history.teams) {
            totalPlayers += team.players.size();
            
            View teamView = inflater.inflate(R.layout.item_history_team, holder.layoutTeams, false);
            TextView txtTeamName = teamView.findViewById(R.id.txtTeamName);
            TextView txtTeamStrength = teamView.findViewById(R.id.txtTeamStrength);
            TextView txtTeamPlayers = teamView.findViewById(R.id.txtTeamPlayers);
            
            txtTeamName.setText(team.name);
            txtTeamStrength.setText("Str: " + team.totalStrength);
            
            StringBuilder playersList = new StringBuilder();
            for (int i = 0; i < team.players.size(); i++) {
                Player p = team.players.get(i);
                playersList.append(p.isCaptain ? "★ " : "• ").append(p.name);
                if (i < team.players.size() - 1) {
                    playersList.append("\n");
                }
            }
            txtTeamPlayers.setText(playersList.toString());
            
            holder.layoutTeams.addView(teamView);
        }
        
        String summary = history.teams.size() + " Teams, " + totalPlayers + " Players total";
        holder.txtSummary.setText(summary);
        
        holder.btnView.setOnClickListener(v -> listener.onHistoryClick(history));
        holder.btnDelete.setOnClickListener(v -> listener.onHistoryDelete(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView txtDate, txtSummary;
        LinearLayout layoutTeams;
        MaterialButton btnView, btnDelete;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            txtDate = itemView.findViewById(R.id.txtHistoryDate);
            txtSummary = itemView.findViewById(R.id.txtHistorySummary);
            layoutTeams = itemView.findViewById(R.id.layoutHistoryTeams);
            btnView = itemView.findViewById(R.id.btnViewHistory);
            btnDelete = itemView.findViewById(R.id.btnDeleteHistory);
        }
    }
}

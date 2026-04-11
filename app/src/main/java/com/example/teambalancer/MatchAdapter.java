package com.example.teambalancer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.MatchViewHolder> {

    private final List<Match> matches;
    private OnMatchActionListener listener;

    public interface OnMatchActionListener {
        void onEditMatch(Match match, int position);
        void onDeleteMatch(Match match, int position);
        void onViewMatch(Match match, int position);
    }

    public MatchAdapter(List<Match> matches) {
        this.matches = matches;
    }

    public void setOnMatchActionListener(OnMatchActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fixture, parent, false);
        return new MatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        Match match = matches.get(position);

        holder.txtTeam1.setText(match.team1);
        holder.txtTeam2.setText(match.team2);

        boolean isStarted = match.isCompleted || match.score1 > 0 || match.score2 > 0 || (match.ballHistory != null && !match.ballHistory.isEmpty());

        if (match.isCompleted) {
            holder.txtStatus.setVisibility(View.VISIBLE);
            holder.txtStatus.setText("COMPLETED");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_stat_pill);
            
            holder.layoutScores.setVisibility(View.VISIBLE);
            holder.txtVs.setVisibility(View.GONE);
            
            holder.txtResult.setVisibility(View.VISIBLE);
            holder.txtResult.setText(getWinnerString(match));

            if ("Cricket".equalsIgnoreCase(match.sport)) {
                holder.txtScore1.setText(match.score1 + "/" + match.wickets1);
                holder.txtScore2.setText(match.score2 + "/" + match.wickets2);
            } else {
                holder.txtScore1.setText(String.valueOf(match.score1));
                holder.txtScore2.setText(String.valueOf(match.score2));
            }
        } else if (isStarted) {
            holder.txtStatus.setVisibility(View.VISIBLE);
            holder.txtStatus.setText("LIVE");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_stat_pill);
            
            holder.layoutScores.setVisibility(View.VISIBLE);
            holder.txtVs.setVisibility(View.GONE);
            holder.txtResult.setVisibility(View.GONE);

            if ("Cricket".equalsIgnoreCase(match.sport)) {
                holder.txtScore1.setText(match.score1 + "/" + match.wickets1);
                holder.txtScore2.setText(match.score2 + "/" + match.wickets2);
            } else {
                holder.txtScore1.setText(String.valueOf(match.score1));
                holder.txtScore2.setText(String.valueOf(match.score2));
            }
        } else {
            holder.txtStatus.setVisibility(View.GONE);
            holder.layoutScores.setVisibility(View.GONE);
            holder.txtVs.setVisibility(View.VISIBLE);
            holder.txtResult.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                if (match.isCompleted) {
                    listener.onViewMatch(match, position);
                } else {
                    listener.onEditMatch(match, position);
                }
            }
        });

        holder.btnDeleteMatch.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteMatch(match, position);
        });
    }

    private String getWinnerString(Match match) {
        if (match.score1 == match.score2) return "Match Tied";
        
        if (match.score1 > match.score2) {
            return match.team1 + " won";
        } else {
            return match.team2 + " won";
        }
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }

    static class MatchViewHolder extends RecyclerView.ViewHolder {
        TextView txtTeam1, txtTeam2, txtScore1, txtScore2, txtVs, txtStatus, txtResult;
        View layoutScores;
        ImageButton btnDeleteMatch;

        MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTeam1 = itemView.findViewById(R.id.txtTeam1);
            txtTeam2 = itemView.findViewById(R.id.txtTeam2);
            txtScore1 = itemView.findViewById(R.id.txtScore1);
            txtScore2 = itemView.findViewById(R.id.txtScore2);
            txtVs = itemView.findViewById(R.id.txtVs);
            txtStatus = itemView.findViewById(R.id.txtMatchStatus);
            txtResult = itemView.findViewById(R.id.txtResult);
            layoutScores = itemView.findViewById(R.id.layoutScores);
            btnDeleteMatch = itemView.findViewById(R.id.btnDeleteFixture);
        }
    }
}

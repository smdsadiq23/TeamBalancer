package com.example.teambalancer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.MatchViewHolder> {

    private List<Match> matches;
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

    public void updateMatches(List<Match> matches) {
        this.matches = matches;
        notifyDataSetChanged();
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
        if (match == null) return;

        holder.txtTeam1.setText(match.team1 != null ? match.team1 : "");
        holder.txtTeam2.setText(match.team2 != null ? match.team2 : "");
        MatchDisplayHelper.bindFixtureMetaLine(holder.txtFixtureMeta, match);

        if (MatchCompletionHelper.isEffectivelyCompleted(match)) {
            holder.txtStatus.setVisibility(View.VISIBLE);
            holder.txtStatus.setText("COMPLETED");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_stat_pill);
            
            holder.layoutScores.setVisibility(View.VISIBLE);
            holder.txtVs.setVisibility(View.GONE);
            
            holder.txtResult.setVisibility(View.VISIBLE);
            holder.txtResult.setText(getResultLine(match));

            if ("Cricket".equalsIgnoreCase(match.sport)) {
                holder.txtScore1.setText(MatchScoreDisplay.cricketScoreWithOvers(match, true));
                holder.txtScore2.setText(MatchScoreDisplay.cricketScoreWithOvers(match, false));
            } else {
                holder.txtScore1.setText(String.valueOf(MatchScoreDisplay.runs1(match)));
                holder.txtScore2.setText(String.valueOf(MatchScoreDisplay.runs2(match)));
            }
        } else if (MatchCompletionHelper.hasRecordedPlay(match)) {
            holder.txtStatus.setVisibility(View.VISIBLE);
            holder.txtStatus.setText("LIVE");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_stat_pill);
            
            holder.layoutScores.setVisibility(View.VISIBLE);
            holder.txtVs.setVisibility(View.GONE);
            holder.txtResult.setVisibility(View.GONE);

            if ("Cricket".equalsIgnoreCase(match.sport)) {
                holder.txtScore1.setText(MatchScoreDisplay.cricketScoreWithOvers(match, true));
                holder.txtScore2.setText(MatchScoreDisplay.cricketScoreWithOvers(match, false));
            } else {
                holder.txtScore1.setText(String.valueOf(MatchScoreDisplay.runs1(match)));
                holder.txtScore2.setText(String.valueOf(MatchScoreDisplay.runs2(match)));
            }
        } else {
            String status = MatchCompletionHelper.isPreBallSetup(match) ? "READY" : "SCHEDULED";
            holder.txtStatus.setVisibility(View.VISIBLE);
            holder.txtStatus.setText(status);
            holder.txtStatus.setBackgroundResource(R.drawable.bg_stat_pill);

            holder.layoutScores.setVisibility(View.VISIBLE);
            holder.txtVs.setVisibility(View.GONE);
            holder.txtResult.setVisibility(View.GONE);

            if ("Cricket".equalsIgnoreCase(match.sport)) {
                holder.txtScore1.setText(MatchScoreDisplay.cricketScoreWithOvers(match, true));
                holder.txtScore2.setText(MatchScoreDisplay.cricketScoreWithOvers(match, false));
            } else {
                holder.txtScore1.setText(String.valueOf(MatchScoreDisplay.runs1(match)));
                holder.txtScore2.setText(String.valueOf(MatchScoreDisplay.runs2(match)));
            }
        }

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                Match m = matches.get(pos);
                if (MatchCompletionHelper.isEffectivelyCompleted(m)) {
                    listener.onViewMatch(m, pos);
                } else {
                    listener.onEditMatch(m, pos);
                }
            }
        });

        holder.btnDeleteMatch.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onDeleteMatch(matches.get(pos), pos);
            }
        });
    }

    private String getResultLine(Match match) {
        if ("Cricket".equalsIgnoreCase(match.sport)) {
            return CricketMatchResultFormatter.formatCardSubtitle(match);
        }
        int s1 = MatchScoreDisplay.runs1(match);
        int s2 = MatchScoreDisplay.runs2(match);
        if (s1 == s2) {
            return "Match tied";
        }
        String winner = s1 > s2 ? (match.team1 != null ? match.team1 : "Team 1") : (match.team2 != null ? match.team2 : "Team 2");
        return winner + " won";
    }

    @Override
    public int getItemCount() {
        return matches == null ? 0 : matches.size();
    }

    static class MatchViewHolder extends RecyclerView.ViewHolder {
        TextView txtTeam1, txtTeam2, txtScore1, txtScore2, txtVs, txtStatus, txtResult, txtFixtureMeta;
        View layoutScores;
        ImageButton btnDeleteMatch;

        MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            txtFixtureMeta = itemView.findViewById(R.id.txtFixtureMeta);
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

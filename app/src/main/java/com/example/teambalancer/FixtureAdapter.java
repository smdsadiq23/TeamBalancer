package com.example.teambalancer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FixtureAdapter extends RecyclerView.Adapter<FixtureAdapter.MatchViewHolder> {

    private List<Match> matches;
    private OnMatchActionListener listener;

    public interface OnMatchActionListener {
        void onEditMatch(Match match, int position);
        void onDeleteMatch(Match match, int position);
        void onViewMatch(Match match, int position);
    }

    public FixtureAdapter(List<Match> matches) {
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

        holder.txtTeam1.setText(match.team1);
        holder.txtTeam2.setText(match.team2);
        MatchDisplayHelper.bindFixtureMetaLine(holder.txtFixtureMeta, match);

        if (MatchCompletionHelper.isEffectivelyCompleted(match)) {
            holder.txtStatus.setVisibility(View.VISIBLE);
            holder.txtStatus.setText("COMPLETED");
            holder.txtStatus.setBackgroundResource(R.drawable.bg_stat_pill);
            
            holder.layoutScores.setVisibility(View.VISIBLE);
            holder.txtVs.setVisibility(View.GONE);
            
            holder.txtResult.setVisibility(View.VISIBLE);
            holder.txtResult.setText(getWinnerString(match));

            if ("Cricket".equalsIgnoreCase(match.sport)) {
                holder.txtScore1.setText(MatchScoreDisplay.cricketScoreWithOvers(match, true));
                holder.txtScore2.setText(MatchScoreDisplay.cricketScoreWithOvers(match, false));
            } else {
                holder.txtScore1.setText(String.valueOf(MatchScoreDisplay.runs1(match)));
                holder.txtScore2.setText(String.valueOf(MatchScoreDisplay.runs2(match)));
            }
            
            holder.btnDeleteMatch.setVisibility(View.VISIBLE);
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
            
            holder.btnDeleteMatch.setVisibility(View.VISIBLE);
        } else if (MatchCompletionHelper.isPreBallSetup(match)) {
            holder.txtStatus.setVisibility(View.VISIBLE);
            holder.txtStatus.setText("READY");
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

            holder.btnDeleteMatch.setVisibility(View.VISIBLE);
        } else {
            holder.txtStatus.setVisibility(View.VISIBLE);
            holder.txtStatus.setText("SCHEDULED");
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

            holder.btnDeleteMatch.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                if (MatchCompletionHelper.isEffectivelyCompleted(match)) {
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
        int s1 = MatchScoreDisplay.runs1(match);
        int s2 = MatchScoreDisplay.runs2(match);
        if (MatchCompletionHelper.isEffectivelyCompleted(match) && s1 == 0 && s2 == 0 && !match.hasFinalScoreSnapshot) {
            return "Completed";
        }
        if (s1 == s2) return "Match Tied";
        
        if (s1 > s2) {
            return match.team1 + " won";
        } else {
            return match.team2 + " won";
        }
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

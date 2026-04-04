package com.example.teambalancer;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import java.util.List;

public class TeamAdapter extends RecyclerView.Adapter<TeamAdapter.TeamViewHolder> {

    private final List<Team> teams;
    private OnTeamModifiedListener listener;
    private boolean isManagementMode = false;
    private int colorOnSurface = Integer.MIN_VALUE;
    private int colorAccent;

    public interface OnTeamModifiedListener {
        void onTeamModified();
        void onMovePlayer(Player player, Team fromTeam);
        default void onDeleteTeam(int position) {}
    }

    public TeamAdapter(List<Team> teams) {
        this.teams = teams;
    }

    public void setManagementMode(boolean managementMode) {
        this.isManagementMode = managementMode;
    }

    public void setOnTeamModifiedListener(OnTeamModifiedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public TeamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_team, parent, false);
        return new TeamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeamViewHolder holder, int position) {
        if (colorOnSurface == Integer.MIN_VALUE) {
            Context c = holder.itemView.getContext();
            colorOnSurface = ContextCompat.getColor(c, R.color.on_surface);
            colorAccent = ContextCompat.getColor(c, R.color.accent);
        }
        Team team = teams.get(position);
        holder.txtTeamName.setText(team.name);
        
        if (isManagementMode) {
            holder.btnDeleteTeam.setVisibility(View.VISIBLE);
            holder.btnDeleteTeam.setOnClickListener(v -> {
                int adapterPos = holder.getBindingAdapterPosition();
                if (listener != null && adapterPos != RecyclerView.NO_POSITION) {
                    listener.onDeleteTeam(adapterPos);
                }
            });
        } else {
            holder.btnDeleteTeam.setVisibility(View.GONE);
        }

        // Optimization: Recycle views instead of removing and inflating every time
        int currentPlayerCount = team.players.size();
        int existingViewCount = holder.layoutPlayers.getChildCount();

        boolean hasCaptain = team.players.stream().anyMatch(p -> p.isCaptain);
        LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());

        for (int i = 0; i < currentPlayerCount; i++) {
            Player player = team.players.get(i);
            View playerView;
            if (i < existingViewCount) {
                playerView = holder.layoutPlayers.getChildAt(i);
            } else {
                playerView = inflater.inflate(R.layout.item_team_player, holder.layoutPlayers, false);
                holder.layoutPlayers.addView(playerView);
            }
            playerView.setVisibility(View.VISIBLE);
            
            TextView txtName = playerView.findViewById(R.id.txtPlayerName);
            ImageButton btnMove = playerView.findViewById(R.id.btnMovePlayer);

            if (player.isCaptain) {
                txtName.setText("★ " + player.name);
                txtName.setTypeface(null, Typeface.BOLD);
                txtName.setTextColor(colorAccent);
            } else {
                txtName.setText(player.name);
                txtName.setTypeface(null, Typeface.NORMAL);
                txtName.setTextColor(colorOnSurface);
            }
            
            if (!isManagementMode && hasCaptain && !player.isCaptain) {
                btnMove.setVisibility(View.VISIBLE);
                btnMove.setOnClickListener(v -> {
                    if (listener != null) listener.onMovePlayer(player, team);
                });
            } else {
                btnMove.setVisibility(View.GONE);
            }
        }

        // Hide unused views if player count decreased
        for (int i = currentPlayerCount; i < existingViewCount; i++) {
            holder.layoutPlayers.getChildAt(i).setVisibility(View.GONE);
        }

        holder.chipStrength.setText("Strength " + team.totalStrength);
    }

    @Override
    public int getItemCount() {
        return teams.size();
    }

    static class TeamViewHolder extends RecyclerView.ViewHolder {
        TextView txtTeamName;
        LinearLayout layoutPlayers;
        Chip chipStrength;
        ImageButton btnDeleteTeam;

        public TeamViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTeamName = itemView.findViewById(R.id.txtTeamName);
            layoutPlayers = itemView.findViewById(R.id.layoutTeamPlayersContainer);
            chipStrength = itemView.findViewById(R.id.chipTeamStrength);
            btnDeleteTeam = itemView.findViewById(R.id.btnDeleteTeam);
        }
    }
}

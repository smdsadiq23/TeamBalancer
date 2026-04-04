package com.example.teambalancer;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.checkbox.MaterialCheckBox;
import java.util.ArrayList;
import java.util.List;

public class PlayerAdapter extends RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder> {

    private final List<Player> players;
    private final OnPlayerActionListener actionListener;
    private List<String> teamNames = new ArrayList<>();
    private boolean showTeamSelection = false;

    public interface OnPlayerActionListener {
        void onPlayerDelete(int position);
        void onPlayerAvailabilityChanged(int position, boolean isAvailable);
        void onPlayerCaptaincyChanged(int position, boolean isCaptain);
        void onPlayerEdit(int position, Player player);
        default void onTeamAssigned(Player player, String teamName) {}
    }

    public PlayerAdapter(List<Player> players, OnPlayerActionListener actionListener) {
        this.players = players;
        this.actionListener = actionListener;
    }

    public void updatePlayers(List<Player> newPlayers) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new PlayerDiffCallback(this.players, newPlayers));
        this.players.clear();
        this.players.addAll(newPlayers);
        diffResult.dispatchUpdatesTo(this);
    }

    public void setTeamNames(List<String> teamNames) {
        this.teamNames = teamNames;
        this.showTeamSelection = !teamNames.isEmpty();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_player_clean, parent, false);
        return new PlayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        Player player = players.get(position);
        holder.txtName.setText(player.name);
        String styleText = player.style.toString() + " (" + player.category.displayName + ")";
        holder.txtStyle.setText(styleText);
        
        // Captaincy Toggle
        holder.checkIsCaptain.setOnCheckedChangeListener(null);
        holder.checkIsCaptain.setChecked(player.isCaptain);
        
        holder.checkIsCaptain.setOnClickListener(v -> {
            boolean isChecked = holder.checkIsCaptain.isChecked();
            actionListener.onPlayerCaptaincyChanged(holder.getAdapterPosition(), isChecked);
        });
        
        // Team Selection for Captains
        if (showTeamSelection && player.isCaptain) {
            holder.txtAssignedTeam.setVisibility(View.VISIBLE);
            String team = player.assignedTeam != null ? player.assignedTeam : "Assign Team";
            holder.txtAssignedTeam.setText(team);
            holder.txtAssignedTeam.setOnClickListener(v -> {
                actionListener.onTeamAssigned(player, null);
            });
        } else {
            holder.txtAssignedTeam.setVisibility(View.GONE);
        }

        // Availability Toggle
        holder.checkAvailable.setOnCheckedChangeListener(null);
        holder.checkAvailable.setChecked(player.isAvailable);
        holder.checkAvailable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                actionListener.onPlayerAvailabilityChanged(pos, isChecked);
            }
        });

        holder.btnEdit.setOnClickListener(v -> actionListener.onPlayerEdit(holder.getAdapterPosition(), player));
        holder.btnDelete.setOnClickListener(v -> actionListener.onPlayerDelete(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    static class PlayerViewHolder extends RecyclerView.ViewHolder {
        View rootView;
        TextView txtName, txtStyle, txtAssignedTeam;
        MaterialCheckBox checkIsCaptain;
        ImageButton btnEdit;
        ImageButton btnDelete;
        MaterialCheckBox checkAvailable;

        public PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            rootView = itemView.findViewById(R.id.playerRowRoot);
            txtName = itemView.findViewById(R.id.txtPlayerName);
            checkIsCaptain = itemView.findViewById(R.id.checkIsCaptain);
            btnEdit = itemView.findViewById(R.id.btnInfoPlayer);
            txtStyle = itemView.findViewById(R.id.txtPlayerStyle);
            btnDelete = itemView.findViewById(R.id.btnDeletePlayer);
            checkAvailable = itemView.findViewById(R.id.checkAvailable);
            txtAssignedTeam = itemView.findViewById(R.id.txtAssignedTeam);
        }
    }

    private static class PlayerDiffCallback extends DiffUtil.Callback {
        private final List<Player> oldList;
        private final List<Player> newList;

        public PlayerDiffCallback(List<Player> oldList, List<Player> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).id == newList.get(newItemPosition).id;
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Player oldPlayer = oldList.get(oldItemPosition);
            Player newPlayer = newList.get(newItemPosition);
            return oldPlayer.name.equals(newPlayer.name) &&
                    oldPlayer.isCaptain == newPlayer.isCaptain &&
                    oldPlayer.isAvailable == newPlayer.isAvailable &&
                    oldPlayer.style == newPlayer.style &&
                    oldPlayer.category == newPlayer.category &&
                    ((oldPlayer.assignedTeam == null && newPlayer.assignedTeam == null) ||
                     (oldPlayer.assignedTeam != null && oldPlayer.assignedTeam.equals(newPlayer.assignedTeam)));
        }
    }
}

package com.example.teambalancer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.checkbox.MaterialCheckBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlayerAdapter extends ListAdapter<Player, PlayerAdapter.PlayerViewHolder> {

    private final OnPlayerActionListener actionListener;
    private List<String> teamNames = new ArrayList<>();
    private boolean showTeamSelection = false;

    public interface OnPlayerActionListener {
        void onPlayerDelete(Player player);
        void onPlayerAvailabilityChanged(Player player, boolean isAvailable);
        void onPlayerCaptaincyChanged(Player player, boolean isCaptain);
        void onPlayerEdit(Player player);
        default void onTeamAssigned(Player player, String teamName) {}
    }

    public PlayerAdapter(OnPlayerActionListener actionListener) {
        super(new PlayerDiffCallback());
        this.actionListener = actionListener;
    }

    public void updatePlayers(List<Player> newPlayers) {
        submitList(newPlayers != null ? new ArrayList<>(newPlayers) : null);
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
        Player player = getItem(position);
        holder.txtName.setText(player.name);
        String styleText = (player.style != null ? player.style.toString() : "Batsman") + 
                " (" + (player.category != null ? player.category.displayName : "Regular") + ")";
        holder.txtStyle.setText(styleText);
        
        // Captaincy Toggle
        holder.checkIsCaptain.setOnCheckedChangeListener(null);
        holder.checkIsCaptain.setChecked(player.isCaptain);
        
        holder.checkIsCaptain.setOnClickListener(v -> {
            boolean isChecked = holder.checkIsCaptain.isChecked();
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                actionListener.onPlayerCaptaincyChanged(getItem(pos), isChecked);
            }
        });
        
        // Team Selection for Captains
        if (showTeamSelection && player.isCaptain) {
            holder.txtAssignedTeam.setVisibility(View.VISIBLE);
            String team = player.assignedTeam != null ? player.assignedTeam : "Assign Team";
            holder.txtAssignedTeam.setText(team);
            holder.txtAssignedTeam.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    actionListener.onTeamAssigned(getItem(pos), null);
                }
            });
        } else {
            holder.txtAssignedTeam.setVisibility(View.GONE);
        }

        // Availability Toggle
        holder.checkAvailable.setOnCheckedChangeListener(null);
        holder.checkAvailable.setChecked(player.isAvailable);
        holder.checkAvailable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                actionListener.onPlayerAvailabilityChanged(getItem(pos), isChecked);
            }
        });

        holder.btnEdit.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                actionListener.onPlayerEdit(getItem(pos));
            }
        });
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                actionListener.onPlayerDelete(getItem(pos));
            }
        });
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

    private static class PlayerDiffCallback extends DiffUtil.ItemCallback<Player> {
        @Override
        public boolean areItemsTheSame(@NonNull Player oldItem, @NonNull Player newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Player oldItem, @NonNull Player newItem) {
            return Objects.equals(oldItem.name, newItem.name) &&
                    oldItem.isCaptain == newItem.isCaptain &&
                    oldItem.isAvailable == newItem.isAvailable &&
                    oldItem.style == newItem.style &&
                    oldItem.category == newItem.category &&
                    Objects.equals(oldItem.assignedTeam, newItem.assignedTeam);
        }
    }
}

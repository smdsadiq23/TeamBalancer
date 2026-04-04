package com.example.teambalancer;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class ClubAdapter extends RecyclerView.Adapter<ClubAdapter.ClubViewHolder> {

    private final List<ClubWithPlayers> clubs;
    private final OnClubActionListener listener;
    private String selectedClubName = "";
    private final int accentColor;
    private final int dividerColor;
    private final int greenColor;

    public interface OnClubActionListener {
        void onClubSelect(Club club);
        void onClubDelete(int position);
        void onShowPlayers(List<Player> players, String clubName);
    }

    public ClubAdapter(List<ClubWithPlayers> clubs, OnClubActionListener listener, Context context) {
        this.clubs = clubs;
        this.listener = listener;
        accentColor = ContextCompat.getColor(context, R.color.accent);
        dividerColor = ContextCompat.getColor(context, R.color.divider);
        greenColor = ContextCompat.getColor(context, R.color.cricket_green);
    }

    public void setSelectedClubName(String selectedClubName) {
        this.selectedClubName = selectedClubName;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ClubViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_club, parent, false);
        return new ClubViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClubViewHolder holder, int position) {
        ClubWithPlayers clubWithPlayers = clubs.get(position);
        Club club = clubWithPlayers.club;
        holder.txtName.setText(club.name);
        
        int playerCount = clubWithPlayers.players != null ? clubWithPlayers.players.size() : 0;
        int teamCount = club.teamNames != null ? club.teamNames.size() : 0;
        
        String playerPart = playerCount + " Players";
        String teamPart = teamCount + (teamCount == 1 ? " Team" : " Teams");
        String fullText = playerPart + " | " + teamPart;

        SpannableStringBuilder ssb = new SpannableStringBuilder(fullText);

        ssb.setSpan(new ForegroundColorSpan(greenColor), 0, playerPart.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, playerPart.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        holder.txtDetails.setText(ssb);

        // Click listener for player list popup
        holder.txtDetails.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShowPlayers(clubWithPlayers.players, club.name);
            }
        });

        boolean isSelected = club.name.equals(selectedClubName);
        
        if (isSelected) {
            holder.cardRoot.setStrokeColor(accentColor);
            holder.cardRoot.setStrokeWidth(4);
            holder.btnSelect.setText("SELECTED");
            holder.btnSelect.setEnabled(false);
            holder.btnSelect.setAlpha(0.6f);
        } else {
            holder.cardRoot.setStrokeColor(dividerColor);
            holder.cardRoot.setStrokeWidth(2);
            holder.btnSelect.setText("SELECT");
            holder.btnSelect.setEnabled(true);
            holder.btnSelect.setAlpha(1.0f);
        }
        
        holder.btnSelect.setOnClickListener(v -> listener.onClubSelect(club));
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onClubDelete(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return clubs.size();
    }

    static class ClubViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        TextView txtName, txtDetails;
        MaterialButton btnSelect, btnDelete;

        public ClubViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = (MaterialCardView) itemView;
            txtName = itemView.findViewById(R.id.txtClubName);
            txtDetails = itemView.findViewById(R.id.txtClubDetails);
            btnSelect = itemView.findViewById(R.id.btnSelectClub);
            btnDelete = itemView.findViewById(R.id.btnDeleteClub);
        }
    }
}

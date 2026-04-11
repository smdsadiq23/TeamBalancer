package com.example.teambalancer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class BattingAdapter extends RecyclerView.Adapter<BattingAdapter.VH> {

    private final List<BattingStats> items = new ArrayList<>();

    public void submit(List<BattingStats> rows) {
        items.clear();
        if (rows != null) {
            items.addAll(rows);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_batting, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        BattingStats b = items.get(position);
        holder.txtPlayer.setText(b.name);
        holder.txtRuns.setText(String.valueOf(b.runs));
        holder.txtBalls.setText(String.valueOf(b.balls));
        holder.txtFours.setText(String.valueOf(b.fours));
        holder.txtSixes.setText(String.valueOf(b.sixes));
        holder.txtSr.setText(b.strikeRateText());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView txtPlayer, txtRuns, txtBalls, txtFours, txtSixes, txtSr;

        VH(@NonNull View itemView) {
            super(itemView);
            txtPlayer = itemView.findViewById(R.id.txtPlayer);
            txtRuns = itemView.findViewById(R.id.txtRuns);
            txtBalls = itemView.findViewById(R.id.txtBalls);
            txtFours = itemView.findViewById(R.id.txtFours);
            txtSixes = itemView.findViewById(R.id.txtSixes);
            txtSr = itemView.findViewById(R.id.txtSr);
        }
    }
}

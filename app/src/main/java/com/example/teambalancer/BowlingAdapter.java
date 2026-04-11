package com.example.teambalancer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class BowlingAdapter extends RecyclerView.Adapter<BowlingAdapter.VH> {

    private final List<BowlingStats> items = new ArrayList<>();

    public void submit(List<BowlingStats> rows) {
        items.clear();
        if (rows != null) {
            items.addAll(rows);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bowling, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        BowlingStats b = items.get(position);
        holder.txtBowler.setText(b.name);
        holder.txtOvers.setText(b.oversText());
        holder.txtRuns.setText(String.valueOf(b.runsConceded));
        holder.txtWkts.setText(String.valueOf(b.wickets));
        holder.txtEcon.setText(b.economyText());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView txtBowler, txtOvers, txtRuns, txtWkts, txtEcon;

        VH(@NonNull View itemView) {
            super(itemView);
            txtBowler = itemView.findViewById(R.id.txtBowler);
            txtOvers = itemView.findViewById(R.id.txtOvers);
            txtRuns = itemView.findViewById(R.id.txtRuns);
            txtWkts = itemView.findViewById(R.id.txtWkts);
            txtEcon = itemView.findViewById(R.id.txtEcon);
        }
    }
}

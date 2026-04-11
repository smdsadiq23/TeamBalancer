package com.example.teambalancer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.VH> {

    private final List<String> labels = new ArrayList<>();

    public void submit(List<String> balls) {
        labels.clear();
        if (balls != null) {
            labels.addAll(balls);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ball, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String label = labels.get(position);
        holder.txt.setText(label);
        boolean wicket = "W".equals(label);
        int color = ContextCompat.getColor(
                holder.itemView.getContext(),
                wicket ? R.color.cricket_red : R.color.neon_blue);
        holder.txt.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return labels.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView txt;

        VH(@NonNull View itemView) {
            super(itemView);
            txt = itemView.findViewById(R.id.txtBall);
        }
    }
}

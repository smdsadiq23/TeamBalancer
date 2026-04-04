package com.example.teambalancer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class SimpleListAdapter extends RecyclerView.Adapter<SimpleListAdapter.SimpleViewHolder> {

    private List<String> items;
    private final OnItemActionListener listener;
    private int selectedPosition = -1;

    public interface OnItemActionListener {
        void onItemDelete(int position);
        default void onItemSelect(int position) {}
    }

    public SimpleListAdapter(List<String> items, OnItemActionListener listener) {
        this.items = items != null ? items : new ArrayList<>();
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_simple_list_with_delete, parent, false);
        return new SimpleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position) {
        holder.txtName.setText(items.get(position));
        
        boolean isSelected = (position == selectedPosition);
        
        if (isSelected) {
            holder.cardRoot.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent));
            holder.cardRoot.setStrokeWidth(4);
        } else {
            holder.cardRoot.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.divider));
            holder.cardRoot.setStrokeWidth(2);
        }

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && listener != null) {
                listener.onItemSelect(pos);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                listener.onItemDelete(pos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class SimpleViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        TextView txtName;
        MaterialButton btnDelete;

        public SimpleViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = (MaterialCardView) itemView;
            txtName = itemView.findViewById(R.id.txtItemName);
            btnDelete = itemView.findViewById(R.id.btnDeleteItem);
        }
    }
}

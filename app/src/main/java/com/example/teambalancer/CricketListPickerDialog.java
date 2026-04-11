package com.example.teambalancer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Styled list picker (avatars, chevrons, subtitle, cancel) for striker / bowler / wicket type.
 */
public final class CricketListPickerDialog {

    public interface OnPickListener {
        void onPicked(String value);
    }

    private CricketListPickerDialog() {}

    public static void show(
            AppCompatActivity activity,
            String title,
            @Nullable String subtitle,
            java.util.List<String> items,
            OnPickListener listener) {
        if (items == null || items.isEmpty()) {
            return;
        }

        View root = activity.getLayoutInflater().inflate(R.layout.dialog_player_pick, null);
        TextView titleTv = root.findViewById(R.id.txtPlayerPickTitle);
        TextView subTv = root.findViewById(R.id.txtPlayerPickSubtitle);
        LinearLayout list = root.findViewById(R.id.layoutPlayerPickList);

        titleTv.setText(title);
        if (subtitle != null && !subtitle.isEmpty()) {
            subTv.setText(subtitle);
            subTv.setVisibility(View.VISIBLE);
        } else {
            subTv.setVisibility(View.GONE);
        }

        LayoutInflater inf = activity.getLayoutInflater();
        list.removeAllViews();

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(activity, R.style.ThemeOverlay_TeamBalancer_Dialog)
                        .setView(root)
                        .setNegativeButton(android.R.string.cancel, (d, w) -> d.dismiss())
                        .create();

        for (String name : items) {
            View row = inf.inflate(R.layout.item_player_pick, list, false);
            TextView nameTv = row.findViewById(R.id.txtPlayerPickName);
            nameTv.setText(name);
            row.setOnClickListener(
                    v -> {
                        dialog.dismiss();
                        if (listener != null) {
                            listener.onPicked(name);
                        }
                    });
            list.addView(row);
        }

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog
                    .getWindow()
                    .setLayout(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }
}

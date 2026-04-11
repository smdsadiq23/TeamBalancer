package com.example.teambalancer;

import android.view.View;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Shared match row labels (venue, schedule) for fixture-style lists.
 */
public final class MatchDisplayHelper {

    private MatchDisplayHelper() {}

    public static void bindFixtureMetaLine(TextView textView, Match match) {
        if (textView == null) {
            return;
        }
        String line = buildFixtureMetaLine(match);
        if (line.isEmpty()) {
            textView.setVisibility(View.GONE);
        } else {
            textView.setVisibility(View.VISIBLE);
            textView.setText(line);
        }
    }

    public static String buildFixtureMetaLine(Match match) {
        if (match == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (match.venue != null && !match.venue.trim().isEmpty()) {
            parts.add(match.venue.trim());
        }
        if (match.scheduledStartMillis > 0L) {
            DateFormat df = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
            parts.add(df.format(new Date(match.scheduledStartMillis)));
        }
        return String.join(" · ", parts);
    }
}

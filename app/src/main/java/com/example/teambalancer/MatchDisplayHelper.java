package com.example.teambalancer;

import android.text.TextUtils;
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

    private static DateFormat cachedDateTimeFormat;

    private MatchDisplayHelper() {}

    private static DateFormat getFormat() {
        if (cachedDateTimeFormat == null) {
            cachedDateTimeFormat = DateFormat.getDateTimeInstance(
                    DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault());
        }
        return cachedDateTimeFormat;
    }

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
            parts.add(getFormat().format(new Date(match.scheduledStartMillis)));
        }
        return TextUtils.join(" · ", parts);
    }
}

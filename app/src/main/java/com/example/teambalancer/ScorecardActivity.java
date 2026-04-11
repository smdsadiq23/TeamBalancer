package com.example.teambalancer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.teambalancer.databinding.ScorecardActivityBinding;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Read-only full scorecard: header, batting, bowling, extras, ball-by-ball timeline.
 */
public class ScorecardActivity extends AppCompatActivity {

    public static final String EXTRA_MATCH = "extra_match";

    public static void start(Context context, Match match) {
        Intent i = new Intent(context, ScorecardActivity.class);
        i.putExtra(EXTRA_MATCH, match);
        context.startActivity(i);
    }

    private ScorecardActivityBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ScorecardActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Match match = (Match) getIntent().getSerializableExtra(EXTRA_MATCH);
        if (match == null) {
            finish();
            return;
        }

        MatchPersistenceHelper.ensureListsNotNull(match);
        MatchPersistenceHelper.restoreListsFromJson(match);
        CricketTotalsRecomputer.recomputeFromStoredEvents(match);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        String t1 = match.team1 != null ? match.team1 : "?";
        String t2 = match.team2 != null ? match.team2 : "?";
        binding.txtTeams.setText(t1 + "  vs  " + t2);

        if (MatchCompletionHelper.isEffectivelyCompleted(match)) {
            binding.txtResult.setText(CricketMatchResultFormatter.formatResult(match));
        } else {
            binding.txtResult.setText(R.string.scorecard_in_progress);
        }

        binding.txtScoresLine.setText(buildScoresLine(match));

        List<BattingStats> batting = CricketScorecardStats.buildBatting(match);
        Collections.sort(batting, Comparator.comparingInt((BattingStats b) -> b.runs).reversed());
        BattingAdapter batAd = new BattingAdapter();
        batAd.submit(batting);
        binding.recyclerBatting.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerBatting.setAdapter(batAd);

        List<BowlingStats> bowling = CricketScorecardStats.buildBowling(match);
        Collections.sort(
                bowling,
                Comparator.comparingInt((BowlingStats b) -> b.wickets)
                        .reversed()
                        .thenComparingDouble(b -> b.economy()));
        BowlingAdapter bowlAd = new BowlingAdapter();
        bowlAd.submit(bowling);
        binding.recyclerBowling.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerBowling.setAdapter(bowlAd);

        binding.txtExtras.setText(CricketScorecardStats.buildExtrasLine(match));

        List<String> timeline = CricketScorecardStats.buildTimelineLabels(match);
        TimelineAdapter timeAd = new TimelineAdapter();
        timeAd.submit(timeline);
        LinearLayoutManager horiz = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.recyclerTimeline.setLayoutManager(horiz);
        binding.recyclerTimeline.setAdapter(timeAd);
    }

    private static String buildScoresLine(Match match) {
        int s1 = MatchScoreDisplay.runs1(match);
        int s2 = MatchScoreDisplay.runs2(match);
        int w1 = MatchScoreDisplay.wickets1(match);
        int w2 = MatchScoreDisplay.wickets2(match);
        double o1 = MatchScoreDisplay.overs1(match);
        double o2 = MatchScoreDisplay.overs2(match);
        String team1 = match.team1 != null ? match.team1 : "?";
        String team2 = match.team2 != null ? match.team2 : "?";
        return String.format(
                Locale.getDefault(),
                "%s %d/%d (%.1f ov)   ·   %s %d/%d (%.1f ov)",
                team1,
                s1,
                w1,
                o1,
                team2,
                s2,
                w2,
                o2);
    }
}

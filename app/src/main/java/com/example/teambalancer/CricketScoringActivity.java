package com.example.teambalancer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Full-screen cricket scoring. Pass {@link #EXTRA_MATCH} and {@link #EXTRA_CLUB_NAME}; finishes with
 * {@link #EXTRA_UPDATED_MATCH} so fragments can reload from Room.
 */
public class CricketScoringActivity extends AppCompatActivity {

    public static final String EXTRA_MATCH = "extra_match";
    public static final String EXTRA_CLUB_NAME = "extra_club_name";
    public static final String EXTRA_UPDATED_MATCH = "extra_updated_match";

    private Match match;
    private String clubName;
    private DataManager dataManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        match = (Match) getIntent().getSerializableExtra(EXTRA_MATCH);
        clubName = getIntent().getStringExtra(EXTRA_CLUB_NAME);
        dataManager = new DataManager(this);
        if (clubName == null) {
            clubName = dataManager.getCurrentClubName();
        }

        if (match == null) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        if (MatchCompletionHelper.isEffectivelyCompleted(match)) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        setContentView(R.layout.activity_cricket_scoring);

        TabLayout tabScoring = findViewById(R.id.tabScoring);
        ScrollView scrollPageScore = findViewById(R.id.scrollPageScore);
        ScrollView scrollPageScoreboard = findViewById(R.id.scrollPageScoreboard);
        tabScoring.addTab(tabScoring.newTab().setText(R.string.scoring_tab_live));
        tabScoring.addTab(tabScoring.newTab().setText(R.string.scoring_tab_scoreboard));
        tabScoring.addOnTabSelectedListener(
                new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        boolean live = tab.getPosition() == 0;
                        scrollPageScore.setVisibility(live ? View.VISIBLE : View.GONE);
                        scrollPageScoreboard.setVisibility(live ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {}

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {}
                });

        TextView txtFullBatting = findViewById(R.id.txtFullScoreboardBatting);
        TextView txtFullBowling = findViewById(R.id.txtFullScoreboardBowling);
        TextView txtFullExtras = findViewById(R.id.txtFullScoreboardExtras);
        TextView txtFullTimeline = findViewById(R.id.txtFullScoreboardTimeline);
        TextView txtFullSquads = findViewById(R.id.txtFullScoreboardSquads);

        TabLayout tabFullScoreboardSub = findViewById(R.id.tabFullScoreboardSub);
        View layoutFullScoreboardScorecard = findViewById(R.id.layoutFullScoreboardScorecard);
        View layoutFullScoreboardLog = findViewById(R.id.layoutFullScoreboardLog);
        if (tabFullScoreboardSub != null
                && layoutFullScoreboardScorecard != null
                && layoutFullScoreboardLog != null) {
            tabFullScoreboardSub.addTab(
                    tabFullScoreboardSub.newTab().setText(R.string.scoring_subtab_scorecard));
            tabFullScoreboardSub.addTab(
                    tabFullScoreboardSub.newTab().setText(R.string.scoring_subtab_log));
            tabFullScoreboardSub.addOnTabSelectedListener(
                    new TabLayout.OnTabSelectedListener() {
                        @Override
                        public void onTabSelected(TabLayout.Tab tab) {
                            boolean scorecard = tab.getPosition() == 0;
                            layoutFullScoreboardScorecard.setVisibility(
                                    scorecard ? View.VISIBLE : View.GONE);
                            layoutFullScoreboardLog.setVisibility(
                                    scorecard ? View.GONE : View.VISIBLE);
                        }

                        @Override
                        public void onTabUnselected(TabLayout.Tab tab) {}

                        @Override
                        public void onTabReselected(TabLayout.Tab tab) {}
                    });
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbarScoring);
        toolbar.setNavigationOnClickListener(v -> finishWithOk());

        getOnBackPressedDispatcher()
                .addCallback(
                        this,
                        new OnBackPressedCallback(true) {
                            @Override
                            public void handleOnBackPressed() {
                                finishWithOk();
                            }
                        });

        TextView txtMatchInfo = findViewById(R.id.txtMatchInfo);
        TextView txtLiveScore = findViewById(R.id.txtLiveScore);
        TextView txtLiveOvers = findViewById(R.id.txtLiveOvers);
        MaterialButton btnFinish = findViewById(R.id.btnFinish);
        View layoutControls = findViewById(R.id.layoutScoringControls);
        View btnUndo = findViewById(R.id.btnUndo);

        TextView txtStriker = findViewById(R.id.txtStriker);
        TextView txtStrikerStats = findViewById(R.id.txtStrikerStats);
        TextView txtNonStriker = findViewById(R.id.txtNonStriker);
        TextView txtNonStrikerStats = findViewById(R.id.txtNonStrikerStats);
        TextView txtBowler = findViewById(R.id.txtBowler);
        TextView txtBowlerStats = findViewById(R.id.txtBowlerStats);

        Runnable updateUI =
                () -> {
                    if (match.battingTeam == null) {
                        return;
                    }
                    int runs =
                            (match.battingTeam.equals(match.team1))
                                    ? MatchScoreDisplay.runs1(match)
                                    : MatchScoreDisplay.runs2(match);
                    int wkts =
                            (match.battingTeam.equals(match.team1))
                                    ? MatchScoreDisplay.wickets1(match)
                                    : MatchScoreDisplay.wickets2(match);
                    double overs =
                            (match.battingTeam.equals(match.team1))
                                    ? MatchScoreDisplay.overs1(match)
                                    : MatchScoreDisplay.overs2(match);

                    if (MatchCompletionHelper.isEffectivelyCompleted(match)) {
                        int s1 = MatchScoreDisplay.runs1(match);
                        int s2 = MatchScoreDisplay.runs2(match);
                        int w1 = MatchScoreDisplay.wickets1(match);
                        int w2 = MatchScoreDisplay.wickets2(match);
                        double o1 = MatchScoreDisplay.overs1(match);
                        double o2 = MatchScoreDisplay.overs2(match);
                        txtLiveScore.setText(s1 + "/" + w1 + "  vs  " + s2 + "/" + w2);
                        txtLiveOvers.setText(
                                String.format(
                                        Locale.getDefault(),
                                        "(%.1f ov · %.1f ov · %d max)",
                                        o1,
                                        o2,
                                        match.maxOvers));
                    } else {
                        txtLiveScore.setText(runs + "/" + wkts);
                        txtLiveOvers.setText(
                                String.format(Locale.getDefault(), "(%.1f / %d)", overs, match.maxOvers));
                    }

                    if (MatchCompletionHelper.isEffectivelyCompleted(match)) {
                        btnFinish.setText("CLOSE SCORECARD");
                        if (layoutControls != null) {
                            layoutControls.setVisibility(View.GONE);
                        }
                        if (btnUndo != null) {
                            btnUndo.setVisibility(View.GONE);
                        }
                        txtMatchInfo.setText(CricketMatchResultFormatter.formatResult(match));
                    } else {
                        txtMatchInfo.setText(
                                match.battingTeam
                                        + " innings"
                                        + (match.currentInnings == 2
                                                ? " (Target: "
                                                        + ((match.battingTeam.equals(match.team1)
                                                                        ? MatchScoreDisplay.runs2(match)
                                                                        : MatchScoreDisplay.runs1(match))
                                                                + 1)
                                                        + ")"
                                                : ""));

                        addRRRDisplay(match, txtMatchInfo);

                        if (match.currentInnings == 1 && !"Test".equals(match.matchType)) {
                            btnFinish.setText("START 2ND INNINGS");
                        } else {
                            btnFinish.setText("FINISH MATCH");
                        }
                    }

                    updatePlayerSection(
                            match,
                            txtStriker,
                            txtStrikerStats,
                            txtNonStriker,
                            txtNonStrikerStats,
                            txtBowler,
                            txtBowlerStats);

                    if (txtFullBatting != null) {
                        txtFullBatting.setText(CricketFullScoreboardHelper.formatBatting(match));
                    }
                    if (txtFullBowling != null) {
                        txtFullBowling.setText(CricketFullScoreboardHelper.formatBowling(match));
                    }
                    if (txtFullExtras != null) {
                        txtFullExtras.setText(CricketFullScoreboardHelper.formatExtras(match));
                    }
                    if (txtFullTimeline != null) {
                        txtFullTimeline.setText(CricketFullScoreboardHelper.formatTimeline(match));
                    }
                    if (txtFullSquads != null) {
                        txtFullSquads.setText(CricketFullScoreboardHelper.formatSquads(match));
                    }
                };

        if (!MatchCompletionHelper.isEffectivelyCompleted(match)) {
            txtStriker.setOnClickListener(
                    v ->
                            promptPlayerSelection(
                                    match,
                                    "Select Striker",
                                    true,
                                    name -> {
                                        match.striker = name;
                                        updateUI.run();
                                        persist();
                                    }));
            txtNonStriker.setOnClickListener(
                    v ->
                            promptPlayerSelection(
                                    match,
                                    "Select Non-Striker",
                                    true,
                                    name -> {
                                        match.nonStriker = name;
                                        updateUI.run();
                                        persist();
                                    }));
            txtBowler.setOnClickListener(
                    v ->
                            promptPlayerSelection(
                                    match,
                                    "Select Bowler",
                                    false,
                                    name -> {
                                        match.currentBowler = name;
                                        updateUI.run();
                                        persist();
                                    }));
        }

        updateUI.run();

        View.OnClickListener scoringListener =
                v -> {
                    if (MatchCompletionHelper.isEffectivelyCompleted(match)) {
                        return;
                    }

                    if (match.striker == null
                            || match.nonStriker == null
                            || match.currentBowler == null) {
                        checkAndPromptInitialPlayers(match, updateUI);
                        return;
                    }

                    double currentOvers =
                            (match.battingTeam.equals(match.team1)) ? match.overs1 : match.overs2;
                    int currentWickets =
                            (match.battingTeam.equals(match.team1)) ? match.wickets1 : match.wickets2;
                    int maxWickets = MatchCompletionHelper.getMaxWickets(match);

                    if (currentOvers >= match.maxOvers || currentWickets >= maxWickets) {
                        Toast.makeText(this, "Innings over!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (match.maxOversPerBowler > 0
                            && match.currentBowler != null
                            && BowlingQuotaHelper.isAtOrOverQuota(match, match.currentBowler)) {
                        Toast.makeText(
                                        this,
                                        "This bowler has bowled their maximum overs for this innings.",
                                        Toast.LENGTH_SHORT)
                                .show();
                        match.currentBowler = null;
                        checkAndPromptInitialPlayers(match, updateUI);
                        return;
                    }

                    BallEvent event = new BallEvent();
                    event.striker = match.striker;
                    event.nonStriker = match.nonStriker;
                    event.bowler = match.currentBowler;

                    if (v.getId() == R.id.btn0) {
                        event.runs = 0;
                    } else if (v.getId() == R.id.btn1) {
                        event.runs = 1;
                    } else if (v.getId() == R.id.btn2) {
                        event.runs = 2;
                    } else if (v.getId() == R.id.btn3) {
                        event.runs = 3;
                    } else if (v.getId() == R.id.btn4) {
                        event.runs = 4;
                    } else if (v.getId() == R.id.btn6) {
                        event.runs = 6;
                    } else if (v.getId() == R.id.btnWide) {
                        event.runs = 1;
                        event.extraType = BallEvent.ExtraType.WIDE;
                        event.isLegalBall = false;
                    } else if (v.getId() == R.id.btnNB) {
                        event.runs = 1;
                        event.extraType = BallEvent.ExtraType.NO_BALL;
                        event.isLegalBall = false;
                    } else if (v.getId() == R.id.btnBye) {
                        event.runs = 1;
                        event.extraType = BallEvent.ExtraType.BYE;
                        event.isLegalBall = true;
                    } else if (v.getId() == R.id.btnLB) {
                        event.runs = 1;
                        event.extraType = BallEvent.ExtraType.LEG_BYE;
                        event.isLegalBall = true;
                    } else if (v.getId() == R.id.btnWicket) {
                        if (match.maxOversPerBowler > 0
                                && match.currentBowler != null
                                && BowlingQuotaHelper.isAtOrOverQuota(match, match.currentBowler)) {
                            Toast.makeText(
                                            this,
                                            "This bowler has bowled their maximum overs for this innings.",
                                            Toast.LENGTH_SHORT)
                                    .show();
                            match.currentBowler = null;
                            checkAndPromptInitialPlayers(match, updateUI);
                            return;
                        }
                        if (match.maxOversPerBowler > 0
                                && match.currentBowler != null
                                && BowlingQuotaHelper.wouldExceedQuotaAfterLegalBall(
                                        match, match.currentBowler)) {
                            Toast.makeText(
                                            this,
                                            "This delivery would exceed the bowler's over limit for this innings.",
                                            Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }
                        showWicketTypeDialog(match, updateUI);
                        return;
                    }

                    if (match.maxOversPerBowler > 0
                            && event.isLegalBall
                            && match.currentBowler != null
                            && BowlingQuotaHelper.wouldExceedQuotaAfterLegalBall(
                                    match, match.currentBowler)) {
                        Toast.makeText(
                                        this,
                                        "This delivery would exceed the bowler's over limit for this innings.",
                                        Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }

                    CricketScoringHelper.processBallAndUpdateRotation(match, event);
                    CricketScoringHelper.checkMatchStatus(match);
                    updateUI.run();
                    persist();

                    if (!MatchCompletionHelper.isEffectivelyCompleted(match)) {
                        checkInningsOverAndBowlerChange(match, updateUI);
                    }
                };

        int[] ids = {
            R.id.btn0,
            R.id.btn1,
            R.id.btn2,
            R.id.btn3,
            R.id.btn4,
            R.id.btn6,
            R.id.btnWide,
            R.id.btnNB,
            R.id.btnBye,
            R.id.btnLB,
            R.id.btnWicket
        };
        for (int id : ids) {
            View b = findViewById(id);
            if (b != null) {
                b.setOnClickListener(scoringListener);
            }
        }

        if (btnUndo != null) {
            btnUndo.setOnClickListener(
                    v -> {
                        if (MatchCompletionHelper.isEffectivelyCompleted(match)) {
                            return;
                        }
                        if (match.ballHistory != null && !match.ballHistory.isEmpty()) {
                            BallEvent last =
                                    match.ballHistory.remove(match.ballHistory.size() - 1);
                            CricketScoringHelper.undoBall(match, last);
                            MatchPersistenceHelper.syncJsonFromLists(match);
                            updateUI.run();
                            persist();
                        }
                    });
        }

        btnFinish.setOnClickListener(
                v -> {
                    if (MatchCompletionHelper.isEffectivelyCompleted(match)) {
                        finishWithOk();
                        return;
                    }

                    if (match.currentInnings == 1 && !"Test".equals(match.matchType)) {
                        if (match.ballHistory == null || match.ballHistory.isEmpty()) {
                            Toast.makeText(
                                            this,
                                            "Record at least one ball before ending the first innings.",
                                            Toast.LENGTH_SHORT)
                                    .show();
                            return;
                        }
                        match.inningsTwoFirstBallIndex = match.ballHistory.size();
                        match.currentInnings = 2;
                        String temp = match.battingTeam;
                        match.battingTeam = match.bowlingTeam;
                        match.bowlingTeam = temp;
                        match.striker = null;
                        match.nonStriker = null;
                        match.currentBowler = null;
                        checkAndPromptInitialPlayers(match, updateUI);
                        updateUI.run();
                        MatchPersistenceHelper.syncJsonFromLists(match);
                        persist();
                    } else {
                        match.hasStarted = true;
                        MatchCompletionHelper.markMatchCompleted(match);
                        MatchPersistenceHelper.syncJsonFromLists(match);
                        persist();
                        updateUI.run();
                        finishWithOk();
                    }
                });

        if (!MatchCompletionHelper.isEffectivelyCompleted(match)) {
            checkAndPromptInitialPlayers(match, updateUI);
        }
    }

    private void persist() {
        dataManager.replaceMatchInClubAndSave(clubName, match);
    }

    private void finishWithOk() {
        MatchPersistenceHelper.syncJsonFromLists(match);
        Intent data = new Intent();
        data.putExtra(EXTRA_UPDATED_MATCH, match);
        setResult(Activity.RESULT_OK, data);
        finish();
    }

    private void updatePlayerSection(
            Match m,
            TextView txtStr,
            TextView txtStrStats,
            TextView txtNonStr,
            TextView txtNonStrStats,
            TextView txtBowler,
            TextView txtBowlerStats) {
        boolean done = MatchCompletionHelper.isEffectivelyCompleted(m);
        if (m.striker != null) {
            txtStr.setText(m.striker + (done ? "" : " ⭐"));
            txtStrStats.setText(CricketScoringHelper.getPlayerBattingStats(m, m.striker));
        } else {
            txtStr.setText(done ? "" : "Select Striker");
            txtStrStats.setText("-");
        }

        if (m.nonStriker != null) {
            txtNonStr.setText(m.nonStriker);
            txtNonStrStats.setText(CricketScoringHelper.getPlayerBattingStats(m, m.nonStriker));
        } else {
            txtNonStr.setText(done ? "" : "Select Non-Striker");
            txtNonStrStats.setText("-");
        }

        if (m.currentBowler != null) {
            txtBowler.setText(m.currentBowler);
            txtBowlerStats.setText(CricketScoringHelper.getPlayerBowlingStats(m, m.currentBowler));
        } else {
            txtBowler.setText(done ? "" : "Select Bowler");
            txtBowlerStats.setText("-");
        }
    }

    private void checkAndPromptInitialPlayers(Match m, Runnable updateUI) {
        if (m.striker == null) {
            promptPlayerSelection(
                    m,
                    "Select Striker",
                    true,
                    name -> {
                        m.striker = name;
                        checkAndPromptInitialPlayers(m, updateUI);
                    });
        } else if (m.nonStriker == null) {
            promptPlayerSelection(
                    m,
                    "Select Non-Striker",
                    true,
                    name -> {
                        m.nonStriker = name;
                        checkAndPromptInitialPlayers(m, updateUI);
                    });
        } else if (m.currentBowler == null
                || (m.maxOversPerBowler > 0
                        && BowlingQuotaHelper.isAtOrOverQuota(m, m.currentBowler))) {
            if (m.currentBowler != null && m.maxOversPerBowler > 0) {
                m.currentBowler = null;
            }
            promptPlayerSelection(
                    m,
                    "Select Bowler",
                    false,
                    name -> {
                        m.currentBowler = name;
                        updateUI.run();
                        persist();
                    });
        }
    }

    private void promptPlayerSelection(
            Match m, String title, boolean isBattingTeam, OnPlayerSelectedListener listener) {
        if (m.battingTeam == null || m.bowlingTeam == null) {
            return;
        }
        List<String> squad =
                isBattingTeam
                        ? (m.battingTeam.equals(m.team1) ? m.squad1 : m.squad2)
                        : (m.bowlingTeam.equals(m.team1) ? m.squad1 : m.squad2);

        List<String> available = new ArrayList<>(squad);
        if (!isBattingTeam && m.maxOversPerBowler > 0) {
            available.removeIf(name -> BowlingQuotaHelper.isAtOrOverQuota(m, name));
        }
        if (isBattingTeam) {
            if (m.striker != null) {
                available.remove(m.striker);
            }
            if (m.nonStriker != null) {
                available.remove(m.nonStriker);
            }
            List<String> outPlayers = new ArrayList<>();
            for (BallEvent event : MatchBallEvents.forStats(m)) {
                if (event.wicketType != BallEvent.WicketType.NONE) {
                    outPlayers.add(event.striker);
                }
            }
            available.removeAll(outPlayers);
        }

        if (available.isEmpty()) {
            Toast.makeText(
                            this,
                            !isBattingTeam && m.maxOversPerBowler > 0
                                    ? "No bowlers left under the per-bowler over limit"
                                    : "No available players",
                            Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        String[] options = available.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setCancelable(true)
                .setItems(options, (dialog, which) -> listener.onSelected(options[which]))
                .show();
    }

    interface OnPlayerSelectedListener {
        void onSelected(String name);
    }

    private void checkInningsOverAndBowlerChange(Match m, Runnable updateUI) {
        double currentOvers = (m.battingTeam.equals(m.team1)) ? m.overs1 : m.overs2;
        int currentWickets = (m.battingTeam.equals(m.team1)) ? m.wickets1 : m.wickets2;
        int maxWickets = MatchCompletionHelper.getMaxWickets(m);

        if (currentOvers >= m.maxOvers || currentWickets >= maxWickets) {
            return;
        }

        if (Math.round((currentOvers - (int) currentOvers) * 10) == 0) {
            promptPlayerSelection(
                    m,
                    "Select New Bowler",
                    false,
                    name -> {
                        m.currentBowler = name;
                        updateUI.run();
                        persist();
                    });
        }
    }

    private void showWicketTypeDialog(Match m, Runnable updateUI) {
        String[] types = {"Bowled", "Caught", "LBW", "Run Out", "Stumped", "Hit Wicket"};
        new AlertDialog.Builder(this)
                .setTitle("Select Wicket Type")
                .setItems(
                        types,
                        (dialog, which) -> {
                            if (m.maxOversPerBowler > 0
                                    && m.currentBowler != null
                                    && BowlingQuotaHelper.wouldExceedQuotaAfterLegalBall(
                                            m, m.currentBowler)) {
                                Toast.makeText(
                                                this,
                                                "This delivery would exceed the bowler's over limit for this innings.",
                                                Toast.LENGTH_SHORT)
                                        .show();
                                return;
                            }
                            BallEvent event = new BallEvent();
                            event.striker = m.striker;
                            event.nonStriker = m.nonStriker;
                            event.bowler = m.currentBowler;
                            event.wicketType =
                                    BallEvent.WicketType.valueOf(
                                            types[which].toUpperCase().replace(" ", "_"));

                            CricketScoringHelper.processBallAndUpdateRotation(m, event);
                            CricketScoringHelper.checkMatchStatus(m);

                            int currentWickets =
                                    (m.battingTeam.equals(m.team1)) ? m.wickets1 : m.wickets2;
                            if (!MatchCompletionHelper.isEffectivelyCompleted(m)
                                    && currentWickets <= MatchCompletionHelper.getMaxWickets(m)) {
                                m.striker = null;
                                checkAndPromptInitialPlayers(m, updateUI);
                            }

                            updateUI.run();
                            persist();

                            if (!MatchCompletionHelper.isEffectivelyCompleted(m)) {
                                checkInningsOverAndBowlerChange(m, updateUI);
                            }
                        })
                .show();
    }

    private void addRRRDisplay(Match m, TextView txtMatchInfo) {
        if (!MatchCompletionHelper.isEffectivelyCompleted(m) && m.currentInnings == 2) {
            int battingScore =
                    (m.battingTeam.equals(m.team1))
                            ? MatchScoreDisplay.runs1(m)
                            : MatchScoreDisplay.runs2(m);
            int bowlingScore =
                    (m.battingTeam.equals(m.team1))
                            ? MatchScoreDisplay.runs2(m)
                            : MatchScoreDisplay.runs1(m);

            int target = bowlingScore + 1;

            double overs =
                    (m.battingTeam.equals(m.team1))
                            ? MatchScoreDisplay.overs1(m)
                            : MatchScoreDisplay.overs2(m);
            int ballsBowled = ((int) overs * 6) + (int) Math.round((overs - (int) overs) * 10);

            int totalBalls = m.maxOvers * 6;
            int ballsLeft = totalBalls - ballsBowled;

            int runsNeeded = target - battingScore;

            if (ballsLeft > 0 && runsNeeded > 0) {
                double rrr = (runsNeeded * 6.0) / ballsLeft;
                txtMatchInfo.setText(
                        m.battingTeam
                                + " needs "
                                + runsNeeded
                                + " runs in "
                                + ballsLeft
                                + " balls (RRR: "
                                + String.format(Locale.getDefault(), "%.2f", rrr)
                                + ")");
            }
        }
    }
}

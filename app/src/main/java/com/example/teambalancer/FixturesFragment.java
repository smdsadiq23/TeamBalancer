package com.example.teambalancer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.teambalancer.databinding.FragmentFixturesBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FixturesFragment extends Fragment {

    private FragmentFixturesBinding binding;
    private DataManager dataManager;
    private FixtureAdapter fixtureAdapter;
    private final List<Match> currentMatches = new ArrayList<>();
    private Club currentClub;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentFixturesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dataManager = new DataManager(requireContext());

        binding.toolbarFixtures.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        setupRecyclerView();
        observeCurrentClub();

        binding.btnAddFixture.setOnClickListener(v -> showAddMatchDialog());
    }

    private void setupRecyclerView() {
        fixtureAdapter = new FixtureAdapter(currentMatches);
        fixtureAdapter.setOnMatchActionListener(new FixtureAdapter.OnMatchActionListener() {
            @Override
            public void onEditMatch(Match match, int position) {
                if ("Cricket".equalsIgnoreCase(match.sport)) {
                    if (!match.hasStarted) {
                        showCricketSetupDialog(match);
                    } else {
                        showCricketScoringDialog(match);
                    }
                } else {
                    showSimpleEditScoreDialog(match);
                }
            }

            @Override
            public void onDeleteMatch(Match match, int position) {
                confirmDeleteMatch(match);
            }

            @Override
            public void onViewMatch(Match match, int position) {
                if ("Cricket".equalsIgnoreCase(match.sport)) {
                    showCricketScoringDialog(match);
                } else {
                    showSimpleEditScoreDialog(match);
                }
            }
        });
        binding.recyclerFixtures.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerFixtures.setAdapter(fixtureAdapter);
    }

    private void confirmDeleteMatch(Match match) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Match")
                .setMessage("Are you sure you want to delete this match?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (currentClub != null && !currentClub.history.isEmpty()) {
                        Club.TeamHistory latestHistory = currentClub.history.get(currentClub.history.size() - 1);
                        if (latestHistory.matches != null) {
                            latestHistory.matches.remove(match);
                            dataManager.updateClub(currentClub);
                            loadFixtures(currentClub);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void observeCurrentClub() {
        dataManager.getCurrentClub().observe(getViewLifecycleOwner(), club -> {
            if (club != null) {
                currentClub = club;
                binding.txtClubName.setText(club.name);
                loadFixtures(club);
            }
        });
    }

    private void loadFixtures(Club club) {
        currentMatches.clear();
        if (club.history != null && !club.history.isEmpty()) {
            Club.TeamHistory latestHistory = club.history.get(club.history.size() - 1);
            if (latestHistory.matches == null) {
                latestHistory.matches = new ArrayList<>();
            }
            currentMatches.addAll(latestHistory.matches);
        }
        fixtureAdapter.updateMatches(currentMatches);

        if (currentMatches.isEmpty()) {
            binding.txtEmptyFixtures.setText("No matches found");
            binding.txtEmptyFixtures.setVisibility(View.VISIBLE);
        } else {
            binding.txtEmptyFixtures.setVisibility(View.GONE);
        }
    }

    private void showAddMatchDialog() {
        if (currentClub == null || currentClub.history == null || currentClub.history.isEmpty()) {
            Toast.makeText(requireContext(), "No active session. Generate teams first.", Toast.LENGTH_SHORT).show();
            return;
        }

        Club.TeamHistory latestHistory = currentClub.history.get(currentClub.history.size() - 1);
        List<String> teamNames = latestHistory.teams.stream().map(t -> t.name).collect(Collectors.toList());

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_fixture, null);
        AutoCompleteTextView spinnerSport = dialogView.findViewById(R.id.spinnerSport);
        AutoCompleteTextView spinnerTeam1 = dialogView.findViewById(R.id.spinnerTeam1);
        AutoCompleteTextView spinnerTeam2 = dialogView.findViewById(R.id.spinnerTeam2);

        String[] sports = {"Cricket", "Football", "Basketball", "Other"};
        spinnerSport.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, sports));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, teamNames);
        spinnerTeam1.setAdapter(adapter);
        spinnerTeam2.setAdapter(adapter);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add New Match")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String sport = spinnerSport.getText().toString();
                    String t1 = spinnerTeam1.getText().toString();
                    String t2 = spinnerTeam2.getText().toString();

                    if (t1.isEmpty() || t2.isEmpty() || t1.equals(t2)) {
                        Toast.makeText(requireContext(), "Select two different teams", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Match newMatch = new Match(t1, t2, sport);
                    
                    Team team1Obj = latestHistory.teams.stream().filter(t -> t.name.equals(t1)).findFirst().orElse(null);
                    Team team2Obj = latestHistory.teams.stream().filter(t -> t.name.equals(t2)).findFirst().orElse(null);
                    if (team1Obj != null) {
                        newMatch.squad1 = team1Obj.players.stream().map(p -> p.name).collect(Collectors.toList());
                    }
                    if (team2Obj != null) {
                        newMatch.squad2 = team2Obj.players.stream().map(p -> p.name).collect(Collectors.toList());
                    }

                    if (latestHistory.matches == null) latestHistory.matches = new ArrayList<>();
                    latestHistory.matches.add(newMatch);
                    
                    dataManager.updateClub(currentClub);
                    loadFixtures(currentClub);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCricketSetupDialog(Match match) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_setup_cricket_match, null);
        AutoCompleteTextView spinnerMatchType = view.findViewById(R.id.spinnerMatchType);
        TextInputEditText editMaxOvers = view.findViewById(R.id.editMaxOvers);
        
        String[] types = {"T20", "ODI", "Test", "Box Cricket", "Custom"};
        spinnerMatchType.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, types));

        new AlertDialog.Builder(requireContext())
                .setView(view)
                .setTitle("Setup Match: " + match.team1 + " vs " + match.team2)
                .setPositiveButton("Next (Toss)", (dialog, which) -> {
                    match.hasStarted = true;
                    match.matchType = spinnerMatchType.getText().toString();
                    try {
                        match.maxOvers = Integer.parseInt(editMaxOvers.getText().toString());
                    } catch (Exception e) { match.maxOvers = 20; }
                    showTossDialog(match);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTossDialog(Match match) {
        String[] teams = {match.team1, match.team2};
        new AlertDialog.Builder(requireContext())
                .setTitle("Who won the toss?")
                .setItems(teams, (dialog, which) -> {
                    match.tossWinner = teams[which];
                    showTossDecisionDialog(match);
                })
                .show();
    }

    private void showTossDecisionDialog(Match match) {
        String[] options = {"Batting", "Bowling"};
        new AlertDialog.Builder(requireContext())
                .setTitle(match.tossWinner + " won the toss and chose to:")
                .setItems(options, (dialog, which) -> {
                    match.tossDecision = options[which];
                    
                    if (match.tossWinner.equals(match.team1)) {
                        if (match.tossDecision.equals("Batting")) {
                            match.battingTeam = match.team1;
                            match.bowlingTeam = match.team2;
                        } else {
                            match.battingTeam = match.team2;
                            match.bowlingTeam = match.team1;
                        }
                    } else {
                        if (match.tossDecision.equals("Batting")) {
                            match.battingTeam = match.team2;
                            match.bowlingTeam = match.team1;
                        } else {
                            match.battingTeam = match.team1;
                            match.bowlingTeam = match.team2;
                        }
                    }
                    
                    dataManager.updateClub(currentClub);
                    showCricketScoringDialog(match);
                })
                .show();
    }

    private void showCricketScoringDialog(Match match) {
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_cricket_scoring, null);
        TextView txtMatchInfo = view.findViewById(R.id.txtMatchInfo);
        TextView txtLiveScore = view.findViewById(R.id.txtLiveScore);
        TextView txtLiveOvers = view.findViewById(R.id.txtLiveOvers);
        MaterialButton btnFinish = view.findViewById(R.id.btnFinish);
        View layoutControls = view.findViewById(R.id.layoutScoringControls);
        View btnUndo = view.findViewById(R.id.btnUndo);

        TextView txtStriker = view.findViewById(R.id.txtStriker);
        TextView txtStrikerStats = view.findViewById(R.id.txtStrikerStats);
        TextView txtNonStriker = view.findViewById(R.id.txtNonStriker);
        TextView txtNonStrikerStats = view.findViewById(R.id.txtNonStrikerStats);
        TextView txtBowler = view.findViewById(R.id.txtBowler);
        TextView txtBowlerStats = view.findViewById(R.id.txtBowlerStats);

        Runnable updateUI = () -> {
            int runs = (match.battingTeam != null && match.battingTeam.equals(match.team1)) ? match.score1 : match.score2;
            int wkts = (match.battingTeam != null && match.battingTeam.equals(match.team1)) ? match.wickets1 : match.wickets2;
            double overs = (match.battingTeam != null && match.battingTeam.equals(match.team1)) ? match.overs1 : match.overs2;
            
            txtLiveScore.setText(runs + "/" + wkts);
            txtLiveOvers.setText(String.format(Locale.getDefault(), "(%.1f / %d)", overs, match.maxOvers));
            
            if (match.isCompleted) {
                btnFinish.setText("CLOSE SCORECARD");
                if (layoutControls != null) layoutControls.setVisibility(View.GONE);
                if (btnUndo != null) btnUndo.setVisibility(View.GONE);
                txtMatchInfo.setText(getWinnerString(match));
            } else {
                txtMatchInfo.setText(match.battingTeam + " innings" + (match.currentInnings == 2 ? " (Target: " + ((match.battingTeam.equals(match.team1) ? match.score2 : match.score1) + 1) + ")" : ""));
                
                addRRRDisplay(match, txtMatchInfo);

                if (match.currentInnings == 1 && !"Test".equals(match.matchType)) {
                    btnFinish.setText("START 2ND INNINGS");
                } else {
                    btnFinish.setText("FINISH MATCH");
                }
            }

            updatePlayerSection(match, txtStriker, txtStrikerStats, txtNonStriker, txtNonStrikerStats, txtBowler, txtBowlerStats);
        };

        if (match.squad1 == null || match.squad1.isEmpty()) match.squad1 = getSquadFromHistory(match.team1);
        if (match.squad2 == null || match.squad2.isEmpty()) match.squad2 = getSquadFromHistory(match.team2);

        if (!match.isCompleted) {
            txtStriker.setOnClickListener(v -> promptPlayerSelection(match, "Select Striker", true, name -> {
                match.striker = name;
                updateUI.run();
                dataManager.updateClub(currentClub);
            }));
            txtNonStriker.setOnClickListener(v -> promptPlayerSelection(match, "Select Non-Striker", true, name -> {
                match.nonStriker = name;
                updateUI.run();
                dataManager.updateClub(currentClub);
            }));
            txtBowler.setOnClickListener(v -> promptPlayerSelection(match, "Select Bowler", false, name -> {
                match.currentBowler = name;
                updateUI.run();
                dataManager.updateClub(currentClub);
            }));
        }

        updateUI.run();

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
                .setView(view)
                .create();

        View.OnClickListener scoringListener = v -> {
            if (match.isCompleted) return;
            
            if (match.striker == null || match.nonStriker == null || match.currentBowler == null) {
                checkAndPromptInitialPlayers(match, updateUI);
                return;
            }

            double currentOvers = (match.battingTeam.equals(match.team1)) ? match.overs1 : match.overs2;
            int currentWickets = (match.battingTeam.equals(match.team1)) ? match.wickets1 : match.wickets2;
            int maxWickets = getMaxWickets(match);
            
            if (currentOvers >= match.maxOvers || currentWickets >= maxWickets) {
                Toast.makeText(requireContext(), "Innings over!", Toast.LENGTH_SHORT).show();
                return;
            }

            BallEvent event = new BallEvent();
            event.striker = match.striker;
            event.nonStriker = match.nonStriker;
            event.bowler = match.currentBowler;

            if (v.getId() == R.id.btn0) event.runs = 0;
            else if (v.getId() == R.id.btn1) event.runs = 1;
            else if (v.getId() == R.id.btn2) event.runs = 2;
            else if (v.getId() == R.id.btn3) event.runs = 3;
            else if (v.getId() == R.id.btn4) event.runs = 4;
            else if (v.getId() == R.id.btn6) event.runs = 6;
            else if (v.getId() == R.id.btnWide) {
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
                showWicketTypeDialog(match, updateUI);
                return;
            }

            processBallAndUpdateRotation(match, event);
            checkMatchStatus(match);
            updateUI.run();
            dataManager.updateClub(currentClub);
            fixtureAdapter.notifyDataSetChanged();

            if (!match.isCompleted) {
                checkInningsOverAndBowlerChange(match, updateUI);
            }
        };

        int[] ids = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn6, R.id.btnWide, R.id.btnNB, R.id.btnBye, R.id.btnLB, R.id.btnWicket};
        for (int id : ids) {
            View b = view.findViewById(id);
            if (b != null) b.setOnClickListener(scoringListener);
        }

        if (btnUndo != null) {
            btnUndo.setOnClickListener(v -> {
                if (match.isCompleted) return;
                if (!match.ballHistory.isEmpty()) {
                    BallEvent last = match.ballHistory.remove(match.ballHistory.size() - 1);
                    undoBall(match, last);
                    updateUI.run();
                    dataManager.updateClub(currentClub);
                    fixtureAdapter.notifyDataSetChanged();
                }
            });
        }

        btnFinish.setOnClickListener(v -> {
            if (match.isCompleted) {
                dialog.dismiss();
                return;
            }
            
            if (match.currentInnings == 1 && !"Test".equals(match.matchType)) {
                match.currentInnings = 2;
                String temp = match.battingTeam;
                match.battingTeam = match.bowlingTeam;
                match.bowlingTeam = temp;
                match.striker = null;
                match.nonStriker = null;
                match.currentBowler = null;
                checkAndPromptInitialPlayers(match, updateUI);
                updateUI.run();
                dataManager.updateClub(currentClub);
            } else {
                match.isCompleted = true;
                dataManager.updateClub(currentClub);
                fixtureAdapter.notifyDataSetChanged();
                updateUI.run();
                dialog.dismiss();
            }
        });

        dialog.show();
        
        if (!match.isCompleted) {
            checkAndPromptInitialPlayers(match, updateUI);
        }
    }

    private void checkMatchStatus(Match match) {
        if (match.isCompleted) return;

        if (match.currentInnings == 2 && !"Test".equals(match.matchType)) {
            int battingScore = (match.battingTeam.equals(match.team1)) ? match.score1 : match.score2;
            int bowlingScore = (match.battingTeam.equals(match.team1)) ? match.score2 : match.score1;

            int target = bowlingScore + 1;

            int wkts = (match.battingTeam.equals(match.team1)) ? match.wickets1 : match.wickets2;
            double overs = (match.battingTeam.equals(match.team1)) ? match.overs1 : match.overs2;

            if (battingScore >= target || wkts >= getMaxWickets(match) || overs >= match.maxOvers) {
                match.isCompleted = true;
            }
        }
    }

    private String getWinnerString(Match match) {
        if (match.score1 == match.score2) return "Match Tied";

        boolean team1Chasing = match.currentInnings == 2 && match.battingTeam != null && match.battingTeam.equals(match.team1);

        if (match.score1 > match.score2) {
            if (team1Chasing) {
                int wkts = getMaxWickets(match) - match.wickets1;
                return match.team1 + " won by " + wkts + " wickets";
            } else {
                return match.team1 + " won by " + (match.score1 - match.score2) + " runs";
            }
        } else {
            if (!team1Chasing) {
                int wkts = getMaxWickets(match) - match.wickets2;
                return match.team2 + " won by " + wkts + " wickets";
            } else {
                return match.team2 + " won by " + (match.score2 - match.score1) + " runs";
            }
        }
    }

    private List<String> getSquadFromHistory(String teamName) {
        if (currentClub != null && currentClub.history != null && !currentClub.history.isEmpty()) {
            Club.TeamHistory latest = currentClub.history.get(currentClub.history.size() - 1);
            for (Team team : latest.teams) {
                if (team.name.equals(teamName)) {
                    return team.players.stream().map(p -> p.name).collect(Collectors.toList());
                }
            }
        }
        return new ArrayList<>();
    }

    private void updatePlayerSection(Match match, TextView txtStr, TextView txtStrStats, TextView txtNonStr, TextView txtNonStrStats, TextView txtBowler, TextView txtBowlerStats) {
        if (match.striker != null) {
            txtStr.setText(match.striker + (match.isCompleted ? "" : " ⭐"));
            txtStrStats.setText(getPlayerBattingStats(match, match.striker));
        } else {
            txtStr.setText(match.isCompleted ? "" : "Select Striker");
            txtStrStats.setText("-");
        }

        if (match.nonStriker != null) {
            txtNonStr.setText(match.nonStriker);
            txtNonStrStats.setText(getPlayerBattingStats(match, match.nonStriker));
        } else {
            txtNonStr.setText(match.isCompleted ? "" : "Select Non-Striker");
            txtNonStrStats.setText("-");
        }

        if (match.currentBowler != null) {
            txtBowler.setText(match.currentBowler);
            txtBowlerStats.setText(getPlayerBowlingStats(match, match.currentBowler));
        } else {
            txtBowler.setText(match.isCompleted ? "" : "Select Bowler");
            txtBowlerStats.setText("-");
        }
    }

    private String getPlayerBattingStats(Match match, String playerName) {
        int runs = 0;
        int balls = 0;
        for (BallEvent event : match.ballHistory) {
            if (playerName.equals(event.striker)) {
                if (event.extraType != BallEvent.ExtraType.WIDE) {
                    balls++;
                    if (event.extraType == BallEvent.ExtraType.NONE) {
                        runs += event.runs;
                    }
                }
            }
        }
        return runs + " (" + balls + ")";
    }

    private String getPlayerBowlingStats(Match match, String playerName) {
        int runsConceded = 0;
        int wickets = 0;
        int balls = 0;
        for (BallEvent event : match.ballHistory) {
            if (playerName.equals(event.bowler)) {
                if (event.isLegalBall) balls++;
                if (event.extraType == BallEvent.ExtraType.WIDE || event.extraType == BallEvent.ExtraType.NO_BALL) {
                    runsConceded += event.runs;
                } else if (event.extraType == BallEvent.ExtraType.NONE) {
                    runsConceded += event.runs;
                }
                if (event.wicketType != BallEvent.WicketType.NONE && event.wicketType != BallEvent.WicketType.RUN_OUT) {
                    wickets++;
                }
            }
        }
        int overs = balls / 6;
        int remainingBalls = balls % 6;
        return String.format(Locale.getDefault(), "%d.%d - %d - %d", overs, remainingBalls, runsConceded, wickets);
    }

    private void checkAndPromptInitialPlayers(Match match, Runnable updateUI) {
        if (match.striker == null) {
            promptPlayerSelection(match, "Select Striker", true, name -> {
                match.striker = name;
                checkAndPromptInitialPlayers(match, updateUI);
            });
        } else if (match.nonStriker == null) {
            promptPlayerSelection(match, "Select Non-Striker", true, name -> {
                match.nonStriker = name;
                checkAndPromptInitialPlayers(match, updateUI);
            });
        } else if (match.currentBowler == null) {
            promptPlayerSelection(match, "Select Bowler", false, name -> {
                match.currentBowler = name;
                updateUI.run();
                dataManager.updateClub(currentClub);
            });
        }
    }

    private void promptPlayerSelection(Match match, String title, boolean isBattingTeam, OnPlayerSelectedListener listener) {
        if (match.battingTeam == null || match.bowlingTeam == null) return;
        List<String> squad = isBattingTeam ? 
            (match.battingTeam.equals(match.team1) ? match.squad1 : match.squad2) :
            (match.bowlingTeam.equals(match.team1) ? match.squad1 : match.squad2);
        
        List<String> available = new ArrayList<>(squad);
        if (isBattingTeam) {
            if (match.striker != null) available.remove(match.striker);
            if (match.nonStriker != null) available.remove(match.nonStriker);
            List<String> outPlayers = new ArrayList<>();
            for (BallEvent event : match.ballHistory) {
                if (event.wicketType != BallEvent.WicketType.NONE) {
                    outPlayers.add(event.striker); 
                }
            }
            available.removeAll(outPlayers);
        }

        if (available.isEmpty()) {
            Toast.makeText(requireContext(), "No available players", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = available.toArray(new String[0]);
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setCancelable(true)
                .setItems(options, (dialog, which) -> listener.onSelected(options[which]))
                .show();
    }

    interface OnPlayerSelectedListener {
        void onSelected(String name);
    }

    private int getMaxWickets(Match match) {
        if (match.battingTeam == null) return 10;
        List<String> squad = match.battingTeam.equals(match.team1) ? match.squad1 : match.squad2;
        if (squad != null && !squad.isEmpty()) return squad.size() - 1;
        return 10;
    }

    private void processBallAndUpdateRotation(Match match, BallEvent event) {
        if (match.isCompleted) return;

        processBall(match, event);

        int runsForRotation = 0;
        if (event.extraType == BallEvent.ExtraType.NONE) {
            runsForRotation = event.runs;
        } else if (event.extraType == BallEvent.ExtraType.BYE || event.extraType == BallEvent.ExtraType.LEG_BYE) {
            runsForRotation = event.runs;
        }

        if (runsForRotation % 2 != 0) {
            String temp = match.striker;
            match.striker = match.nonStriker;
            match.nonStriker = temp;
        }

        double overs = (match.battingTeam != null && match.battingTeam.equals(match.team1)) ? match.overs1 : match.overs2;
        int balls = (int) Math.round((overs - (int) overs) * 10);

        if (event.isLegalBall && balls == 0) {
            String temp = match.striker;
            match.striker = match.nonStriker;
            match.nonStriker = temp;
            match.currentBowler = null;
        }
    }

    private void checkInningsOverAndBowlerChange(Match match, Runnable updateUI) {
        if (match.battingTeam == null) return;
        double currentOvers = (match.battingTeam.equals(match.team1)) ? match.overs1 : match.overs2;
        int currentWickets = (match.battingTeam.equals(match.team1)) ? match.wickets1 : match.wickets2;
        int maxWickets = getMaxWickets(match);

        if (currentOvers >= match.maxOvers || currentWickets >= maxWickets) {
            return;
        }

        if (Math.round((currentOvers - (int)currentOvers) * 10) == 0) {
            promptPlayerSelection(match, "Select New Bowler", false, name -> {
                match.currentBowler = name;
                updateUI.run();
                dataManager.updateClub(currentClub);
            });
        }
    }

    private void showWicketTypeDialog(Match match, Runnable updateUI) {
        String[] types = {"Bowled", "Caught", "LBW", "Run Out", "Stumped", "Hit Wicket"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Wicket Type")
                .setItems(types, (dialog, which) -> {
                    BallEvent event = new BallEvent();
                    event.striker = match.striker;
                    event.nonStriker = match.nonStriker;
                    event.bowler = match.currentBowler;
                    event.wicketType = BallEvent.WicketType.valueOf(types[which].toUpperCase().replace(" ", "_"));
                    
                    processBallAndUpdateRotation(match, event);
                    checkMatchStatus(match);
                    
                    int currentWickets = (match.battingTeam != null && match.battingTeam.equals(match.team1)) ? match.wickets1 : match.wickets2;
                    if (!match.isCompleted && currentWickets <= getMaxWickets(match)) {
                        match.striker = null; 
                        checkAndPromptInitialPlayers(match, updateUI);
                    }
                    
                    updateUI.run();
                    dataManager.updateClub(currentClub);
                    fixtureAdapter.notifyDataSetChanged();
                    
                    if (!match.isCompleted) {
                        checkInningsOverAndBowlerChange(match, updateUI);
                    }
                })
                .show();
    }

    private void processBall(Match match, BallEvent event) {
        if (match.isCompleted || match.battingTeam == null) return;

        match.ballHistory.add(event);
        boolean isTeam1 = match.battingTeam.equals(match.team1);

        if (event.extraType == BallEvent.ExtraType.NO_BALL) match.isFreeHit = true;

        if (match.isFreeHit && event.wicketType != BallEvent.WicketType.RUN_OUT) {
            event.wicketType = BallEvent.WicketType.NONE;
        }

        if (isTeam1) {
            match.score1 += event.runs;
            if (event.wicketType != BallEvent.WicketType.NONE) match.wickets1++;
            if (event.isLegalBall) match.overs1 = addBall(match.overs1);
        } else {
            match.score2 += event.runs;
            if (event.wicketType != BallEvent.WicketType.NONE) match.wickets2++;
            if (event.isLegalBall) match.overs2 = addBall(match.overs2);
        }

        if (match.isFreeHit && event.extraType != BallEvent.ExtraType.NO_BALL) {
            match.isFreeHit = false;
        }
    }

    private void undoBall(Match match, BallEvent event) {
        if (match.battingTeam == null) return;
        boolean isTeam1 = match.battingTeam.equals(match.team1);
        if (isTeam1) {
            match.score1 -= event.runs;
            if (event.wicketType != BallEvent.WicketType.NONE) match.wickets1--;
            if (event.isLegalBall) match.overs1 = removeBall(match.overs1);
        } else {
            match.score2 -= event.runs;
            if (event.wicketType != BallEvent.WicketType.NONE) match.wickets2--;
            if (event.isLegalBall) match.overs2 = removeBall(match.overs2);
        }
        if (!match.ballHistory.isEmpty()) {
            BallEvent last = match.ballHistory.get(match.ballHistory.size() - 1);
            match.striker = last.striker;
            match.nonStriker = last.nonStriker;
            match.currentBowler = last.bowler;
        }
    }

    private double addBall(double overs) {
        int whole = (int) overs;
        int balls = (int) Math.round((overs - whole) * 10);
        balls++;
        if (balls >= 6) {
            whole++;
            balls = 0;
        }
        return whole + (balls / 10.0);
    }

    private double removeBall(double overs) {
        int whole = (int) overs;
        int balls = (int) Math.round((overs - whole) * 10);
        balls--;
        if (balls < 0) {
            whole--;
            balls = 5;
        }
        if (whole < 0) return 0.0;
        return whole + (balls / 10.0);
    }

    private void showSimpleEditScoreDialog(Match match) {
        if (match.isCompleted) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Match Summary")
                    .setMessage(match.team1 + ": " + match.score1 + "\n" + match.team2 + ": " + match.score2 + "\n\nMatch Completed.")
                    .setPositiveButton("Close", null)
                    .show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_score, null);
        TextInputEditText editScore1 = dialogView.findViewById(R.id.editScore1);
        TextInputEditText editScore2 = dialogView.findViewById(R.id.editScore2);
        
        editScore1.setText(String.valueOf(match.score1));
        editScore2.setText(String.valueOf(match.score2));

        new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setPositiveButton("Save & Finish", (dialog, which) -> {
                    try {
                        match.score1 = Integer.parseInt(editScore1.getText().toString());
                        match.score2 = Integer.parseInt(editScore2.getText().toString());
                        match.hasStarted = true;
                        match.isCompleted = true;
                        dataManager.updateClub(currentClub);
                        fixtureAdapter.notifyDataSetChanged();
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Invalid score", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addRRRDisplay(Match match, TextView txtMatchInfo) {
        if (!match.isCompleted && match.currentInnings == 2 && match.battingTeam != null) {
            int battingScore = (match.battingTeam.equals(match.team1)) ? match.score1 : match.score2;
            int bowlingScore = (match.battingTeam.equals(match.team1)) ? match.score2 : match.score1;

            int target = bowlingScore + 1;

            double overs = (match.battingTeam.equals(match.team1)) ? match.overs1 : match.overs2;
            int ballsBowled = ((int)overs * 6) + (int)Math.round((overs - (int)overs) * 10);

            int totalBalls = match.maxOvers * 6;
            int ballsLeft = totalBalls - ballsBowled;

            int runsNeeded = target - battingScore;

            if (ballsLeft > 0 && runsNeeded > 0) {
                double rrr = (runsNeeded * 6.0) / ballsLeft;
                txtMatchInfo.setText(
                        match.battingTeam + " needs " + runsNeeded +
                                " runs in " + ballsLeft + " balls (RRR: " +
                                String.format(Locale.getDefault(), "%.2f", rrr) + ")"
                );
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

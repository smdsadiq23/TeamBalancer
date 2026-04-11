package com.example.teambalancer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.teambalancer.databinding.FragmentFixturesBinding;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MatchesFragment extends Fragment {

    private static final String ARG_EMBEDDED = "embedded";

    public static MatchesFragment newInstance(boolean embedded) {
        MatchesFragment f = new MatchesFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_EMBEDDED, embedded);
        f.setArguments(args);
        return f;
    }

    private FragmentFixturesBinding binding;
    private DataManager dataManager;
    private MatchAdapter matchAdapter;
    private final List<Match> currentMatches = new ArrayList<>();
    private Club currentClub;
    private Club.TeamHistory latestHistory;

    private ActivityResultLauncher<Intent> cricketScoringLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cricketScoringLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || dataManager == null) {
                        return;
                    }
                    dataManager.loadClubFromDatabaseAsync(fresh -> {
                        if (!isAdded() || fresh == null || binding == null) {
                            return;
                        }
                        currentClub = fresh;
                        binding.txtClubName.setText(fresh.name);
                        loadMatches(fresh);
                    });
                });
    }

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

        boolean embedded = getArguments() != null && getArguments().getBoolean(ARG_EMBEDDED, false);
        if (embedded) {
            binding.appBarFixtures.setVisibility(View.GONE);
        } else {
            binding.toolbarFixtures.setTitle("Matches");
            binding.toolbarFixtures.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        }
        binding.txtSessionBlurb.setText("Ball-by-ball scoring and finished games for this session.");
        binding.txtSectionTitle.setText("Live & completed");
        binding.btnAddFixture.setVisibility(View.GONE);

        setupRecyclerView();
        observeCurrentClub();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dataManager == null) {
            return;
        }
        dataManager.loadClubFromDatabaseAsync(fresh -> {
            if (!isAdded() || fresh == null || binding == null) {
                return;
            }
            currentClub = fresh;
            binding.txtClubName.setText(fresh.name);
            loadMatches(fresh);
        });
    }

    private void setupRecyclerView() {
        matchAdapter = new MatchAdapter(currentMatches);
        matchAdapter.setOnMatchActionListener(new MatchAdapter.OnMatchActionListener() {
            @Override
            public void onEditMatch(Match match, int position) {
                if ("Cricket".equalsIgnoreCase(match.sport)) {
                    if (MatchCompletionHelper.isEffectivelyCompleted(match)) {
                        CompletedMatchScoreboardDialog.show(MatchesFragment.this, match);
                    } else if (!match.hasStarted
                            && match.tossWinner == null
                            && (match.ballHistory == null || match.ballHistory.isEmpty())) {
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
                    if (MatchCompletionHelper.isEffectivelyCompleted(match)) {
                        CompletedMatchScoreboardDialog.show(MatchesFragment.this, match);
                    } else {
                        showCricketScoringDialog(match);
                    }
                } else {
                    showSimpleEditScoreDialog(match);
                }
            }
        });
        binding.recyclerFixtures.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerFixtures.setAdapter(matchAdapter);
    }

    private void confirmDeleteMatch(Match match) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Match")
                .setMessage("Are you sure you want to delete this match?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (currentClub != null && !currentClub.history.isEmpty()) {
                        Club.TeamHistory latestHistory = currentClub.history.get(currentClub.history.size() - 1);
                        if (latestHistory.matches != null) {
                            MatchCompletionHelper.forgetMatchCompletion(match);
                            latestHistory.matches.remove(match);
                            dataManager.updateClub(currentClub);
                            loadMatches(currentClub);
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
                loadMatches(club);
            }
        });
    }

    private void loadMatches(Club club) {
        currentMatches.clear();
        if (club.history != null && !club.history.isEmpty()) {
            latestHistory = club.history.get(club.history.size() - 1);
            boolean updated = false;
            if (latestHistory.matches == null) {
                latestHistory.matches = new ArrayList<>();
                updated = true;
            }

            if (latestHistory.matches != null) {
                updated |= SessionMatchLoader.prepareMatchesForSession(latestHistory.matches);
            }

            for (Match m : latestHistory.matches) {
                if (!MatchFixtureHelper.isScheduledFixture(m)) {
                    currentMatches.add(m);
                }
            }
            Collections.sort(currentMatches, SessionMatchLoader.BY_SCHEDULE_THEN_TEAM);
            if (updated) {
                dataManager.updateClub(club);
            }
        }

        if (currentMatches.isEmpty()) {
            binding.txtEmptyFixtures.setText("No matches in progress yet.\nAdd a fixture under Scheduled, then toss and score.");
            binding.txtEmptyFixtures.setVisibility(View.VISIBLE);
        } else {
            binding.txtEmptyFixtures.setVisibility(View.GONE);
        }
        matchAdapter.notifyDataSetChanged();
    }

    private void showCricketSetupDialog(Match match) {
        if (MatchCompletionHelper.isEffectivelyCompleted(match)) {
            showCricketScoringDialog(match);
            return;
        }
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_setup_cricket_match, null);
        AutoCompleteTextView spinnerMatchType = view.findViewById(R.id.spinnerMatchType);
        TextInputEditText editMaxOvers = view.findViewById(R.id.editMaxOvers);
        TextInputEditText editMaxOversPerBowler = view.findViewById(R.id.editMaxOversPerBowler);
        if (match.maxOversPerBowler > 0) {
            editMaxOversPerBowler.setText(String.valueOf(match.maxOversPerBowler));
        }

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
                    try {
                        match.maxOversPerBowler = Integer.parseInt(editMaxOversPerBowler.getText().toString().trim());
                    } catch (Exception e) { match.maxOversPerBowler = 0; }
                    if (match.maxOversPerBowler < 0) {
                        match.maxOversPerBowler = 0;
                    }
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

                    MatchFixtureHelper.promoteToMatch(match);
                    MatchPersistenceHelper.syncJsonFromLists(match);
                    dataManager.updateClub(currentClub);
                    showCricketScoringDialog(match);
                })
                .show();
    }

    private void showCricketScoringDialog(Match match) {
        if (MatchCompletionHelper.isEffectivelyCompleted(match)) {
            CompletedMatchScoreboardDialog.show(MatchesFragment.this, match);
            return;
        }
        Intent i = new Intent(requireContext(), CricketScoringActivity.class);
        i.putExtra(CricketScoringActivity.EXTRA_MATCH, match);
        i.putExtra(CricketScoringActivity.EXTRA_CLUB_NAME, currentClub != null ? currentClub.name : null);
        cricketScoringLauncher.launch(i);
    }

    private void showSimpleEditScoreDialog(Match match) {
        if (MatchCompletionHelper.isEffectivelyCompleted(match)) {
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
                        MatchFixtureHelper.promoteToMatch(match);
                        match.score1 = Integer.parseInt(editScore1.getText().toString());
                        match.score2 = Integer.parseInt(editScore2.getText().toString());
                        match.hasStarted = true;
                        MatchCompletionHelper.markMatchCompleted(match);
                        MatchPersistenceHelper.syncJsonFromLists(match);
                        dataManager.updateClub(currentClub);
                        matchAdapter.notifyDataSetChanged();
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Invalid score", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

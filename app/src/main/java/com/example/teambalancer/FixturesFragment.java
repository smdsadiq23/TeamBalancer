package com.example.teambalancer;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.teambalancer.databinding.FragmentFixturesBinding;
import com.google.android.material.textfield.TextInputEditText;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class FixturesFragment extends Fragment {

    private static final String ARG_EMBEDDED = "embedded";

    public static FixturesFragment newInstance(boolean embedded) {
        FixturesFragment f = new FixturesFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_EMBEDDED, embedded);
        f.setArguments(args);
        return f;
    }

    private FragmentFixturesBinding binding;
    private DataManager dataManager;
    private FixtureAdapter fixtureAdapter;
    private final List<Match> currentMatches = new ArrayList<>();
    private Club currentClub;

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
                        loadFixtures(fresh);
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
            binding.toolbarFixtures.setTitle("Fixtures");
            binding.toolbarFixtures.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        }
        binding.txtSessionBlurb.setText("Schedule games for this session. Tap a fixture to toss and score; it then appears under Live & results.");
        binding.txtSectionTitle.setText("Scheduled");
        binding.btnAddFixture.setVisibility(View.VISIBLE);

        setupRecyclerView();
        observeCurrentClub();

        binding.btnAddFixture.setOnClickListener(v -> showAddMatchDialog());
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
            loadFixtures(fresh);
        });
    }

    private void setupRecyclerView() {
        fixtureAdapter = new FixtureAdapter(currentMatches);
        fixtureAdapter.setOnMatchActionListener(new FixtureAdapter.OnMatchActionListener() {
            @Override
            public void onEditMatch(Match match, int position) {
                if ("Cricket".equalsIgnoreCase(match.sport)) {
                    if (MatchCompletionHelper.isEffectivelyCompleted(match)) {
                        CompletedMatchScoreboardDialog.show(FixturesFragment.this, match);
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
                        CompletedMatchScoreboardDialog.show(FixturesFragment.this, match);
                    } else {
                        showCricketScoringDialog(match);
                    }
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
                    if (currentClub != null && currentClub.history != null && !currentClub.history.isEmpty()) {
                        Club.TeamHistory latestHistory = currentClub.history.get(currentClub.history.size() - 1);
                        if (latestHistory.matches != null) {
                            MatchCompletionHelper.forgetMatchCompletion(match);
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
            if (latestHistory.matches != null) {
                for (Match m : latestHistory.matches) {
                    if (m != null && MatchFixtureHelper.isScheduledFixture(m)) {
                        currentMatches.add(m);
                    }
                }
                Collections.sort(currentMatches, SessionMatchLoader.BY_SCHEDULE_THEN_TEAM);
            }
        }
        fixtureAdapter.updateMatches(new ArrayList<>(currentMatches));

        if (currentMatches.isEmpty()) {
            binding.txtEmptyFixtures.setText("No scheduled fixtures.\nTap Add New to create one.");
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
        TextInputEditText editVenue = dialogView.findViewById(R.id.editVenue);
        TextInputEditText editSchedule = dialogView.findViewById(R.id.editScheduleStart);
        final long[] scheduleMs = {0L};

        editSchedule.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            if (scheduleMs[0] > 0) {
                c.setTimeInMillis(scheduleMs[0]);
            }
            new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        Calendar dayCal = Calendar.getInstance();
                        dayCal.set(year, month, dayOfMonth);
                        new TimePickerDialog(
                                requireContext(),
                                (tv, hour, minute) -> {
                                    dayCal.set(Calendar.HOUR_OF_DAY, hour);
                                    dayCal.set(Calendar.MINUTE, minute);
                                    dayCal.set(Calendar.SECOND, 0);
                                    dayCal.set(Calendar.MILLISECOND, 0);
                                    scheduleMs[0] = dayCal.getTimeInMillis();
                                    editSchedule.setText(DateFormat.getDateTimeInstance(
                                            DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                                            .format(dayCal.getTime()));
                                },
                                c.get(Calendar.HOUR_OF_DAY),
                                c.get(Calendar.MINUTE),
                                false
                        ).show();
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            ).show();
        });

        String[] sports = {"Cricket", "Football", "Basketball", "Other"};
        spinnerSport.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, sports));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.dropdown_item, teamNames);
        spinnerTeam1.setAdapter(adapter);
        spinnerTeam2.setAdapter(adapter);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add fixture")
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
                    if (editVenue != null && editVenue.getText() != null) {
                        newMatch.venue = editVenue.getText().toString().trim();
                    }
                    newMatch.scheduledStartMillis = scheduleMs[0];

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
                        String oversStr = editMaxOvers.getText() != null ? editMaxOvers.getText().toString() : "20";
                        match.maxOvers = Integer.parseInt(oversStr);
                    } catch (Exception e) { match.maxOvers = 20; }
                    try {
                        String perBowlerStr = editMaxOversPerBowler.getText() != null ? editMaxOversPerBowler.getText().toString().trim() : "0";
                        match.maxOversPerBowler = Integer.parseInt(perBowlerStr);
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
            CompletedMatchScoreboardDialog.show(FixturesFragment.this, match);
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
                        String s1 = editScore1.getText() != null ? editScore1.getText().toString() : "0";
                        String s2 = editScore2.getText() != null ? editScore2.getText().toString() : "0";
                        match.score1 = Integer.parseInt(s1);
                        match.score2 = Integer.parseInt(s2);
                        match.hasStarted = true;
                        MatchCompletionHelper.markMatchCompleted(match);
                        MatchPersistenceHelper.syncJsonFromLists(match);
                        dataManager.updateClub(currentClub);
                        fixtureAdapter.notifyDataSetChanged();
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

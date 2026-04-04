package com.example.teambalancer;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.teambalancer.databinding.FragmentBalanceBinding;
import com.google.android.material.chip.Chip;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class BalanceFragment extends Fragment {

    private FragmentBalanceBinding binding;
    private DataManager dataManager;
    private PlayerAdapter captainAdapter;
    private final List<Player> availablePlayers = new ArrayList<>();
    private final List<Player> filteredPlayers = new ArrayList<>();
    private Club currentClub;
    private LiveData<List<Player>> observedPlayersLiveData;
    private final Observer<List<Player>> playersObserver = players -> {
        if (players == null || binding == null) return;
        availablePlayers.clear();
        availablePlayers.addAll(players.stream()
                .filter(p -> p.isAvailable)
                .collect(Collectors.toList()));
        binding.txtAvailableCount.setText("Available Players: " + availablePlayers.size());
        filter(binding.editSearch.getText().toString());
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBalanceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dataManager = new DataManager(requireContext());
        
        setupSportSpinner();
        setupCaptainSelection();
        setupSearch();
        observeData();

        binding.btnGenerate.setOnClickListener(v -> generateTeams());
    }

    private void setupSportSpinner() {
        String[] sports = {"Cricket", "Football", "Basketball", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, sports);
        binding.spinnerSport.setAdapter(adapter);
    }

    private void observeData() {
        dataManager.getCurrentClub().observe(getViewLifecycleOwner(), club -> {
            if (club != null) {
                currentClub = club;
                binding.txtCurrentClub.setText("Current Club: " + club.name);
                setupTeamSelection();
                observePlayers(club.id);
            }
        });
    }

    private void observePlayers(int clubId) {
        if (observedPlayersLiveData != null) {
            observedPlayersLiveData.removeObserver(playersObserver);
        }
        observedPlayersLiveData = dataManager.getPlayersForClub(clubId);
        observedPlayersLiveData.observe(getViewLifecycleOwner(), playersObserver);
    }

    private void setupTeamSelection() {
        if (currentClub != null && currentClub.teamNames != null && !currentClub.teamNames.isEmpty()) {
            binding.txtSavedTeamsLabel.setVisibility(View.VISIBLE);
            binding.chipGroupTeams.setVisibility(View.VISIBLE);
            binding.layoutTeamNames.setVisibility(View.GONE);
            
            binding.chipGroupTeams.removeAllViews();
            for (String teamName : currentClub.teamNames) {
                Chip chip = new Chip(requireContext());
                chip.setText(teamName);
                chip.setCheckable(true);
                chip.setChecked(true);
                chip.setOnClickListener(v -> updateAdapterTeamNames());
                binding.chipGroupTeams.addView(chip);
            }
        } else {
            binding.txtSavedTeamsLabel.setVisibility(View.GONE);
            binding.chipGroupTeams.setVisibility(View.GONE);
            binding.layoutTeamNames.setVisibility(View.VISIBLE);
        }
        updateAdapterTeamNames();
    }

    private void updateAdapterTeamNames() {
        if (captainAdapter != null) {
            captainAdapter.setTeamNames(getSelectedTeamNames());
        }
    }

    private List<String> getSelectedTeamNames() {
        List<String> teamNamesList = new ArrayList<>();
        if (currentClub != null && currentClub.teamNames != null && !currentClub.teamNames.isEmpty()) {
            for (int i = 0; i < binding.chipGroupTeams.getChildCount(); i++) {
                Chip chip = (Chip) binding.chipGroupTeams.getChildAt(i);
                if (chip.isChecked()) {
                    teamNamesList.add(chip.getText().toString());
                }
            }
        } else {
            String teamNamesText = binding.editTeamNames.getText().toString().trim();
            if (!teamNamesText.isEmpty()) {
                String[] names = teamNamesText.split(",");
                for (String name : names) {
                    if (!name.trim().isEmpty()) teamNamesList.add(name.trim());
                }
            }
        }
        return teamNamesList;
    }

    private void setupCaptainSelection() {
        captainAdapter = new PlayerAdapter(new PlayerAdapter.OnPlayerActionListener() {
            @Override
            public void onPlayerDelete(Player player) {
                dataManager.deletePlayer(player);
            }

            @Override
            public void onPlayerAvailabilityChanged(Player player, boolean isAvailable) {
                player.isAvailable = isAvailable;
                dataManager.updatePlayer(player);
            }

            @Override
            public void onPlayerCaptaincyChanged(Player player, boolean isCaptain) {
                player.isCaptain = isCaptain;
                if (!isCaptain) player.assignedTeam = null;
                dataManager.updatePlayer(player);
                
                // Refresh the list immediately to show/hide the team assignment button
                filter(binding.editSearch.getText().toString());
            }

            @Override
            public void onPlayerEdit(Player player) {
                showEditPlayerDialog(player);
            }

            @Override
            public void onTeamAssigned(Player player, String teamName) {
                showAssignTeamDialog(player);
            }
        });
        
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        binding.recyclerCaptainSelection.setLayoutManager(lm);
        binding.recyclerCaptainSelection.setAdapter(captainAdapter);
        
        binding.editTeamNames.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateAdapterTeamNames();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void showAssignTeamDialog(Player player) {
        List<String> teamNames = getSelectedTeamNames();
        if (teamNames.isEmpty()) {
            Toast.makeText(requireContext(), "Please define team names first", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = teamNames.toArray(new String[0]);
        new AlertDialog.Builder(requireContext())
                .setTitle("Assign " + player.name + " to Team:")
                .setItems(options, (dialog, which) -> {
                    player.assignedTeam = options[which];
                    captainAdapter.notifyDataSetChanged();
                })
                .setNeutralButton("Clear Assignment", (dialog, which) -> {
                    player.assignedTeam = null;
                    captainAdapter.notifyDataSetChanged();
                })
                .show();
    }

    private void setupSearch() {
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filter(String query) {
        List<Player> newFilteredList = new ArrayList<>();
        String lowerQuery = query.toLowerCase().trim();
        
        if (lowerQuery.isEmpty()) {
            // Show ONLY current captains when not searching
            for (Player player : availablePlayers) {
                if (player.isCaptain) {
                    newFilteredList.add(player);
                }
            }
        } else {
            // Show all available players matching the search query
            for (Player player : availablePlayers) {
                if (player.name.toLowerCase().contains(lowerQuery)) {
                    newFilteredList.add(player);
                }
            }
        }
        
        filteredPlayers.clear();
        filteredPlayers.addAll(newFilteredList);
        captainAdapter.updatePlayers(newFilteredList);
    }

    private void showEditPlayerDialog(Player player) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_player, null);
        AutoCompleteTextView editStyle = dialogView.findViewById(R.id.editStyle);
        AutoCompleteTextView editCategory = dialogView.findViewById(R.id.editCategory);

        Player.Style[] styles = Player.Style.values();
        editStyle.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.item_simple_list, styles));
        editStyle.setText(player.style.toString(), false);

        Player.Category[] categories = Player.Category.values();
        List<String> categoryNames = new ArrayList<>();
        for (Player.Category cat : categories) categoryNames.add(cat.displayName);
        editCategory.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.item_simple_list, categoryNames));
        editCategory.setText(player.category.displayName, false);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit " + player.name)
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        player.style = findStyleByDisplayName(editStyle.getText().toString());
                        player.category = findCategoryByDisplayName(editCategory.getText().toString());
                        dataManager.updatePlayer(player);
                        Toast.makeText(requireContext(), "Player updated", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Error updating player", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private Player.Category findCategoryByDisplayName(String displayName) {
        for (Player.Category cat : Player.Category.values()) {
            if (cat.displayName.equals(displayName)) return cat;
        }
        return Player.Category.REGULAR;
    }

    private Player.Style findStyleByDisplayName(String displayName) {
        for (Player.Style s : Player.Style.values()) {
            if (s.displayName.equals(displayName)) return s;
        }
        return Player.Style.BATSMAN;
    }

    private void generateTeams() {
        if (availablePlayers.isEmpty()) {
            Toast.makeText(requireContext(), "No players marked as available", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> selectedTeamNames = getSelectedTeamNames();
        int numTeams = selectedTeamNames.size();
        if (numTeams < 2) {
            Toast.makeText(requireContext(), "Please select at least 2 teams", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Player> captains = availablePlayers.stream().filter(p -> p.isCaptain).collect(Collectors.toList());
        if (captains.size() != numTeams) {
            Toast.makeText(requireContext(), "Selected " + numTeams + " teams but have " + captains.size() + " captains. They must match.", Toast.LENGTH_LONG).show();
            return;
        }

        List<Team> teams = balanceTeams(availablePlayers, selectedTeamNames);
        String selectedSport = binding.spinnerSport.getText().toString();

        // Save to History
        if (currentClub != null) {
            String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(new Date());
            Club.TeamHistory history = new Club.TeamHistory(date, teams, selectedSport);
            currentClub.history.add(history);
            dataManager.updateClub(currentClub);
        }

        // Navigate to Results
        ResultsFragment resultsFragment = ResultsFragment.newInstance(currentClub.id, currentClub.name, new ArrayList<>(teams));
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, resultsFragment)
                .addToBackStack(null)
                .commit();
    }

    private List<Team> balanceTeams(List<Player> allPlayers, List<String> teamNames) {
        int numTeams = teamNames.size();
        List<List<Player>> teamLists = new ArrayList<>();
        int[] teamStrengths = new int[numTeams];
        for (int i = 0; i < numTeams; i++) teamLists.add(new ArrayList<>());

        List<Player> captains = allPlayers.stream().filter(p -> p.isCaptain).collect(Collectors.toList());
        List<Player> regulars = allPlayers.stream().filter(p -> !p.isCaptain).collect(Collectors.toList());

        // Assign captains to their preferred teams or distribute them
        Collections.shuffle(captains);
        List<String> remainingTeamNames = new ArrayList<>(teamNames);
        
        // First pass: assign captains who already have an assigned team
        for (Player captain : captains) {
            if (captain.assignedTeam != null && remainingTeamNames.contains(captain.assignedTeam)) {
                int teamIndex = teamNames.indexOf(captain.assignedTeam);
                teamLists.get(teamIndex).add(captain);
                teamStrengths[teamIndex] += captain.getPower();
                remainingTeamNames.remove(captain.assignedTeam);
                captain.isProcessed = true;
            } else {
                captain.isProcessed = false;
            }
        }

        // Second pass: assign remaining captains to remaining teams
        for (Player captain : captains) {
            if (!captain.isProcessed) {
                String teamName = remainingTeamNames.remove(0);
                int teamIndex = teamNames.indexOf(teamName);
                teamLists.get(teamIndex).add(captain);
                teamStrengths[teamIndex] += captain.getPower();
            }
        }

        // Balance regulars based on power
        Map<Integer, List<Player>> powerGroups = regulars.stream()
                .collect(Collectors.groupingBy(Player::getPower));

        List<Integer> sortedPowerLevels = new ArrayList<>(powerGroups.keySet());
        Collections.sort(sortedPowerLevels, Collections.reverseOrder());

        for (int power : sortedPowerLevels) {
            List<Player> group = powerGroups.get(power);
            Collections.shuffle(group);
            for (Player player : group) {
                int weakestTeamIndex = 0;
                for (int i = 1; i < numTeams; i++) {
                    if (teamStrengths[i] < teamStrengths[weakestTeamIndex]) {
                        weakestTeamIndex = i;
                    } else if (teamStrengths[i] == teamStrengths[weakestTeamIndex]) {
                        if (teamLists.get(i).size() < teamLists.get(weakestTeamIndex).size()) {
                            weakestTeamIndex = i;
                        }
                    }
                }
                teamLists.get(weakestTeamIndex).add(player);
                teamStrengths[weakestTeamIndex] += player.getPower();
            }
        }

        List<Team> teamsRes = new ArrayList<>();
        for (int i = 0; i < numTeams; i++) {
            teamsRes.add(new Team(teamNames.get(i), teamLists.get(i), teamStrengths[i]));
        }
        return teamsRes;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

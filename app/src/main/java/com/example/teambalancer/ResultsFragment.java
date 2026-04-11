package com.example.teambalancer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.teambalancer.databinding.FragmentResultsBinding;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ResultsFragment extends Fragment {

    private static final String ARG_CLUB_ID = "club_id";
    private static final String ARG_CLUB_NAME = "club_name";
    private static final String ARG_TEAMS = "teams";

    private FragmentResultsBinding binding;
    private ArrayList<Team> teams;
    private TeamAdapter teamAdapter;
    private DataManager dataManager;
    private Club currentClub;
    private int clubId;

    public static ResultsFragment newInstance(int clubId, String clubName, ArrayList<Team> teams) {
        ResultsFragment fragment = new ResultsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CLUB_ID, clubId);
        args.putString(ARG_CLUB_NAME, clubName);
        args.putSerializable(ARG_TEAMS, teams);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentResultsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dataManager = new DataManager(requireContext());

        clubId = getArguments().getInt(ARG_CLUB_ID);
        String clubName = getArguments().getString(ARG_CLUB_NAME);
        teams = (ArrayList<Team>) getArguments().getSerializable(ARG_TEAMS);

        binding.txtResultClubName.setText(clubName.isEmpty() ? "BALANCED TEAMS" : clubName.toUpperCase());
        
        setupTeamRecyclerView();

        dataManager.getClubById(clubId).observe(getViewLifecycleOwner(), club -> {
            if (club != null) {
                currentClub = club;
            }
        });

        binding.swipeRefresh.setColorSchemeColors(
                ContextCompat.getColor(requireContext(), R.color.accent),
                ContextCompat.getColor(requireContext(), R.color.secondary));

        binding.swipeRefresh.setOnRefreshListener(() -> {
            regenerateTeams();
            binding.swipeRefresh.setRefreshing(false);
        });

        binding.btnRegenerate.setOnClickListener(v -> regenerateTeams());
        
        binding.btnSaveChanges.setOnClickListener(v -> {
            saveChangesToHistory(false);
            binding.btnSaveChanges.setVisibility(View.GONE);
            binding.btnRegenerate.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Changes Saved!", Toast.LENGTH_SHORT).show();
        });

        binding.btnBack.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        });
    }

    private void setupTeamRecyclerView() {
        teamAdapter = new TeamAdapter(teams);
        teamAdapter.setOnTeamModifiedListener(new TeamAdapter.OnTeamModifiedListener() {
            @Override
            public void onTeamModified() {
                binding.btnSaveChanges.setVisibility(View.VISIBLE);
                binding.btnRegenerate.setVisibility(View.VISIBLE);
            }

            @Override
            public void onMovePlayer(Player player, Team fromTeam) {
                showMovePlayerDialog(player, fromTeam);
            }
        });

        binding.recyclerTeams.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.recyclerTeams.setAdapter(teamAdapter);
    }

    private void showMovePlayerDialog(Player player, Team fromTeam) {
        List<String> teamNames = teams.stream()
                .filter(t -> !t.equals(fromTeam))
                .map(t -> t.name)
                .collect(Collectors.toList());

        String[] options = teamNames.toArray(new String[0]);

        new AlertDialog.Builder(requireContext())
                .setTitle("Move " + player.name + " to:")
                .setItems(options, (dialog, which) -> {
                    String targetTeamName = options[which];
                    Team targetTeam = teams.stream()
                            .filter(t -> t.name.equals(targetTeamName))
                            .findFirst()
                            .orElse(null);

                    if (targetTeam != null) {
                        movePlayer(player, fromTeam, targetTeam);
                    }
                })
                .show();
    }

    private void movePlayer(Player player, Team fromTeam, Team targetTeam) {
        fromTeam.players.remove(player);
        targetTeam.players.add(player);
        
        fromTeam.totalStrength = fromTeam.players.stream().mapToInt(Player::getPower).sum();
        targetTeam.totalStrength = targetTeam.players.stream().mapToInt(Player::getPower).sum();

        teamAdapter.notifyDataSetChanged();
        binding.btnSaveChanges.setVisibility(View.VISIBLE);
        binding.btnRegenerate.setVisibility(View.VISIBLE);
        
        Toast.makeText(requireContext(), player.name + " moved to " + targetTeam.name, Toast.LENGTH_SHORT).show();
    }

    private void regenerateTeams() {
        List<Player> allPlayers = new ArrayList<>();
        for (Team team : teams) {
            allPlayers.addAll(team.players);
        }

        long captainCount = allPlayers.stream().filter(p -> p.isCaptain).count();
        if (captainCount != teams.size()) {
            Toast.makeText(requireContext(), "Please ensure exactly " + teams.size() + " captains are selected", Toast.LENGTH_LONG).show();
            return;
        }

        String[] teamNames = teams.stream().map(t -> t.name).toArray(String[]::new);
        List<Team> newTeams = balanceTeams(allPlayers, teamNames);
        
        teams.clear();
        teams.addAll(newTeams);
        teamAdapter.notifyDataSetChanged();

        binding.btnSaveChanges.setVisibility(View.VISIBLE);
        binding.btnRegenerate.setVisibility(View.VISIBLE);
        
        saveChangesToHistory(true);
        Toast.makeText(requireContext(), "Teams Regenerated!", Toast.LENGTH_SHORT).show();
    }

    private void saveChangesToHistory(boolean forceRegenerateMatches) {
        if (currentClub != null && !currentClub.history.isEmpty()) {
            int lastIndex = currentClub.history.size() - 1;
            Club.TeamHistory latest = currentClub.history.get(lastIndex);
            
            latest.teams = new ArrayList<>(teams);
            
            if (forceRegenerateMatches || latest.matches == null || latest.matches.isEmpty()) {
                latest.matches = new ArrayList<>();
            } else {
                for (Match m : latest.matches) {
                    Team t1 = teams.stream().filter(t -> t.name.equals(m.team1)).findFirst().orElse(null);
                    Team t2 = teams.stream().filter(t -> t.name.equals(m.team2)).findFirst().orElse(null);
                    if (t1 != null) m.squad1 = t1.players.stream().map(p -> p.name).collect(Collectors.toList());
                    if (t2 != null) m.squad2 = t2.players.stream().map(p -> p.name).collect(Collectors.toList());
                }
            }
            
            dataManager.updateClub(currentClub);
        }
    }

    private List<Team> balanceTeams(List<Player> allPlayers, String[] teamNames) {
        int numTeams = teamNames.length;
        List<List<Player>> teamLists = new ArrayList<>();
        int[] teamStrengths = new int[numTeams];
        for (int i = 0; i < numTeams; i++) teamLists.add(new ArrayList<>());

        List<Player> captains = allPlayers.stream().filter(p -> p.isCaptain).collect(Collectors.toList());
        List<Player> regulars = allPlayers.stream().filter(p -> !p.isCaptain).collect(Collectors.toList());

        Collections.shuffle(captains);
        for (int i = 0; i < captains.size(); i++) {
            int teamIndex = i % numTeams;
            teamLists.get(teamIndex).add(captains.get(i));
            teamStrengths[teamIndex] += captains.get(i).getPower();
        }

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
            teamsRes.add(new Team(teamNames[i], teamLists.get(i), teamStrengths[i]));
        }
        return teamsRes;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

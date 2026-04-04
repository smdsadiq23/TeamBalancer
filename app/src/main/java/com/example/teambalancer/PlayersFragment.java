package com.example.teambalancer;

import android.os.Bundle;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.teambalancer.databinding.FragmentPlayersBinding;
import java.util.ArrayList;
import java.util.List;

public class PlayersFragment extends Fragment {

    private FragmentPlayersBinding binding;
    private DataManager dataManager;
    private PlayerAdapter adapter;
    private final List<Player> playersList = new ArrayList<>();
    private Club currentClub;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPlayersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dataManager = new DataManager(requireContext());

        setupDropdowns();
        setupRecyclerView();
        observeData();
        setupListeners();
    }

    private void setupDropdowns() {
        String[] styles = {"Batsman", "Bowler", "All-rounder"};
        ArrayAdapter<String> styleAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_simple_list, styles);
        binding.autoCompleteStyle.setAdapter(styleAdapter);
        binding.autoCompleteStyle.setText(styles[0], false);

        Player.Category[] categories = Player.Category.values();
        List<String> categoryNames = new ArrayList<>();
        for (Player.Category cat : categories) categoryNames.add(cat.displayName);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_simple_list, categoryNames);
        binding.autoCompleteCategory.setAdapter(categoryAdapter);
        binding.autoCompleteCategory.setText(categories[0].displayName, false);

        String[] ratings = {"1", "2", "3", "4"};
        ArrayAdapter<String> ratingAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_simple_list, ratings);
        binding.autoCompleteBatting.setAdapter(ratingAdapter);
        binding.autoCompleteBowling.setAdapter(ratingAdapter);
        binding.autoCompleteFielding.setAdapter(ratingAdapter);
        
        binding.autoCompleteBatting.setText("1", false);
        binding.autoCompleteBowling.setText("1", false);
        binding.autoCompleteFielding.setText("1", false);
    }

    private void setupRecyclerView() {
        adapter = new PlayerAdapter(playersList, new PlayerAdapter.OnPlayerActionListener() {
            @Override
            public void onPlayerDelete(int position) {
                dataManager.deletePlayer(playersList.get(position));
            }

            @Override
            public void onPlayerAvailabilityChanged(int position, boolean isAvailable) {
                Player player = playersList.get(position);
                player.isAvailable = isAvailable;
                dataManager.updatePlayer(player);
            }

            @Override
            public void onPlayerCaptaincyChanged(int position, boolean isCaptain) {
                Player player = playersList.get(position);
                player.isCaptain = isCaptain;
                dataManager.updatePlayer(player);
            }

            @Override
            public void onPlayerEdit(int position, Player player) {
                showEditPlayerDialog(player);
            }
        });

        binding.recyclerPlayers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerPlayers.setAdapter(adapter);
    }

    private void observeData() {
        dataManager.getClubs().observe(getViewLifecycleOwner(), clubs -> {
            if (clubs != null && !clubs.isEmpty()) {
                List<String> names = new ArrayList<>();
                for (Club c : clubs) names.add(c.name);
                ArrayAdapter<String> clubAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_simple_list, names);
                binding.editClubName.setAdapter(clubAdapter);
            }
        });

        dataManager.getCurrentClub().observe(getViewLifecycleOwner(), club -> {
            if (club != null) {
                currentClub = club;
                binding.editClubName.setText(club.name, false);
                observePlayers(club.id);
            }
        });
    }

    private void observePlayers(int clubId) {
        dataManager.getPlayersForClub(clubId).observe(getViewLifecycleOwner(), players -> {
            playersList.clear();
            playersList.addAll(players);
            binding.txtPlayerCount.setText("Total: " + players.size());
            adapter.notifyDataSetChanged();
        });
    }

    private void setupListeners() {
        binding.editClubName.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            dataManager.setCurrentClub(selected);
        });

        binding.btnAddPlayer.setOnClickListener(v -> {
            String name = binding.editPlayerName.getText().toString().trim();
            if (name.isEmpty() || currentClub == null) {
                Toast.makeText(requireContext(), "Enter name and select club", Toast.LENGTH_SHORT).show();
                return;
            }

            Player.Style style = Player.Style.BATSMAN;
            String styleStr = binding.autoCompleteStyle.getText().toString();
            if (styleStr.equals("Bowler")) style = Player.Style.BOWLER;
            else if (styleStr.equals("All-rounder")) style = Player.Style.ALL_ROUNDER;

            Player.Category category = Player.Category.REGULAR;
            String categoryStr = binding.autoCompleteCategory.getText().toString();
            for (Player.Category cat : Player.Category.values()) {
                if (cat.displayName.equals(categoryStr)) {
                    category = cat;
                    break;
                }
            }

            int batting = Integer.parseInt(binding.autoCompleteBatting.getText().toString());
            int bowling = Integer.parseInt(binding.autoCompleteBowling.getText().toString());
            int fielding = Integer.parseInt(binding.autoCompleteFielding.getText().toString());
            boolean isCaptain = binding.checkIsCaptain.isChecked();

            Player player = new Player(name, style, category, batting, bowling, fielding, isCaptain);
            player.clubId = currentClub.id;
            dataManager.addPlayer(player);

            binding.editPlayerName.setText("");
            binding.checkIsCaptain.setChecked(false);
            Toast.makeText(requireContext(), "Player added", Toast.LENGTH_SHORT).show();
        });
    }

    private void showEditPlayerDialog(Player player) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_player, null);
        AutoCompleteTextView editStyle = dialogView.findViewById(R.id.editStyle);
        AutoCompleteTextView editCategory = dialogView.findViewById(R.id.editCategory);
        AutoCompleteTextView editBatting = dialogView.findViewById(R.id.editBatting);
        AutoCompleteTextView editBowling = dialogView.findViewById(R.id.editBowling);
        AutoCompleteTextView editFielding = dialogView.findViewById(R.id.editFielding);

        String[] styles = {"Batsman", "Bowler", "All-rounder"};
        editStyle.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.item_simple_list, styles));
        editStyle.setText(player.style.toString(), false);

        Player.Category[] categories = Player.Category.values();
        List<String> categoryNames = new ArrayList<>();
        for (Player.Category cat : categories) categoryNames.add(cat.displayName);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_simple_list, categoryNames);
        editCategory.setAdapter(categoryAdapter);
        editCategory.setText(player.category != null ? player.category.displayName : Player.Category.REGULAR.displayName, false);

        String[] ratings = {"1", "2", "3", "4"};
        ArrayAdapter<String> ratingAdapter = new ArrayAdapter<>(requireContext(), R.layout.item_simple_list, ratings);
        editBatting.setAdapter(ratingAdapter);
        editBowling.setAdapter(ratingAdapter);
        editFielding.setAdapter(ratingAdapter);

        editBatting.setText(String.valueOf(player.battingRating), false);
        editBowling.setText(String.valueOf(player.bowlingRating), false);
        editFielding.setText(String.valueOf(player.fieldingRating), false);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit " + player.name)
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String styleStr = editStyle.getText().toString();
                    if (styleStr.equals("Bowler")) player.style = Player.Style.BOWLER;
                    else if (styleStr.equals("All-rounder")) player.style = Player.Style.ALL_ROUNDER;
                    else player.style = Player.Style.BATSMAN;

                    String categoryStr = editCategory.getText().toString();
                    for (Player.Category cat : Player.Category.values()) {
                        if (cat.displayName.equals(categoryStr)) {
                            player.category = cat;
                            break;
                        }
                    }

                    player.battingRating = Integer.parseInt(editBatting.getText().toString());
                    player.bowlingRating = Integer.parseInt(editBowling.getText().toString());
                    player.fieldingRating = Integer.parseInt(editFielding.getText().toString());

                    dataManager.updatePlayer(player);
                    Toast.makeText(requireContext(), "Player updated", Toast.LENGTH_SHORT).show();
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

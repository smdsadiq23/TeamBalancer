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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import com.example.teambalancer.databinding.FragmentPlayerListBinding;
import java.util.ArrayList;
import java.util.List;

public class PlayerListFragment extends Fragment {

    private static final String ARG_CLUB_ID = "club_id";
    private static final String ARG_CLUB_NAME = "club_name";

    private FragmentPlayerListBinding binding;
    private DataManager dataManager;
    private PlayerAdapter adapter;
    private int clubId;
    private String clubName;
    private LiveData<List<Player>> observedPlayersLiveData;
    
    private final Observer<List<Player>> playersObserver = players -> {
        if (players == null || binding == null) return;
        adapter.updatePlayers(players);
        binding.txtPlayerCount.setText("Total: " + players.size());
    };

    public static PlayerListFragment newInstance(int clubId, String clubName) {
        PlayerListFragment fragment = new PlayerListFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CLUB_ID, clubId);
        args.putString(ARG_CLUB_NAME, clubName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            clubId = getArguments().getInt(ARG_CLUB_ID);
            clubName = getArguments().getString(ARG_CLUB_NAME);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPlayerListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dataManager = new DataManager(requireContext());
        
        binding.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.txtClubName.setText(clubName);

        setupRecyclerView();
        observePlayers();
    }

    private void setupRecyclerView() {
        adapter = new PlayerAdapter(new PlayerAdapter.OnPlayerActionListener() {
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
                dataManager.updatePlayer(player);
            }

            @Override
            public void onPlayerEdit(Player player) {
                showEditPlayerDialog(player);
            }
        });

        binding.recyclerPlayers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerPlayers.setAdapter(adapter);
        binding.recyclerPlayers.setHasFixedSize(true);
        
        RecyclerView.ItemAnimator animator = binding.recyclerPlayers.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
    }

    private void observePlayers() {
        if (observedPlayersLiveData != null) {
            observedPlayersLiveData.removeObserver(playersObserver);
        }
        observedPlayersLiveData = dataManager.getPlayersForClub(clubId);
        observedPlayersLiveData.observe(getViewLifecycleOwner(), playersObserver);
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

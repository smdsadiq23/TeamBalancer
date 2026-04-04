package com.example.teambalancer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.teambalancer.databinding.FragmentPlayersBinding;
import java.util.ArrayList;
import java.util.List;

public class PlayersFragment extends Fragment {

    private FragmentPlayersBinding binding;
    private DataManager dataManager;
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
            }
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

        binding.btnViewPlayerList.setOnClickListener(v -> {
            if (currentClub != null) {
                PlayerListFragment listFragment = PlayerListFragment.newInstance(currentClub.id, currentClub.name);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, listFragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(requireContext(), "Select a club first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

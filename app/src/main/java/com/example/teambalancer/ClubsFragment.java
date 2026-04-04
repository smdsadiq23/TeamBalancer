package com.example.teambalancer;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.teambalancer.databinding.FragmentClubsBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClubsFragment extends Fragment {

    private FragmentClubsBinding binding;
    private DataManager dataManager;
    private ClubAdapter clubAdapter;
    private final List<ClubWithPlayers> clubsList = new ArrayList<>();
    private String currentClubName = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentClubsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dataManager = new DataManager(requireContext());
        
        setupRecyclerView();
        observeData();
        setupListeners();
    }

    private void setupRecyclerView() {
        clubAdapter = new ClubAdapter(clubsList, new ClubAdapter.OnClubActionListener() {
            @Override
            public void onClubSelect(Club club) {
                dataManager.setCurrentClub(club.name);
                currentClubName = club.name;
                clubAdapter.setSelectedClubName(currentClubName);
                Toast.makeText(requireContext(), "Selected: " + club.name, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onClubDelete(int position) {
                if (clubsList.size() <= 1) {
                    Toast.makeText(requireContext(), "Must have at least one club", Toast.LENGTH_SHORT).show();
                    return;
                }

                Club clubToDelete = clubsList.get(position).club;
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Club")
                        .setMessage("Are you sure you want to delete '" + clubToDelete.name + "' and all its history?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            dataManager.deleteClub(clubToDelete);
                            if (clubToDelete.name.equals(currentClubName)) {
                                Club nextClub = position == 0 ? clubsList.get(1).club : clubsList.get(0).club;
                                dataManager.setCurrentClub(nextClub.name);
                                currentClubName = nextClub.name;
                                clubAdapter.setSelectedClubName(currentClubName);
                            }
                            Toast.makeText(requireContext(), "Club deleted", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            @Override
            public void onShowPlayers(List<Player> players, String clubName) {
                if (players == null || players.isEmpty()) {
                    Toast.makeText(requireContext(), "No players in this club", Toast.LENGTH_SHORT).show();
                    return;
                }

                List<String> playerNames = players.stream()
                        .map(p -> p.name + (p.isCaptain ? " (C)" : ""))
                        .sorted()
                        .collect(Collectors.toList());

                ListView listView = new ListView(requireContext());
                listView.setPadding(32, 32, 32, 32);
                
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(),
                        android.R.layout.simple_list_item_1, playerNames) {
                    @NonNull
                    @Override
                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        TextView textView = view.findViewById(android.R.id.text1);
                        textView.setTextColor(Color.WHITE);
                        return view;
                    }
                };
                listView.setAdapter(adapter);

                new AlertDialog.Builder(requireContext())
                        .setTitle(clubName + " - Player List")
                        .setView(listView)
                        .setPositiveButton("Close", null)
                        .show();
            }
        });

        binding.recyclerClubs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerClubs.setAdapter(clubAdapter);
    }

    private void observeData() {
        dataManager.getClubsWithPlayers().observe(getViewLifecycleOwner(), clubs -> {
            clubsList.clear();
            clubsList.addAll(clubs);
            clubAdapter.setSelectedClubName(currentClubName);
        });

        dataManager.getCurrentClub().observe(getViewLifecycleOwner(), club -> {
            if (club != null && currentClubName.isEmpty()) {
                currentClubName = club.name;
                clubAdapter.setSelectedClubName(currentClubName);
            }
        });
    }

    private void setupListeners() {
        binding.btnCreateClub.setOnClickListener(v -> {
            String clubName = binding.editNewClubName.getText().toString().trim();
            if (clubName.isEmpty()) {
                Toast.makeText(requireContext(), "Enter club name", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean exists = clubsList.stream()
                    .anyMatch(c -> c.club.name.equalsIgnoreCase(clubName));
            
            if (exists) {
                Toast.makeText(requireContext(), "Club already exists", Toast.LENGTH_SHORT).show();
                return;
            }

            Club newClub = new Club(clubName);
            dataManager.addClub(newClub);
            binding.editNewClubName.setText("");
            Toast.makeText(requireContext(), "Club created", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

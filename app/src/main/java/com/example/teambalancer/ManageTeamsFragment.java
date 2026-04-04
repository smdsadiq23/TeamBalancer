package com.example.teambalancer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.teambalancer.databinding.FragmentManageTeamsBinding;
import java.util.ArrayList;
import java.util.List;

public class ManageTeamsFragment extends Fragment {

    private FragmentManageTeamsBinding binding;
    private DataManager dataManager;
    private SimpleListAdapter adapter;
    private Club currentClub;
    private final List<String> teamNames = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentManageTeamsBinding.inflate(inflater, container, false);
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
        adapter = new SimpleListAdapter(teamNames, new SimpleListAdapter.OnItemActionListener() {
            @Override
            public void onItemDelete(int position) {
                if (currentClub != null) {
                    currentClub.teamNames.remove(position);
                    dataManager.updateClub(currentClub);
                }
            }

            @Override
            public void onItemSelect(int position) {
                adapter.setSelectedPosition(position);
                Toast.makeText(requireContext(), "Selected: " + teamNames.get(position), Toast.LENGTH_SHORT).show();
            }
        }, requireContext());

        binding.recyclerTeams.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTeams.setAdapter(adapter);
    }

    private void observeData() {
        dataManager.getCurrentClub().observe(getViewLifecycleOwner(), club -> {
            if (club != null) {
                currentClub = club;
                binding.toolbar.setTitle("Teams: " + club.name);
                teamNames.clear();
                if (club.teamNames != null) {
                    teamNames.addAll(club.teamNames);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void setupListeners() {
        binding.btnAddTeam.setOnClickListener(v -> {
            String teamName = binding.editNewTeamName.getText().toString().trim();
            if (teamName.isEmpty()) {
                Toast.makeText(requireContext(), "Enter team name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentClub != null) {
                if (currentClub.teamNames.contains(teamName)) {
                    Toast.makeText(requireContext(), "Team already exists", Toast.LENGTH_SHORT).show();
                    return;
                }

                currentClub.teamNames.add(teamName);
                dataManager.updateClub(currentClub);
                binding.editNewTeamName.setText("");
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

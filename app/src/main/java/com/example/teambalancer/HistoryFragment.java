package com.example.teambalancer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.teambalancer.databinding.FragmentHistoryBinding;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private DataManager dataManager;
    private HistoryAdapter adapter;
    private final List<Club.TeamHistory> historyList = new ArrayList<>();
    private Club currentClub;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dataManager = new DataManager(requireContext());

        setupRecyclerView();
        observeData();
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter(historyList, new HistoryAdapter.OnHistoryActionListener() {
            @Override
            public void onHistoryClick(Club.TeamHistory history) {
                if (currentClub != null) {
                    ResultsFragment resultsFragment = ResultsFragment.newInstance(currentClub.id, currentClub.name, (ArrayList<Team>) history.teams);
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, resultsFragment)
                            .addToBackStack(null)
                            .commit();
                }
            }

            @Override
            public void onHistoryDelete(int position) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete History")
                        .setMessage("Are you sure you want to delete this game record?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            if (currentClub != null) {
                                Club.TeamHistory historyToRemove = historyList.get(position);
                                currentClub.history.remove(historyToRemove);
                                dataManager.updateClub(currentClub);
                                Toast.makeText(requireContext(), "History deleted", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerHistory.setAdapter(adapter);
    }

    private void observeData() {
        dataManager.getCurrentClub().observe(getViewLifecycleOwner(), club -> {
            if (club != null) {
                currentClub = club;
                binding.toolbar.setTitle("History: " + club.name);
                
                historyList.clear();
                historyList.addAll(club.history);
                Collections.reverse(historyList); // Show newest first
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

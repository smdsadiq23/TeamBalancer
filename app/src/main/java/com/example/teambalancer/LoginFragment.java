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
import com.example.teambalancer.databinding.FragmentLoginBinding;
import java.util.ArrayList;
import java.util.List;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private AppDatabase database;
    private DataManager dataManager;
    private List<Club> allClubs = new ArrayList<>();
    private String selectedClubName = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        database = AppDatabase.getInstance(requireContext());
        dataManager = new DataManager(requireContext());

        setupClubSelection();

        binding.btnLogin.setOnClickListener(v -> {
            String username = binding.editUsername.getText().toString().trim();
            String password = binding.editPassword.getText().toString().trim();
            selectedClubName = binding.autoCompleteClub.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedClubName.isEmpty()) {
                Toast.makeText(requireContext(), "Please select or type a club name", Toast.LENGTH_SHORT).show();
                return;
            }

            AppDatabase.databaseWriteExecutor.execute(() -> {
                User user = database.userDao().login(username, password);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (user != null) {
                            dataManager.setLoggedInUser(username); // Save the username
                            handleLoginSuccess(selectedClubName);
                        } else {
                            Toast.makeText(requireContext(), "Invalid credentials", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        });

        binding.btnRegister.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new RegisterFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void handleLoginSuccess(String clubName) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Club existingClub = database.clubDao().getClubByNameSync(clubName);
            if (existingClub == null) {
                // If the user typed a new club name, create it
                database.clubDao().insert(new Club(clubName));
            }
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    dataManager.setCurrentClub(clubName);
                    Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show();
                    navigateToHome();
                });
            }
        });
    }

    private void setupClubSelection() {
        dataManager.getClubs().observe(getViewLifecycleOwner(), clubs -> {
            if (clubs != null) {
                allClubs = clubs;
                List<String> clubNames = new ArrayList<>();
                for (Club club : clubs) {
                    clubNames.add(club.name);
                }
                
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_dropdown_item_1line, clubNames);
                binding.autoCompleteClub.setAdapter(adapter);
                
                // Pre-select the first club if available and nothing is typed yet
                if (!clubNames.isEmpty() && binding.autoCompleteClub.getText().toString().isEmpty()) {
                    binding.autoCompleteClub.setText(clubNames.get(0), false);
                }
            }
        });
    }

    private void navigateToHome() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

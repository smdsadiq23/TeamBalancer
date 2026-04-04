package com.example.teambalancer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import com.example.teambalancer.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private DataManager dataManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        dataManager = new DataManager(requireContext());

        // Display the logged-in username
        binding.txtUsername.setText(dataManager.getLoggedInUser());

        binding.btnUserMenu.setOnClickListener(this::showUserMenu);

        observeCurrentClub();

        binding.cardPlayers.setOnClickListener(v -> navigateTo(new PlayersFragment()));
        binding.cardTeams.setOnClickListener(v -> navigateTo(new ManageTeamsFragment()));
        binding.cardBalance.setOnClickListener(v -> navigateTo(new BalanceFragment()));
        binding.cardHistory.setOnClickListener(v -> navigateTo(new HistoryFragment()));
        binding.cardClubs.setOnClickListener(v -> navigateTo(new ClubsFragment()));
    }

    private void showUserMenu(View v) {
        // Use ContextThemeWrapper to apply white text color to the popup menu
        ContextThemeWrapper wrapper = new ContextThemeWrapper(requireContext(), R.style.Widget_TeamBalancer_PopupMenu);
        PopupMenu popup = new PopupMenu(wrapper, v);
        
        popup.getMenu().add("Manage Users");
        popup.getMenu().add("Logout");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Manage Users")) {
                navigateTo(new ManageUsersFragment());
            } else if (item.getTitle().equals("Logout")) {
                logout();
            }
            return true;
        });
        popup.show();
    }

    private void logout() {
        dataManager.setLoggedInUser("Guest");
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new LoginFragment())
                .commit();
    }

    private void observeCurrentClub() {
        dataManager.getCurrentClub().observe(getViewLifecycleOwner(), currentClub -> {
            if (currentClub != null) {
                binding.txtHomeCurrentClub.setText(currentClub.name);
            } else {
                binding.txtHomeCurrentClub.setText("No Club Selected");
            }
        });
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

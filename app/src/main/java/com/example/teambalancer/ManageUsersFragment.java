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
import com.example.teambalancer.databinding.FragmentManageUsersBinding;
import java.util.ArrayList;
import java.util.List;

public class ManageUsersFragment extends Fragment {

    private FragmentManageUsersBinding binding;
    private AppDatabase database;
    private UserAdapter adapter;
    private final List<User> userList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentManageUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        database = AppDatabase.getInstance(requireContext());

        setupRecyclerView();
        loadUsers();

        binding.toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void setupRecyclerView() {
        adapter = new UserAdapter(userList, user -> {
            if (userList.size() <= 1) {
                Toast.makeText(requireContext(), "Must have at least one user", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete User")
                    .setMessage("Are you sure you want to delete user '" + user.username + "'?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteUser(user))
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        binding.recyclerUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerUsers.setAdapter(adapter);
    }

    private void loadUsers() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<User> users = database.userDao().getAllUsers();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    userList.clear();
                    userList.addAll(users);
                    adapter.notifyDataSetChanged();
                });
            }
        });
    }

    private void deleteUser(User user) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            database.userDao().delete(user);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    userList.remove(user);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(requireContext(), "User deleted", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

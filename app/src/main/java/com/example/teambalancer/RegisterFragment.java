package com.example.teambalancer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.teambalancer.databinding.FragmentRegisterBinding;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private AppDatabase database;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        database = AppDatabase.getInstance(requireContext());

        binding.btnRegisterSubmit.setOnClickListener(v -> {
            String username = binding.editRegUsername.getText().toString().trim();
            String password = binding.editRegPassword.getText().toString().trim();
            String confirmPassword = binding.editRegConfirmPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            AppDatabase.databaseWriteExecutor.execute(() -> {
                User existingUser = database.userDao().findByUsername(username);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (existingUser != null) {
                            Toast.makeText(requireContext(), "Username already exists", Toast.LENGTH_SHORT).show();
                        } else {
                            registerUser(username, password);
                        }
                    });
                }
            });
        });

        binding.btnBackToLogin.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void registerUser(String username, String password) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            database.userDao().register(new User(username, password));
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Registration successful! Please login.", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
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

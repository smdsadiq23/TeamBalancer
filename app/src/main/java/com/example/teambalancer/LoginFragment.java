package com.example.teambalancer;

import android.content.Context;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    private static final Executor IO = Executors.newSingleThreadExecutor();

    private FragmentLoginBinding binding;
    private AppDatabase database;
    private DataManager dataManager;
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
            String selectedClubName = binding.autoCompleteClub.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedClubName.isEmpty()) {
                Toast.makeText(requireContext(), "Please select or type a club name", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.btnLogin.setEnabled(false);

            final Context appCtx = requireContext().getApplicationContext();

            IO.execute(() -> {
                TeamBalancerApi api = ApiModule.api();
                try {
                    Response<TeamBalancerApi.AuthResponseDto> loginRes =
                            api.login(new TeamBalancerApi.UsernamePasswordBody(username, password)).execute();
                    if (!loginRes.isSuccessful()) {
                        showToast(ApiErrorReader.readMessage(loginRes));
                        return;
                    }
                    TeamBalancerApi.AuthResponseDto auth = loginRes.body();
                    if (auth == null || auth.token == null || auth.token.isEmpty() || auth.user == null) {
                        showToast("Invalid server response");
                        return;
                    }
                    SecureSessionStore.save(appCtx, auth.token, auth.user.id, auth.user.username);

                    Response<TeamBalancerApi.ClubsListResponseDto> listRes = api.listClubs().execute();
                    if (!listRes.isSuccessful()) {
                        SecureSessionStore.clear(appCtx);
                        showToast(ApiErrorReader.readMessage(listRes));
                        return;
                    }
                    TeamBalancerApi.ClubsListResponseDto listBody = listRes.body();
                    TeamBalancerApi.ClubListItemDto[] remoteClubs = listBody != null ? listBody.clubs : null;
                    RemoteClubSync.mergeServerClubsIntoRoom(database, remoteClubs);

                    String clubName =
                            RemoteClubSync.ensureClubExistsOnServer(database, api, remoteClubs, selectedClubName);

                    DataManager dm = new DataManager(appCtx);
                    dm.setLoggedInUser(auth.user.username);
                    dm.setCurrentClub(clubName);

                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show();
                            navigateToHome();
                        });
                    }
                } catch (IOException e) {
                    SecureSessionStore.clear(appCtx);
                    showToast("Network: " + ApiErrorReader.readIoMessage(e));
                } catch (Exception e) {
                    SecureSessionStore.clear(appCtx);
                    String m = e.getMessage();
                    showToast(m != null ? m : "Login failed");
                } finally {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> binding.btnLogin.setEnabled(true));
                    }
                }
            });
        });

        binding.btnRegister.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new RegisterFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void showToast(String msg) {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show());
    }

    private void setupClubSelection() {
        dataManager.getClubs().observe(getViewLifecycleOwner(), clubs -> {
            if (clubs != null) {
                List<String> clubNames = new ArrayList<>();
                for (Club club : clubs) {
                    clubNames.add(club.name);
                }

                ArrayAdapter<String> adapter =
                        new ArrayAdapter<>(requireContext(), R.layout.item_simple_list, clubNames);
                binding.autoCompleteClub.setAdapter(adapter);

                if (!clubNames.isEmpty() && binding.autoCompleteClub.getText().toString().isEmpty()) {
                    binding.autoCompleteClub.setText(clubNames.get(0), false);
                }
            }
        });
    }

    private void navigateToHome() {
        getParentFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

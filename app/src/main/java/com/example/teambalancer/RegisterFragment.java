package com.example.teambalancer;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.teambalancer.databinding.FragmentRegisterBinding;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import retrofit2.Response;

public class RegisterFragment extends Fragment {

    private static final Executor IO = Executors.newSingleThreadExecutor();

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

            binding.btnRegisterSubmit.setEnabled(false);
            final Context appCtx = requireContext().getApplicationContext();

            IO.execute(() -> {
                TeamBalancerApi api = ApiModule.api();
                try {
                    Response<TeamBalancerApi.AuthResponseDto> reg =
                            api.register(new TeamBalancerApi.UsernamePasswordBody(username, password)).execute();
                    if (!reg.isSuccessful()) {
                        toast(ApiErrorReader.readMessage(reg));
                        return;
                    }
                    TeamBalancerApi.AuthResponseDto body = reg.body();
                    if (body == null || body.token == null || body.token.isEmpty() || body.user == null) {
                        toast("Invalid server response");
                        return;
                    }
                    SecureSessionStore.save(appCtx, body.token, body.user.id, body.user.username);

                    Response<TeamBalancerApi.ClubsListResponseDto> listRes = api.listClubs().execute();
                    if (!listRes.isSuccessful()) {
                        SecureSessionStore.clear(appCtx);
                        toast(ApiErrorReader.readMessage(listRes));
                        return;
                    }
                    TeamBalancerApi.ClubsListResponseDto listBody = listRes.body();
                    TeamBalancerApi.ClubListItemDto[] clubs = listBody != null ? listBody.clubs : null;
                    RemoteClubSync.mergeServerClubsIntoRoom(database, clubs);

                    String clubName;
                    if (clubs != null && clubs.length > 0 && clubs[0] != null && clubs[0].name != null) {
                        clubName = clubs[0].name;
                    } else {
                        Response<TeamBalancerApi.ClubCreateResponseDto> createRes =
                                api.createClub(new TeamBalancerApi.CreateClubBody("Default Club")).execute();
                        if (!createRes.isSuccessful()) {
                            SecureSessionStore.clear(appCtx);
                            toast(ApiErrorReader.readMessage(createRes));
                            return;
                        }
                        clubName = "Default Club";
                        TeamBalancerApi.ClubCreateResponseDto cr = createRes.body();
                        Integer newRemoteId =
                                cr != null && cr.club != null ? cr.club.id : null;
                        Club existingDef = database.clubDao().getClubByNameSync(clubName);
                        if (existingDef == null) {
                            Club nc = new Club(clubName);
                            if (newRemoteId != null) {
                                nc.remoteId = newRemoteId;
                            }
                            database.clubDao().insert(nc);
                        } else if (newRemoteId != null) {
                            existingDef.remoteId = newRemoteId;
                            database.clubDao().update(existingDef);
                        }
                    }

                    ClubDao dao = database.clubDao();
                    Club pick = dao.getClubByNameSync(clubName);
                    if (pick == null) {
                        Club nc = new Club(clubName);
                        dao.insert(nc);
                    }
                    DataManager dm = new DataManager(appCtx);
                    dm.setLoggedInUser(body.user.username);
                    dm.setCurrentClub(clubName);

                    if (getActivity() != null) {
                        getActivity()
                                .runOnUiThread(() -> {
                                    Toast.makeText(requireContext(), "Registered and signed in", Toast.LENGTH_SHORT).show();
                                    getParentFragmentManager()
                                            .beginTransaction()
                                            .replace(R.id.fragment_container, new HomeFragment())
                                            .commit();
                                });
                    }
                } catch (IOException e) {
                    SecureSessionStore.clear(appCtx);
                    toast("Network: " + ApiErrorReader.readIoMessage(e));
                } catch (Exception e) {
                    SecureSessionStore.clear(appCtx);
                    toast(e.getMessage() != null ? e.getMessage() : "Registration failed");
                } finally {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> binding.btnRegisterSubmit.setEnabled(true));
                    }
                }
            });
        });

        binding.btnBackToLogin.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void toast(String msg) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

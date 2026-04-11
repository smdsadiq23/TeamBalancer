package com.example.teambalancer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.teambalancer.databinding.FragmentCricketHubBinding;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * Single entry for cricket: scheduled fixtures and live/completed matches (hub + tabs).
 */
public class CricketHubFragment extends Fragment {

    private FragmentCricketHubBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCricketHubBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.toolbarCricketHub.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.viewPagerCricket.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                return position == 0
                        ? FixturesFragment.newInstance(true)
                        : MatchesFragment.newInstance(true);
            }

            @Override
            public int getItemCount() {
                return 2;
            }
        });
        binding.viewPagerCricket.setOffscreenPageLimit(1);

        new TabLayoutMediator(
                binding.tabLayoutCricket,
                binding.viewPagerCricket,
                (tab, position) -> tab.setText(position == 0 ? "Scheduled" : "Live & results")
        ).attach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

package com.example.loyaltyprogram;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.loyaltyprogram.PointsRepository;
import com.example.loyaltyprogram.models.RewardItem;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RewardsFragment extends Fragment {

    private final List<RewardItem> allRewards = new ArrayList<>();
    private final List<RewardItem> featuredRewards = new ArrayList<>();

    private RewardAdapter featuredAdapter;
    private RewardAdapter allAdapter;

    private TextView pointsBalanceView;
    private TextInputEditText searchInput;
    private ChipGroup chipGroup;
    private NestedScrollView scrollView;

    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.getDefault());
    private PointsRepository pointsRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rewards, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pointsRepository = PointsRepository.getInstance(requireContext());
        setupData();
        setupViews(view);
        bindAdapters(view);
        applyFilter("All");
    }

    private void setupViews(View root) {
        pointsBalanceView = root.findViewById(R.id.tvRewardPointsBalance);
        searchInput = root.findViewById(R.id.etSearchRewards);
        chipGroup = root.findViewById(R.id.chipGroupRewards);
        scrollView = root.findViewById(R.id.rewardsScroll);

        int totalPoints = pointsRepository.getPoints();
        pointsBalanceView.setText(numberFormat.format(totalPoints));

        root.findViewById(R.id.tvViewAllFeatured).setOnClickListener(v -> {
            chipGroup.check(R.id.chip_rewards_all);
            scrollView.post(() -> scrollView.smoothScrollTo(0, pointsBalanceView.getBottom()));
        });

        searchInput.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            filterRewards(chipGroup.getCheckedChipId());
            return false;
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // no-op
            }

            @Override
            public void afterTextChanged(Editable s) {
                filterRewards(chipGroup.getCheckedChipId());
            }
        });

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            filterRewards(checkedIds.get(0));
        });
    }

    private void bindAdapters(View root) {
        RecyclerView featuredRecycler = root.findViewById(R.id.rvFeaturedRewards);
        LinearLayoutManager horizontalManager = new LinearLayoutManager(requireContext(),
                LinearLayoutManager.HORIZONTAL, false);
        featuredRecycler.setLayoutManager(horizontalManager);
        featuredAdapter = new RewardAdapter(featuredRewards);
        featuredRecycler.setAdapter(featuredAdapter);

        RecyclerView allRecycler = root.findViewById(R.id.rvAllRewards);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(requireContext(), 2);
        allRecycler.setLayoutManager(gridLayoutManager);
        allAdapter = new RewardAdapter(allRewards);
        allRecycler.setAdapter(allAdapter);
    }

    private void setupData() {
        if (!allRewards.isEmpty()) {
            return;
        }

        featuredRewards.add(new RewardItem(
                "Free handcrafted latte",
                "Choose any medium latte or cappuccino",
                450,
                R.drawable.ic_gift,
                "Drinks"));
        featuredRewards.add(new RewardItem(
                "2-for-1 pastry",
                "Bring a friend and enjoy two pastries for the price of one",
                320,
                R.drawable.ic_gift,
                "Food"));
        featuredRewards.add(new RewardItem(
                "Limited edition mug",
                "Collect our seasonal ceramic mug",
                900,
                R.drawable.ic_star,
                "Merch"));

        allRewards.addAll(featuredRewards);
        allRewards.add(new RewardItem(
                "Iced cold brew upgrade",
                "Upgrade any drink to cold brew",
                250,
                R.drawable.ic_gift,
                "Drinks"));
        allRewards.add(new RewardItem(
                "Breakfast sandwich",
                "Redeem for a fresh breakfast sandwich",
                600,
                R.drawable.ic_gift,
                "Food"));
        allRewards.add(new RewardItem(
                "Bean bag (1lb)",
                "Take home a bag of our signature roast",
                1200,
                R.drawable.ic_history,
                "Merch"));
        allRewards.add(new RewardItem(
                "Extra shot add-on",
                "Add an extra espresso shot to any drink",
                150,
                R.drawable.ic_gift,
                "Drinks"));
    }

    private void filterRewards(int checkedChipId) {
        Chip chip = chipGroup.findViewById(checkedChipId);
        if (chip == null) {
            return;
        }
        applyFilter(chip.getText().toString());
    }

    private void applyFilter(String category) {
        String query = searchInput.getText() != null ? searchInput.getText().toString() : "";
        query = query.trim().toLowerCase(Locale.getDefault());

        List<RewardItem> filtered = new ArrayList<>();
        for (RewardItem item : allRewards) {
            boolean matchesCategory = TextUtils.equals("All", category) ||
                    TextUtils.equals(item.getCategory(), category);
            boolean matchesQuery = TextUtils.isEmpty(query) ||
                    item.getTitle().toLowerCase(Locale.getDefault()).contains(query) ||
                    item.getDescription().toLowerCase(Locale.getDefault()).contains(query);
            if (matchesCategory && matchesQuery) {
                filtered.add(item);
            }
        }
        allAdapter.updateData(filtered);
    }

}

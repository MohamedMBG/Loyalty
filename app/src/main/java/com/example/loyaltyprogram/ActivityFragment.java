package com.example.loyaltyprogram;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.loyaltyprogram.models.UserActivityItem;
import com.google.android.material.chip.ChipGroup;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ActivityFragment extends Fragment {

    private final List<UserActivityItem> allActivity = new ArrayList<>();
    private ActivityAdapter adapter;
    private RecyclerView recyclerView;

    private TextView monthlyPointsView;
    private TextView earnedPointsView;
    private TextView redeemedPointsView;
    private ChipGroup chipGroup;

    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initData();
        setupViews(view);
        setupRecycler(view);
        updateSummaries();
        chipGroup.check(R.id.chip_activity_all);
        filterActivities(R.id.chip_activity_all);
    }

    private void setupViews(View root) {
        monthlyPointsView = root.findViewById(R.id.tvMonthlyPoints);
        earnedPointsView = root.findViewById(R.id.tvEarnedPoints);
        redeemedPointsView = root.findViewById(R.id.tvRedeemedPoints);
        chipGroup = root.findViewById(R.id.chipGroupActivity);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int id = checkedIds.get(0);
            filterActivities(id);
        });
    }

    private void setupRecycler(View root) {
        recyclerView = root.findViewById(R.id.rvActivity);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ActivityAdapter(new ArrayList<>(allActivity));
        recyclerView.setAdapter(adapter);
    }

    private void initData() {
        if (!allActivity.isEmpty()) {
            return;
        }

        allActivity.add(new UserActivityItem("Purchased seasonal latte", "Nov 18", 250, true, R.drawable.ic_star));
        allActivity.add(new UserActivityItem("Weekend double points", "Nov 15", 220, true, R.drawable.ic_home));
        allActivity.add(new UserActivityItem("Birthday bonus", "Nov 12", 150, true, R.drawable.ic_star));
        allActivity.add(new UserActivityItem("Redeemed free pastry", "Nov 08", 80, false, R.drawable.ic_gift));
        allActivity.add(new UserActivityItem("Redeemed mug", "Nov 02", 120, false, R.drawable.ic_history));
    }

    private void updateSummaries() {
        int earned = 0;
        int redeemed = 0;
        for (UserActivityItem item : allActivity) {
            if (item.isPositive()) {
                earned += item.getPointsDelta();
            } else {
                redeemed += item.getPointsDelta();
            }
        }

        int net = earned - redeemed;
        monthlyPointsView.setText(String.format(Locale.getDefault(), "%s pts",
                net >= 0 ? "+" + numberFormat.format(net) : "-" + numberFormat.format(Math.abs(net))));
        earnedPointsView.setText(numberFormat.format(earned));
        redeemedPointsView.setText(numberFormat.format(redeemed));
    }

    private void filterActivities(int checkedId) {
        List<UserActivityItem> filtered = new ArrayList<>();
        for (UserActivityItem item : allActivity) {
            if (checkedId == R.id.chip_activity_all) {
                filtered.add(item);
            } else if (checkedId == R.id.chip_activity_earned && item.isPositive()) {
                filtered.add(item);
            } else if (checkedId == R.id.chip_activity_redeemed && !item.isPositive()) {
                filtered.add(item);
            }
        }
        adapter.updateData(filtered);
        recyclerView.scheduleLayoutAnimation();
    }
}

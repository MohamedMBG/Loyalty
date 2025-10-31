package com.example.loyaltyprogram;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.loyaltyprogram.models.RewardItem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RewardAdapter extends RecyclerView.Adapter<RewardAdapter.RewardViewHolder> {
    private final List<RewardItem> rewards = new ArrayList<>();
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.getDefault());

    public RewardAdapter(List<RewardItem> rewards) {
        if (rewards != null) {
            this.rewards.addAll(rewards);
        }
    }

    @NonNull
    @Override
    public RewardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reward, parent, false);
        return new RewardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RewardViewHolder holder, int position) {
        RewardItem item = rewards.get(position);
        holder.title.setText(item.getTitle());
        holder.description.setText(item.getDescription());
        holder.points.setText(numberFormat.format(item.getPointsCost()) + " pts");
        holder.icon.setImageResource(item.getImageRes());
    }

    @Override
    public int getItemCount() {
        return rewards.size();
    }

    public void updateData(List<RewardItem> newRewards) {
        rewards.clear();
        if (newRewards != null) {
            rewards.addAll(newRewards);
        }
        notifyDataSetChanged();
    }

    static class RewardViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView description;
        final TextView points;

        RewardViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.ivReward);
            title = itemView.findViewById(R.id.tvRewardTitle);
            description = itemView.findViewById(R.id.tvRewardDescription);
            points = itemView.findViewById(R.id.tvRewardPoints);
        }
    }
}

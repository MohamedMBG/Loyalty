package com.example.loyaltyprogram;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.loyaltyprogram.models.UserActivityItem;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {
    private final List<UserActivityItem> activities = new ArrayList<>();
    private final NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.getDefault());

    public ActivityAdapter(List<UserActivityItem> activities) {
        if (activities != null) {
            this.activities.addAll(activities);
        }
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        UserActivityItem item = activities.get(position);
        holder.title.setText(item.getTitle());
        holder.date.setText(item.getDateLabel());

        String formatted = numberFormat.format(Math.abs(item.getPointsDelta()));
        holder.points.setText((item.isPositive() ? "+" : "-") + formatted);

        int color = ContextCompat.getColor(holder.points.getContext(),
                item.isPositive() ? R.color.activity_points_positive : R.color.activity_points_negative);
        holder.points.setTextColor(color);

        holder.icon.setBackgroundResource(item.getIconRes());
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public void updateData(List<UserActivityItem> newActivities) {
        activities.clear();
        if (newActivities != null) {
            activities.addAll(newActivities);
        }
        notifyDataSetChanged();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        final View icon;
        final TextView title;
        final TextView date;
        final TextView points;

        ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.vActivityIcon);
            title = itemView.findViewById(R.id.tvActivityTitle);
            date = itemView.findViewById(R.id.tvActivityDate);
            points = itemView.findViewById(R.id.tvActivityPoints);
        }
    }
}

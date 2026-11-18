package com.screenzen.app.ui;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.screenzen.app.R;

import java.util.List;

public final class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.Holder> {

    public interface OnActionListener {
        void onAction(OnboardingStep step);
    }

    private final List<OnboardingStep> steps;
    private final OnActionListener listener;

    public OnboardingAdapter(List<OnboardingStep> steps, OnActionListener listener) {
        this.steps = steps;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding_step, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final Holder holder, int position) {
        final OnboardingStep step = steps.get(position);
        holder.title.setText(step.title);
        holder.desc.setText(step.desc);
        holder.status.setText(step.statusText);
        holder.status.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), step.statusColorRes));
        holder.button.setText(step.buttonText);
        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onAction(step);
            }
        });

        if (TextUtils.isEmpty(step.extraText)) {
            holder.extra.setVisibility(View.GONE);
        } else {
            holder.extra.setText(step.extraText);
            holder.extra.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView status;
        final TextView desc;
        final TextView extra;
        final Button button;

        Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.step_title);
            status = itemView.findViewById(R.id.step_status);
            desc = itemView.findViewById(R.id.step_desc);
            extra = itemView.findViewById(R.id.step_extra);
            button = itemView.findViewById(R.id.step_button);
        }
    }
}

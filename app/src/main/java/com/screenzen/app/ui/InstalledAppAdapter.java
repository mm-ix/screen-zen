package com.screenzen.app.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.screenzen.app.R;
import com.screenzen.app.data.model.InstalledApp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 「拦截清单」页：已安装应用 + 勾选，带本地搜索过滤。 */
public final class InstalledAppAdapter extends RecyclerView.Adapter<InstalledAppAdapter.Holder> {

    public interface OnToggleListener {
        void onToggle(InstalledApp app, boolean blocked);
    }

    private final List<InstalledApp> all = new ArrayList<>();
    private final List<InstalledApp> visible = new ArrayList<>();
    private final OnToggleListener listener;
    private String query = "";

    public InstalledAppAdapter(OnToggleListener listener) {
        this.listener = listener;
    }

    public void setData(List<InstalledApp> apps) {
        all.clear();
        if (apps != null) {
            all.addAll(apps);
        }
        applyFilter();
    }

    public void setQuery(String text) {
        query = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        applyFilter();
    }

    private void applyFilter() {
        visible.clear();
        if (query.isEmpty()) {
            visible.addAll(all);
        } else {
            for (InstalledApp app : all) {
                if (app.label.toLowerCase(Locale.ROOT).contains(query)
                        || app.pkg.toLowerCase(Locale.ROOT).contains(query)) {
                    visible.add(app);
                }
            }
        }
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return visible.isEmpty();
    }

    /** 统计全部应用里的勾选数，不受搜索过滤影响。 */
    public int blockedCount() {
        int count = 0;
        for (InstalledApp app : all) {
            if (app.blocked) {
                count++;
            }
        }
        return count;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_installed_app, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final Holder holder, int position) {
        final InstalledApp app = visible.get(position);
        holder.label.setText(app.label);
        holder.pkg.setText(app.pkg);
        holder.icon.setImageDrawable(app.icon);
        holder.check.setChecked(app.blocked);

        // 勾选框自己不收点击（见布局），整行点击统一在这里toggle，
        // 这样 RecyclerView 复用不会触发到别的行的回调。
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                app.blocked = !app.blocked;
                holder.check.setChecked(app.blocked);
                listener.onToggle(app, app.blocked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return visible.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView label;
        final TextView pkg;
        final CheckBox check;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.app_icon);
            label = itemView.findViewById(R.id.app_label);
            pkg = itemView.findViewById(R.id.app_pkg);
            check = itemView.findViewById(R.id.app_check);
        }
    }
}

package com.screenzen.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.screenzen.app.R;
import com.screenzen.app.data.EventDao;
import com.screenzen.app.data.SettingsDao;
import com.screenzen.app.data.WatchConfig;
import com.screenzen.app.data.model.AppOpenStat;
import com.screenzen.app.data.model.DayCount;
import com.screenzen.app.data.model.OpenEvent;
import com.screenzen.app.data.model.UsageRow;
import com.screenzen.app.databinding.ActivityMainBinding;
import com.screenzen.app.usage.UsageStatsHelper;
import com.screenzen.app.util.PermissionUtils;
import com.screenzen.app.util.TimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 主页·统计：今日、累计、近 7 天趋势、按 App 明细、今日使用时长。 */
public final class MainActivity extends AppCompatActivity {

    private static final int CHART_DAYS = 7;

    private ActivityMainBinding binding;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    /** 程序化 setChecked 时抑制监听器，避免把同步当成用户点击。 */
    private boolean syncingSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBlocklist.setOnClickListener(v ->
                startActivity(new Intent(this, BlockListActivity.class)));
        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        binding.btnOnboarding.setOnClickListener(v ->
                startActivity(new Intent(this, OnboardingActivity.class)));
        binding.usageHint.setOnClickListener(v ->
                PermissionUtils.openUsageAccessSettings(this));

        binding.masterSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (syncingSwitch) {
                return;
            }
            new SettingsDao(this).setEnabled(checked);
            WatchConfig.refresh(this);
            renderMasterSwitch(checked);
            Toast.makeText(this,
                    checked ? R.string.home_master_on_toast : R.string.home_master_off_toast,
                    Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        WatchConfig.refresh(this);
        renderMasterSwitch(WatchConfig.isEnabled());
        loadStats();
    }

    private void renderMasterSwitch(boolean enabled) {
        syncingSwitch = true;
        binding.masterSwitch.setChecked(enabled);
        syncingSwitch = false;
        binding.masterStatus.setText(
                enabled ? R.string.home_master_on : R.string.home_master_off);
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private void loadStats() {
        io.execute(new Runnable() {
            @Override
            public void run() {
                final EventDao dao = new EventDao(getApplicationContext());
                final int todayRelease = dao.countToday(OpenEvent.RESULT_RELEASE);
                final int todayAbandon = dao.countToday(OpenEvent.RESULT_ABANDON);
                final int totalRelease = dao.countTotal(OpenEvent.RESULT_RELEASE);
                final int totalAbandon = dao.countTotal(OpenEvent.RESULT_ABANDON);
                final List<AppOpenStat> perApp = dao.perAppStats();

                final List<DayCount> trend = new ArrayList<>();
                for (int daysAgo = CHART_DAYS - 1; daysAgo >= 0; daysAgo--) {
                    long from = TimeUtils.startOfDay(daysAgo);
                    // 今天(daysAgo=0)的上界取「明天 0 点」
                    long to = TimeUtils.startOfDay(daysAgo - 1);
                    trend.add(new DayCount(TimeUtils.dayLabel(daysAgo),
                            dao.countBetween(from, to, OpenEvent.RESULT_RELEASE)));
                }

                final boolean usageGranted = PermissionUtils.hasUsageAccess(getApplicationContext());
                final List<UsageRow> usage = usageGranted
                        ? UsageStatsHelper.todayUsage(getApplicationContext())
                        : Collections.<UsageRow>emptyList();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        render(todayRelease, todayAbandon, totalRelease, totalAbandon,
                                perApp, trend, usage, usageGranted);
                    }
                });
            }
        });
    }

    private void render(int todayRelease, int todayAbandon, int totalRelease, int totalAbandon,
                        List<AppOpenStat> perApp, List<DayCount> trend,
                        List<UsageRow> usage, boolean usageGranted) {
        binding.todayRelease.setText(String.valueOf(todayRelease));
        binding.todayAbandon.setText(String.valueOf(todayAbandon));
        binding.totalText.setText(getString(R.string.home_total_format, totalRelease, totalAbandon));
        binding.chart.setData(trend);

        LayoutInflater inflater = getLayoutInflater();

        binding.perAppContainer.removeAllViews();
        if (perApp.isEmpty()) {
            binding.perAppEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.perAppEmpty.setVisibility(View.GONE);
            for (AppOpenStat stat : perApp) {
                View row = inflater.inflate(R.layout.item_app_open_stat,
                        binding.perAppContainer, false);
                ((TextView) row.findViewById(R.id.stat_name)).setText(stat.displayName());
                ((TextView) row.findViewById(R.id.stat_detail)).setText(getString(
                        R.string.home_per_app_item_format,
                        stat.todayRelease, stat.totalRelease, stat.totalAbandon));
                binding.perAppContainer.addView(row);
            }
        }

        binding.usageContainer.removeAllViews();
        binding.usageHint.setVisibility(usageGranted ? View.GONE : View.VISIBLE);
        if (!usageGranted) {
            binding.usageEmpty.setVisibility(View.GONE);
        } else if (usage.isEmpty()) {
            binding.usageEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.usageEmpty.setVisibility(View.GONE);
            for (UsageRow item : usage) {
                View row = inflater.inflate(R.layout.item_usage, binding.usageContainer, false);
                ((TextView) row.findViewById(R.id.usage_name)).setText(item.label);
                ((TextView) row.findViewById(R.id.usage_time))
                        .setText(TimeUtils.formatDuration(item.millis));
                binding.usageContainer.addView(row);
            }
        }
    }
}

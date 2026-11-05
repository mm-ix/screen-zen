package com.screenzen.app.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.screenzen.app.R;
import com.screenzen.app.data.WatchConfig;
import com.screenzen.app.databinding.ActivityOnboardingBinding;
import com.screenzen.app.service.AccessibilityKeeper;
import com.screenzen.app.util.PermissionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 引导页：每一步都带实时状态检测，onResume 重算一次，
 * 这样用户去系统设置里点完回来就能立刻看到状态变化。
 */
public final class OnboardingActivity extends AppCompatActivity
        implements OnboardingAdapter.OnActionListener {

    private ActivityOnboardingBinding binding;
    private final List<OnboardingStep> steps = new ArrayList<>();
    private OnboardingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(R.string.onboarding_title);

        steps.add(new OnboardingStep(OnboardingStep.ID_ACCESSIBILITY,
                getString(R.string.onboarding_step_accessibility),
                getString(R.string.onboarding_step_accessibility_desc)));
        steps.add(new OnboardingStep(OnboardingStep.ID_OVERLAY,
                getString(R.string.onboarding_step_overlay),
                getString(R.string.onboarding_step_overlay_desc)));
        steps.add(new OnboardingStep(OnboardingStep.ID_USAGE,
                getString(R.string.onboarding_step_usage),
                getString(R.string.onboarding_step_usage_desc)));
        steps.add(new OnboardingStep(OnboardingStep.ID_BATTERY,
                getString(R.string.onboarding_step_battery),
                getString(R.string.onboarding_step_battery_desc)));
        steps.add(new OnboardingStep(OnboardingStep.ID_KEEPALIVE,
                getString(R.string.onboarding_step_keepalive),
                getString(R.string.onboarding_step_keepalive_desc)));
        steps.add(new OnboardingStep(OnboardingStep.ID_AUTOSTART,
                getString(R.string.onboarding_step_autostart),
                getString(R.string.onboarding_step_autostart_desc)));

        adapter = new OnboardingAdapter(steps, this);
        binding.stepList.setLayoutManager(new LinearLayoutManager(this));
        binding.stepList.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatuses();
    }

    private void refreshStatuses() {
        WatchConfig.refresh(this);

        final String done = getString(R.string.onboarding_action_done);
        final String go = getString(R.string.onboarding_action_go);
        final String missing = getString(R.string.onboarding_status_missing);
        final int okColor = ContextCompat.getColor(this, R.color.status_ok);
        final int missColor = ContextCompat.getColor(this, R.color.status_missing);
        final int neutralColor = ContextCompat.getColor(this, R.color.status_neutral);

        OnboardingStep accessibility = steps.get(0);
        accessibility.satisfied = PermissionUtils.isAccessibilityEnabled(this);
        accessibility.statusText = accessibility.satisfied ? done : missing;
        accessibility.statusColorRes = accessibility.satisfied ? okColor : missColor;
        accessibility.buttonText = accessibility.satisfied ? done : go;
        accessibility.extraText = null;

        OnboardingStep overlay = steps.get(1);
        overlay.satisfied = PermissionUtils.canDrawOverlays(this);
        overlay.statusText = overlay.satisfied ? done : missing;
        overlay.statusColorRes = overlay.satisfied ? okColor : missColor;
        overlay.buttonText = overlay.satisfied ? done : go;
        overlay.extraText = null;

        OnboardingStep usage = steps.get(2);
        usage.satisfied = PermissionUtils.hasUsageAccess(this);
        usage.statusText = usage.satisfied ? done : missing;
        usage.statusColorRes = usage.satisfied ? okColor : missColor;
        usage.buttonText = usage.satisfied ? done : go;
        usage.extraText = null;

        OnboardingStep battery = steps.get(3);
        battery.satisfied = PermissionUtils.isIgnoringBatteryOptimizations(this);
        battery.statusText = battery.satisfied ? done : missing;
        battery.statusColorRes = battery.satisfied ? okColor : missColor;
        battery.buttonText = battery.satisfied ? done : go;
        battery.extraText = null;

        OnboardingStep keepAlive = steps.get(4);
        keepAlive.satisfied = AccessibilityKeeper.hasWritePermission(this);
        keepAlive.statusText = keepAlive.satisfied ? done : missing;
        keepAlive.statusColorRes = keepAlive.satisfied ? okColor : missColor;
        keepAlive.buttonText = keepAlive.satisfied ? done : getString(R.string.onboarding_action_copy);
        keepAlive.extraText = keepAlive.satisfied ? null
                : getString(R.string.onboarding_adb_format, getPackageName())
                        + "\n\n" + getString(R.string.onboarding_adb_hint);

        // 自启动没有公开 API 可查，只能给个中性提示 + 手动跳转
        OnboardingStep autoStart = steps.get(5);
        autoStart.satisfied = false;
        autoStart.statusText = getString(R.string.onboarding_status_unknown);
        autoStart.statusColorRes = neutralColor;
        autoStart.buttonText = go;
        autoStart.extraText = null;

        adapter.notifyDataSetChanged();

        binding.onboardingSummary.setText(getString(R.string.onboarding_intro)
                + "\n\n" + getString(R.string.onboarding_summary_format,
                        WatchConfig.blockedCount(), WatchConfig.countdownSeconds()));
    }

    @Override
    public void onAction(OnboardingStep step) {
        switch (step.id) {
            case OnboardingStep.ID_ACCESSIBILITY:
                // 已经授了 WRITE_SECURE_SETTINGS 的话可以直接帮用户打开，省得翻系统设置
                if (!AccessibilityKeeper.restore(this)) {
                    PermissionUtils.openAccessibilitySettings(this);
                }
                refreshStatuses();
                break;
            case OnboardingStep.ID_OVERLAY:
                PermissionUtils.openOverlaySettings(this);
                break;
            case OnboardingStep.ID_USAGE:
                PermissionUtils.openUsageAccessSettings(this);
                break;
            case OnboardingStep.ID_BATTERY:
                PermissionUtils.openBatteryOptimizationSettings(this);
                break;
            case OnboardingStep.ID_KEEPALIVE:
                copyKeepAliveCommand();
                break;
            case OnboardingStep.ID_AUTOSTART:
            default:
                PermissionUtils.openAutoStartSettings(this);
                break;
        }
    }

    private void copyKeepAliveCommand() {
        String command = getString(R.string.onboarding_adb_format, getPackageName());
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("adb", command));
            Toast.makeText(this, R.string.onboarding_copied, Toast.LENGTH_LONG).show();
        }
    }
}

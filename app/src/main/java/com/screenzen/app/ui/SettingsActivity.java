package com.screenzen.app.ui;

import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.screenzen.app.R;
import com.screenzen.app.data.EventDao;
import com.screenzen.app.data.SettingsDao;
import com.screenzen.app.data.WatchConfig;
import com.screenzen.app.databinding.ActivitySettingsBinding;
import com.screenzen.app.export.Exporter;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 「设置」页：倒计时秒数（0~60）+ 数据导出。 */
public final class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SettingsDao settingsDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(R.string.settings_title);

        settingsDao = new SettingsDao(this);

        binding.countdownSeek.setMax(SettingsDao.MAX_COUNTDOWN);
        binding.countdownSeek.setProgress(settingsDao.getCountdownSeconds());
        showCountdown(settingsDao.getCountdownSeconds());

        binding.countdownSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                showCountdown(progress);
                if (fromUser) {
                    // 拖动即生效，不用额外保存按钮
                    settingsDao.setCountdownSeconds(progress);
                    WatchConfig.refresh(SettingsActivity.this);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        binding.btnExportCsv.setOnClickListener(v -> export(true));
        binding.btnExportDb.setOnClickListener(v -> export(false));
    }

    @Override
    protected void onResume() {
        super.onResume();
        io.execute(new Runnable() {
            @Override
            public void run() {
                final int count = new EventDao(getApplicationContext()).countAll();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        binding.recordCount.setText(
                                getString(R.string.settings_record_count_format, count));
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private void showCountdown(int seconds) {
        binding.countdownValue.setText(getString(R.string.settings_countdown_value_format, seconds));
    }

    private void setExportEnabled(boolean enabled) {
        binding.btnExportCsv.setEnabled(enabled);
        binding.btnExportDb.setEnabled(enabled);
    }

    private void export(boolean csv) {
        setExportEnabled(false);
        final boolean wantCsv = csv;
        io.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (wantCsv && new EventDao(getApplicationContext()).countAll() == 0) {
                        postToast(getString(R.string.settings_export_empty));
                        return;
                    }
                    File file = wantCsv
                            ? Exporter.exportCsv(getApplicationContext())
                            : Exporter.exportDatabase(getApplicationContext());
                    final File result = file;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }
                            setExportEnabled(true);
                            Toast.makeText(SettingsActivity.this,
                                    getString(R.string.settings_export_done_format, result.getName()),
                                    Toast.LENGTH_LONG).show();
                            Exporter.share(SettingsActivity.this,
                                    result, wantCsv ? Exporter.MIME_CSV : Exporter.MIME_DB);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isFinishing() || isDestroyed()) {
                                return;
                            }
                            setExportEnabled(true);
                            Toast.makeText(SettingsActivity.this,
                                    getString(R.string.settings_export_failed_format,
                                            String.valueOf(e.getMessage())),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private void postToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setExportEnabled(true);
                Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

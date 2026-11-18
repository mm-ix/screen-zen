package com.screenzen.app.ui;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.screenzen.app.R;
import com.screenzen.app.data.BlockedAppDao;
import com.screenzen.app.data.WatchConfig;
import com.screenzen.app.data.model.InstalledApp;
import com.screenzen.app.databinding.ActivityBlockListBinding;
import com.screenzen.app.util.PackageUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 「拦截清单」页：读已安装应用，勾选即加入拦截。 */
public final class BlockListActivity extends AppCompatActivity
        implements InstalledAppAdapter.OnToggleListener {

    private ActivityBlockListBinding binding;
    private InstalledAppAdapter adapter;
    private BlockedAppDao blockedAppDao;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBlockListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle(R.string.blocklist_title);

        blockedAppDao = new BlockedAppDao(this);
        adapter = new InstalledAppAdapter(this);
        binding.appList.setLayoutManager(new LinearLayoutManager(this));
        binding.appList.setAdapter(adapter);

        binding.search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.setQuery(query);
                refreshEmptyState();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.setQuery(newText);
                refreshEmptyState();
                return true;
            }
        });

        loadApps();
    }

    @Override
    protected void onDestroy() {
        io.shutdownNow();
        super.onDestroy();
    }

    private void loadApps() {
        binding.blocklistEmpty.setText(R.string.blocklist_loading);
        binding.blocklistEmpty.setVisibility(View.VISIBLE);
        io.execute(new Runnable() {
            @Override
            public void run() {
                final List<InstalledApp> apps =
                        PackageUtils.loadLaunchableApps(getApplicationContext());
                final Set<String> enabled = blockedAppDao.enabledPackages();
                for (InstalledApp app : apps) {
                    app.blocked = enabled.contains(app.pkg);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        adapter.setData(apps);
                        refreshEmptyState();
                    }
                });
            }
        });
    }

    @Override
    public void onToggle(InstalledApp app, boolean blocked) {
        blockedAppDao.setEnabled(app.pkg, app.label, blocked);
        // 服务端用的是内存快照，改完立刻刷新
        WatchConfig.refresh(this);
        refreshEmptyState();
    }

    private void refreshEmptyState() {
        if (adapter.isEmpty()) {
            binding.blocklistEmpty.setText(R.string.blocklist_empty);
            binding.blocklistEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.blocklistEmpty.setVisibility(View.GONE);
        }
        binding.blocklistSummary.setText(
                getString(R.string.blocklist_summary_format, adapter.blockedCount()));
    }
}
